/*
 * Escape from Pixel Dungeon
 * A hardcore extraction mod of Shattered Pixel Dungeon
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.efpd;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Open world: once Yog-Dzewa has fallen, the ring sage moves into the base town. The sage takes
 * a ring for good and binds its power to the hero: the ring's effect then applies permanently,
 * as if it were worn, on top of the rings actually worn.
 *
 * One ring per kind. Offering a stronger ring of a kind already bound replaces the old one
 * (which is lost); weaker or equal rings and cursed rings are refused.
 *
 * Bound rings live in the run's Statistics bundle. Their buffs aren't saved with the hero
 * (ring buffs never are), so they are re-attached whenever the hero arrives somewhere.
 */
public class RingSage {

	public static final int YOG_DEPTH = 25;
	//where the sage stands in the base town (x, y on BaseLevel's map)
	private static final int SPOT_X = 8, SPOT_Y = 2;

	private static ArrayList<Ring> bound = new ArrayList<>();
	//the buff each bound ring gave the hero, to re-attach or remove it (not saved)
	private static HashMap<Ring, Buff> buffs = new HashMap<>();

	public static boolean available(){
		return OpenWorld.isActive() && (Statistics.efpdBossesCleared & (1L << YOG_DEPTH)) != 0;
	}

	public static ArrayList<Ring> bound(){
		return bound;
	}

	/** Called when Yog-Dzewa dies in open world mode. */
	public static void onYogSlain(){
		if (OpenWorld.isActive()) GLog.p( Messages.get( RingSage.class, "unlocked" ) );
	}

	//--- effects ---------------------------------------------------------------------------------

	/** Makes sure every bound ring's buff is on the hero. */
	public static void ensureActive(){
		Hero hero = Dungeon.hero;
		if (hero == null || !OpenWorld.isActive()) return;
		boolean changed = false;
		for (Ring r : bound){
			Buff b = buffs.get( r );
			if (b != null && b.target == hero && hero.buffs().contains( b )) continue;
			attach( hero, r );
			changed = true;
		}
		if (changed) hero.updateHT( false );
	}

	private static void attach( Hero hero, Ring r ){
		HashSet<Buff> before = new HashSet<>( hero.buffs() );
		r.activate( hero );
		for (Buff b : hero.buffs()){
			if (!before.contains( b ) && b instanceof Ring.RingBuff){
				buffs.put( r, b );
				break;
			}
		}
	}

	private static Ring boundOfKind( Ring r ){
		for (Ring b : bound) if (b.getClass() == r.getClass()) return b;
		return null;
	}

	/** Why a ring can't be bound, or null if it can. Identifies it first. */
	private static String refusal( Ring r ){
		if (r.cursed || r.level() < 0) return Messages.get( RingSage.class, "refuse_cursed" );
		Ring old = boundOfKind( r );
		if (old != null && old.level() >= r.level()){
			return Messages.get( RingSage.class, "refuse_weaker", old.title() );
		}
		return null;
	}

