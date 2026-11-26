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
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.Ammo;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.Gun;
import com.shatteredpixel.shatteredpixeldungeon.efpd.hvt.DogTag;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Bandage;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.FirstAidKit;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Splint;
import com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Legacy perks: permanent upgrades for every future game, in every mode. Each ending earns a
 * legacy point (bringing the Amulet of Yendor out, escaping the Lab); points buy perk levels.
 * Saved in their own file next to the stash, so they survive every run.
 *
 * All tuning values live here.
 */
public class Legacy {

	public enum Perk {
		VITALITY  ( 3 ), //+HP_PER max HP per level
		STRENGTH  ( 2 ), //+1 strength per level
		PORTER    ( 3 ), //+CARRY_PER weight capacity per level
		LIGHT_GEAR( 2 ), //weapons and armor weigh GEAR_LIGHTEN less per level
		BANDOLIER ( 1 ), //ammo weighs half
		FIELD_MEDIC( 1 ),//recipe: bandage + splint -> first aid kit
		FORGER    ( 1 ); //recipe: 3 dog tags -> Lab keycard

		public final int max;
		Perk( int max ){ this.max = max; }
	}

	public static final int   HP_PER       = 5;
	public static final float CARRY_PER    = 4f;
	public static final float GEAR_LIGHTEN = 0.15f;
	public static final float AMMO_MULTI   = 0.5f;

	public static final String FILE = "legacy.dat";

	private static boolean loaded = false;
	private static int points = 0;
	private static int earned = 0;
	private static int[] levels = new int[Perk.values().length];

	private static void ensureLoaded(){
		if (loaded) return;
		loaded = true;
		try {
			Bundle b = FileUtils.bundleFromFile( FILE );
			points = b.getInt( "points" );
			earned = b.getInt( "earned" );
			int[] l = b.getIntArray( "levels" );
			if (l != null) System.arraycopy( l, 0, levels, 0, Math.min( l.length, levels.length ) );
		} catch (IOException e){
			//no legacy yet
		}
	}

	private static void save(){
		Bundle b = new Bundle();
		b.put( "points", points );
		b.put( "earned", earned );
		b.put( "levels", levels );
		try {
			FileUtils.bundleToFile( FILE, b );
		} catch (IOException e){
			ShatteredPixelDungeon.reportException( e );
		}
	}

	public static int points(){ ensureLoaded(); return points; }
	public static int level( Perk p ){ ensureLoaded(); return levels[p.ordinal()]; }
	public static boolean has( Perk p ){ return level( p ) > 0; }

	/** An ending was reached: one more legacy point. */
	public static void award( String reasonKey ){
		ensureLoaded();
		points++;
		earned++;
		save();
		GLog.p( Messages.get( Legacy.class, "awarded", Messages.get( Legacy.class, reasonKey ) ) );
	}

	public static String awardText( String reasonKey ){
		return Messages.get( Legacy.class, "awarded", Messages.get( Legacy.class, reasonKey ) );
	}

	private static boolean buy( Perk p ){
		ensureLoaded();
		if (points <= 0 || levels[p.ordinal()] >= p.max) return false;
		points--;
		levels[p.ordinal()]++;
		save();
		if (Dungeon.hero != null) Dungeon.hero.updateHT( true );
		Item.updateQuickslot();
		return true;
	}

	//--- effects ---------------------------------------------------------------------------------

	public static int htBonus(){ return level( Perk.VITALITY ) * HP_PER; }
	public static int strBonus(){ return level( Perk.STRENGTH ); }
	public static float carryBonus(){ return level( Perk.PORTER ) * CARRY_PER; }

	public static float weightMulti( Item item ){
		if (item instanceof Ammo) return has( Perk.BANDOLIER ) ? AMMO_MULTI : 1f;
		if (item instanceof MeleeWeapon || item instanceof MissileWeapon || item instanceof Armor || item instanceof Gun){
			return 1f - GEAR_LIGHTEN * level( Perk.LIGHT_GEAR );
		}
		return 1f;
	}

	//--- recipes unlocked by perks ---------------------------------------------------------------

	public static class MedicRecipe extends Recipe.SimpleRecipe {
		{
			inputs = new Class[]{ Bandage.class, Splint.class }; inQuantity = new int[]{ 1, 1 };
			cost = 2; output = FirstAidKit.class; outQuantity = 1;
		}
		@Override
		public boolean testIngredients( ArrayList<Item> ingredients ) {
			return has( Perk.FIELD_MEDIC ) && super.testIngredients( ingredients );
		}
	}

	public static class ForgeKeycard extends Recipe.SimpleRecipe {
		{
			inputs = new Class[]{ DogTag.class }; inQuantity = new int[]{ 3 };
			cost = 15; output = Keycard.class; outQuantity = 1;
		}
		@Override
		public boolean testIngredients( ArrayList<Item> ingredients ) {
			return has( Perk.FORGER ) && super.testIngredients( ingredients );
		}
	}

	//--- window ----------------------------------------------------------------------------------

	private static String name( Perk p ){
		return Messages.get( Legacy.class, "perk_" + p.name().toLowerCase() );
	}

	private static String desc( Perk p ){
		return Messages.get( Legacy.class, "perk_" + p.name().toLowerCase() + "_desc" );
	}

	/** Shows the legacy perks over whatever scene is open. */
	public static void show(){
		Game.runOnRenderThread( new com.watabou.utils.Callback() {
			@Override
			public void call() {
				ShatteredPixelDungeon.scene().addToFront( new Wnd() );
			}
		} );
	}

	private static class Wnd extends WndOptions {
		private static Perk[] perks = Perk.values();

		Wnd(){
			super( Messages.get( Legacy.class, "title" ),
					Messages.get( Legacy.class, "desc", points(), earned ), labels() );
		}

		private static String[] labels(){
			String[] l = new String[perks.length + 1];
			for (int i = 0; i < perks.length; i++){
				Perk p = perks[i];
				l[i] = Messages.get( Legacy.class, "entry", name( p ), level( p ), p.max );
			}
			l[perks.length] = Messages.get( Legacy.class, "close" );
			return l;
		}

		@Override
		protected void onSelect( int index ) {
			if (index >= perks.length) return;
			final Perk p = perks[index];
			boolean can = points() > 0 && level( p ) < p.max;
			String text = desc( p ) + "\n\n" + Messages.get( Legacy.class, "level_line", level( p ), p.max, points() );
			ShatteredPixelDungeon.scene().addToFront( new WndOptions( name( p ), text,
					can ? Messages.get( Legacy.class, "buy" ) : Messages.get( Legacy.class, level( p ) >= p.max ? "maxed" : "no_points" ),
					Messages.get( Legacy.class, "back" ) ){
				@Override
				protected boolean enabled( int i ) {
					return i != 0 || (points() > 0 && level( p ) < p.max);
				}
				@Override
				protected void onSelect( int i ) {
					if (i == 0 && buy( p )){
						Sample.INSTANCE.play( Assets.Sounds.LEVELUP );
						GLog.p( Messages.get( Legacy.class, "bought", name( p ), level( p ) ) );
					}
					show();
				}
			} );
		}
	}

}
