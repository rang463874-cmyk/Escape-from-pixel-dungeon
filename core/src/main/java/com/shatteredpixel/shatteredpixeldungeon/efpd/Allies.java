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
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Material;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BlacksmithSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GhostSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ImpSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WandmakerSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.io.IOException;

/**
 * Open world: the quest givers join the base once their quest is done, and each one helps out.
 *  - Ghost:      on death the hero keeps their gold and wakes up at full health
 *  - Wandmaker:  once a day, recharges every wand and hands over a potion or scroll
 *  - Blacksmith: once a day, delivers stone and metal; facility and forge gold costs halved
 *  - Imp:        items sell for 50% more gold at the base
 *
 * All tuning values live here.
 */
public class Allies {

	public enum Ally { GHOST, WANDMAKER, BLACKSMITH, IMP }

	public static final float IMP_SELL_BONUS   = 1.5f;
	public static final float SMITH_GOLD_MULTI = 0.5f;

	//where each ally stands in the base town (x, y on BaseLevel's map)
	private static final int[][] SPOTS = { {3, 3}, {13, 3}, {4, 10}, {12, 10} };

	private static int joined = 0;      //bitmask of Ally
	private static int[] lastService = new int[Ally.values().length]; //day of the last daily service

	public static void reset(){
		joined = 0;
		lastService = new int[Ally.values().length];
	}

	public static boolean has( Ally a ){
		return OpenWorld.isActive() && (joined & (1 << a.ordinal())) != 0;
	}

	public static String name( Ally a ){
		return Messages.get(Allies.class, "name_" + a.name().toLowerCase());
	}

	/** Called when a quest is completed. */
	public static void join( Ally a ){
		if (!OpenWorld.isActive() || has( a )) return;
		joined |= 1 << a.ordinal();
		GLog.p( Messages.get(Allies.class, "joined", name( a )) );
	}

	//--- effects ---------------------------------------------------------------------------------

	public static int sellPrice( int value ){
		return has( Ally.IMP ) && OpenWorld.onBase() ? Math.round( value * IMP_SELL_BONUS ) : value;
	}

	public static int facilityGold( int gold ){
		return has( Ally.BLACKSMITH ) ? Math.round( gold * SMITH_GOLD_MULTI ) : gold;
	}

	//--- daily services --------------------------------------------------------------------------

	public static boolean serviceReady( Ally a ){
		return lastService[a.ordinal()] < DayNight.day();
	}

	private static void markService( Ally a ){
		lastService[a.ordinal()] = DayNight.day();
		try {
			Dungeon.saveAll();
		} catch (IOException e) {
			com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon.reportException( e );
		}
	}

	private static void give( Item item ){
		if (!item.collect()) Dungeon.level.drop( item, Dungeon.hero.pos ).sprite.drop();
	}

	static void wandmakerService( Hero hero ){
		int wands = 0;
		for (Item i : hero.belongings){
			if (i instanceof Wand){
				((Wand) i).curCharges = Math.max( ((Wand) i).curCharges, ((Wand) i).maxCharges );
				wands++;
			}
		}
		Item gift = Generator.random( Random.Int(2) == 0 ? Generator.Category.POTION : Generator.Category.SCROLL );
		give( gift );
		Item.updateQuickslot();
		Sample.INSTANCE.play( Assets.Sounds.CHARGEUP );
		GLog.p( Messages.get(Allies.class, "wandmaker_done", wands, Messages.titleCase(gift.name())) );
		markService( Ally.WANDMAKER );
	}

	static void smithService(){
		int stone = Random.IntRange( 2, 4 );
		int metal = Random.IntRange( 1, 2 );
		give( new Material.StoneBlock().quantity( stone ) );
		give( new Material.RefinedMetal().quantity( metal ) );
		Item.updateQuickslot();
		GLog.p( Messages.get(Allies.class, "smith_done", stone, metal) );
		markService( Ally.BLACKSMITH );
	}

	//--- placing allies in the town --------------------------------------------------------------

	/** Puts every ally who has joined (and isn't standing there yet) into the base town. */
	public static void populateBase(){
		if (!OpenWorld.onBase() || !(Dungeon.level instanceof BaseLevel)) return;
		for (Ally a : Ally.values()){
			if (!has( a )) continue;
			boolean present = false;
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof AllyNPC && ((AllyNPC) m).ally == a) present = true;
			}
			if (present) continue;

			AllyNPC npc = new AllyNPC();
			npc.setAlly( a );
			int cell = SPOTS[a.ordinal()][0] + SPOTS[a.ordinal()][1] * Dungeon.level.width();
			if (Actor.findChar( cell ) != null) continue;
			npc.pos = cell;
			GameScene.add( npc );
		}
	}

	/** A quest giver living at the base. */
	public static class AllyNPC extends NPC {

		Ally ally = Ally.GHOST;

		{
			properties.add( Property.IMMOVABLE );
		}

		void setAlly( Ally a ){
			ally = a;
			switch (a){
				case GHOST:      spriteClass = GhostSprite.class; break;
				case WANDMAKER:  spriteClass = WandmakerSprite.class; break;
				case BLACKSMITH: spriteClass = BlacksmithSprite.class; break;
				case IMP:        spriteClass = ImpSprite.class; break;
			}
		}

		@Override
		public String name() {
			return Allies.name( ally );
		}

		@Override
		public String description() {
			return Messages.get(Allies.class, "desc_" + ally.name().toLowerCase());
		}

		@Override
		protected boolean act() {
			if (sprite != null) sprite.turnTo( pos, Dungeon.hero.pos );
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
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					showMenu( hero );
				}
			});
			return true;
		}

		private void showMenu( final Hero hero ){
			String text = Messages.get(Allies.class, "perk_" + ally.name().toLowerCase());
			final boolean daily = ally == Ally.WANDMAKER || ally == Ally.BLACKSMITH;
			final boolean ready = daily && serviceReady( ally );
			String[] options;
			if (daily){
				options = new String[]{
						ready ? Messages.get(Allies.class, "service_" + ally.name().toLowerCase())
						      : Messages.get(Allies.class, "service_done"),
						Messages.get(Allies.class, "bye") };
			} else {
				options = new String[]{ Messages.get(Allies.class, "bye") };
			}
			GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ), text, options ){
				@Override
				protected boolean enabled( int index ) {
					return !daily || index != 0 || serviceReady( ally );
				}

				@Override
				protected void onSelect( int index ) {
					if (!daily || index != 0 || !serviceReady( ally )) return;
					if (ally == Ally.WANDMAKER) wandmakerService( hero );
					else smithService();
				}
			});
		}

		private static final String ALLY = "efpd_ally";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( ALLY, ally );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			setAlly( bundle.getEnum( ALLY, Ally.class ) );
		}
	}

	//--- saving ----------------------------------------------------------------------------------

	private static final String NODE    = "efpd_allies";
	private static final String JOINED  = "joined";
	private static final String SERVICE = "service";

	public static void storeInBundle( Bundle bundle ){
		Bundle node = new Bundle();
		node.put( JOINED, joined );
		node.put( SERVICE, lastService );
		bundle.put( NODE, node );
	}

	public static void restoreFromBundle( Bundle bundle ){
		reset();
		if (!bundle.contains( NODE )) return;
		Bundle node = bundle.getBundle( NODE );
		joined = node.getInt( JOINED );
		int[] s = node.getIntArray( SERVICE );
		if (s != null) System.arraycopy( s, 0, lastService, 0, Math.min(s.length, lastService.length) );
	}

}