	private static void bind( Hero hero, Ring r ){
		Ring old = boundOfKind( r );
		if (old != null){
			Buff b = buffs.remove( old );
			if (b != null) b.detach();
			bound.remove( old );
		}
		r.detachAll( hero.belongings.backpack );
		bound.add( r );
		attach( hero, r );
		hero.updateHT( false );
		Item.updateQuickslot();

		Sample.INSTANCE.play( Assets.Sounds.LEVELUP, 0.8f, 0.8f );
		if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 12 );
		GLog.p( Messages.get( RingSage.class, "bound", r.title() ) );
		try {
			Dungeon.saveAll();
		} catch (IOException e) {
			com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon.reportException( e );
		}
	}

	public static String boundList(){
		if (bound.isEmpty()) return Messages.get( RingSage.class, "none_bound" );
		StringBuilder sb = new StringBuilder( Messages.get( RingSage.class, "bound_title" ) );
		for (Ring r : bound) sb.append( "\n- " ).append( Messages.titleCase( r.title() ) );
		return sb.toString();
	}

	//--- the sage in town ------------------------------------------------------------------------

	public static void populateBase(){
		if (!available() || !OpenWorld.onBase() || !(Dungeon.level instanceof BaseLevel)) return;
		for (Mob m : Dungeon.level.mobs) if (m instanceof SageNPC) return;
		int cell = SPOT_X + SPOT_Y * Dungeon.level.width();
		if (Actor.findChar( cell ) != null) return;
		SageNPC npc = new SageNPC();
		npc.pos = cell;
		GameScene.add( npc );
	}

	public static class SageNPC extends NPC {

		{
			spriteClass = SageSprite.class;
			properties.add( Property.IMMOVABLE );
		}

		@Override
		public String name() {
			return Messages.get( RingSage.class, "name" );
		}

		@Override
		public String description() {
			return Messages.get( RingSage.class, "desc" );
		}

		@Override
		protected boolean act() {
			if (sprite != null && Dungeon.hero != null) sprite.turnTo( pos, Dungeon.hero.pos );
			spend( TICK );
			return super.act();
		}

		@Override
		public int defenseSkill( Char enemy ) {
			return INFINITE_EVASION;
		}

		@Override
		public void damage( int dmg, Object src ) {
		}

		@Override
		public boolean add( Buff buff ) {
			return false;
		}

		@Override
		public boolean reset() {
			return true;
		}

		@Override
		public boolean interact( Char c ) {
			if (c != Dungeon.hero) return true;
			final Hero hero = (Hero) c;
			Game.runOnRenderThread( new Callback() {
				@Override
				public void call() {
					showMenu( hero );
				}
			} );
			return true;
		}

		private void showMenu( final Hero hero ){
			GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ),
					Messages.get( RingSage.class, "talk" ) + "\n\n" + boundList(),
					Messages.get( RingSage.class, "offer" ),
					Messages.get( RingSage.class, "bye" ) ){
				@Override
				protected void onSelect( int index ) {
					if (index == 0) GameScene.selectItem( selector( hero ) );
				}
			} );
		}

		private WndBag.ItemSelector selector( final Hero hero ){
			return new WndBag.ItemSelector() {
				@Override
				public String textPrompt() {
					return Messages.get( RingSage.class, "prompt" );
				}

				@Override
				public boolean itemSelectable( Item item ) {
					return item instanceof Ring && !item.isEquipped( hero );
				}

				@Override
				public void onSelect( Item item ) {
					if (!(item instanceof Ring)) return;
					final Ring r = (Ring) item;
					if (!r.isIdentified()) r.identify();
					String no = refusal( r );
					if (no != null){
						GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ), no,
								Messages.get( RingSage.class, "bye" ) ) );
						return;
					}
					Ring old = boundOfKind( r );
					String text = old == null
							? Messages.get( RingSage.class, "confirm", r.title() )
							: Messages.get( RingSage.class, "confirm_replace", r.title(), old.title() );
					GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ), text,
							Messages.get( RingSage.class, "yes" ),
							Messages.get( RingSage.class, "no" ) ){
						@Override
						protected void onSelect( int index ) {
							if (index == 0) bind( hero, r );
						}
					} );
				}
			};
		}
	}

	/** The wandmaker's sprite in gold and white (sprites/efpd_ringsage.png). */
	public static class SageSprite extends MobSprite {
		public SageSprite(){
			super();
			texture( "sprites/efpd_ringsage.png" );
			TextureFilm frames = new TextureFilm( texture, 12, 14 );
			idle = new Animation( 10, true );
			idle.frames( frames, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 3, 3, 3, 3, 3, 2, 1 );
			run = new Animation( 20, true );
			run.frames( frames, 0 );
			die = new Animation( 20, false );
			die.frames( frames, 0 );
			play( idle );
		}
	}

	//--- saving ----------------------------------------------------------------------------------

	public static void reset(){
		bound = new ArrayList<>();
		buffs = new HashMap<>();
	}

	private static final String NODE  = "efpd_ring_sage";
	private static final String RINGS = "rings";

	public static void storeInBundle( Bundle bundle ){
		Bundle node = new Bundle();
		node.put( RINGS, bound );
		bundle.put( NODE, node );
	}

	public static void restoreFromBundle( Bundle bundle ){
		reset();
		if (!bundle.contains( NODE )) return;
		for (Bundlable b : bundle.getBundle( NODE ).getCollection( RINGS )){
			if (b instanceof Ring) bound.add( (Ring) b );
		}
	}

}
