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
import com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Lab;
import com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * Open world: the final tier of town buildings, raised with Amulets of Yendor from the stash
 * (every amulet carried out through the first floor ends up there, in any mode).
 *  - dimensional gate: travel to any unlocked floor, and enter the Lab without a keycard once a day
 *  - tower of time: set the time of day, and make any floor or city district regrow right away
 *  - altar of legacy: raises every legacy perk's cap by one (for all games), and turns further
 *    amulets into legacy points
 *
 * All tuning values live here.
 */
public class YendorWorks {

	public static final int GATE_AMULETS  = 1, GATE_GOLD  = 2000;
	public static final int TOWER_AMULETS = 1, TOWER_GOLD = 3000;
	public static final int ALTAR_AMULETS = 1;

	private static boolean gate = false;
	private static boolean tower = false;
	private static int freeLabDay = -1;

	public static void reset(){
		gate = tower = false;
		freeLabDay = -1;
	}

	private static String msg( String key, Object... args ){
		return Messages.get( YendorWorks.class, key, args );
	}

	//--- amulets in the stash ---------------------------------------------------------------------

	public static int amulets(){
		Stash.load();
		int n = 0;
		for (Item i : Stash.items) if (i instanceof Amulet) n++;
		return n;
	}

	private static boolean spendAmulets( int n ){
		if (amulets() < n) return false;
		for (Item i : new ArrayList<>( Stash.items )){
			if (n <= 0) break;
			if (i instanceof Amulet){
				Stash.items.remove( i );
				n--;
			}
		}
		Stash.save();
		return true;
	}

	private static boolean pay( int amulets, int gold ){
		if (amulets() < amulets || Facilities.haveGold() < gold) return false;
		spendAmulets( amulets );
		Facilities.spendGold( gold );
		OpenWorld.saveRun();
		return true;
	}

	private static String cost( int amulets, int gold ){
		return gold > 0 ? msg( "cost", amulets, gold ) : msg( "cost_amulets", amulets );
	}

	//--- menu ------------------------------------------------------------------------------------

	public static void show(){
		WndFacilities.Builder b = new WndFacilities.Builder();
		b.add( gate ? msg( "gate_built" ) : msg( "gate_build", cost( GATE_AMULETS, GATE_GOLD ) ), true,
				new Runnable() { public void run() { showGate(); } } );
		b.add( tower ? msg( "tower_built" ) : msg( "tower_build", cost( TOWER_AMULETS, TOWER_GOLD ) ), true,
				new Runnable() { public void run() { showTower(); } } );
		b.add( Legacy.altarBuilt() ? msg( "altar_built" ) : msg( "altar_build", cost( ALTAR_AMULETS, 0 ) ), true,
				new Runnable() { public void run() { showAltar(); } } );
		b.add( msg( "back" ), true, new Runnable() { public void run() { WndFacilities.show(); } } );
		b.show( msg( "title" ), msg( "intro", amulets(), Facilities.haveGold() ) );
	}

	private static void built( String key ){
		Sample.INSTANCE.play( Assets.Sounds.LEVELUP, 0.8f, 0.7f );
		GLog.p( msg( key ) );
	}

	//--- dimensional gate ------------------------------------------------------------------------

	private static int maxDepth(){
		int r = 0;
		for (int i = 0; i < OpenWorld.REGION_START.length; i++) if (OpenWorld.regionUnlocked( i )) r = i;
		return Math.min( 25, OpenWorld.REGION_START[r] + 4 );
	}

	private static void showGate(){
		WndFacilities.Builder b = new WndFacilities.Builder();
		if (!gate){
			b.add( msg( "build", cost( GATE_AMULETS, GATE_GOLD ) ),
					amulets() >= GATE_AMULETS && Facilities.haveGold() >= GATE_GOLD, new Runnable() { public void run() {
				if (pay( GATE_AMULETS, GATE_GOLD )){
					gate = true;
					built( "gate_done" );
					OpenWorld.saveRun();
				}
			}});
		} else {
			b.add( msg( "gate_travel", maxDepth() ), true, new Runnable() { public void run() { askDepth( false ); } } );
			final boolean free = freeLabDay != DayNight.day();
			b.add( free ? msg( "gate_lab" ) : msg( "gate_lab_used" ), free, new Runnable() { public void run() {
				freeLabDay = DayNight.day();
				Lab.enterFree();
			}});
		}
		b.add( msg( "back" ), true, new Runnable() { public void run() { show(); } } );
		b.show( msg( "gate_name" ), msg( "gate_desc" ) );
	}

	//--- tower of time ---------------------------------------------------------------------------

	private static void showTower(){
		WndFacilities.Builder b = new WndFacilities.Builder();
		if (!tower){
			b.add( msg( "build", cost( TOWER_AMULETS, TOWER_GOLD ) ),
					amulets() >= TOWER_AMULETS && Facilities.haveGold() >= TOWER_GOLD, new Runnable() { public void run() {
				if (pay( TOWER_AMULETS, TOWER_GOLD )){
					tower = true;
					built( "tower_done" );
					OpenWorld.saveRun();
				}
			}});
		} else {
			b.add( msg( "tower_dawn" ), true, new Runnable() { public void run() { setTime( 0 ); } } );
			b.add( msg( "tower_evening" ), true, new Runnable() { public void run() { setTime( DayNight.EVENING ); } } );
			b.add( msg( "tower_night" ), true, new Runnable() { public void run() { setTime( DayNight.NIGHT ); } } );
			b.add( msg( "tower_floor" ), true, new Runnable() { public void run() { askDepth( true ); } } );
			b.add( msg( "tower_city" ), true, new Runnable() { public void run() { showCityRegen(); } } );
		}
		b.add( msg( "back" ), true, new Runnable() { public void run() { show(); } } );
		b.show( msg( "tower_name" ), msg( "tower_desc", DayNight.label() ) );
	}

	private static void setTime( int target ){
		int now = DayNight.timeOfDay();
		int delta = (target - now + DayNight.DAY_LENGTH) % DayNight.DAY_LENGTH;
		if (delta == 0) delta = DayNight.DAY_LENGTH;
		Statistics.duration += delta;
		Sample.INSTANCE.play( Assets.Sounds.TELEPORT );
		GLog.p( msg( "time_set", DayNight.label() ) );
		Dungeon.observe();
		OpenWorld.saveRun();
	}

	private static void showCityRegen(){
		WndFacilities.Builder b = new WndFacilities.Builder();
		for (int z = 0; z < Urban.ZONE_DEPTHS.length; z++){
			final int zone = z;
			boolean ok = OpenWorld.regionUnlocked( z ) && !Regen.cityHeldByCorpseAt( Urban.ZONE_DEPTHS[z] );
			b.add( Messages.get( WndWorldMap.class, "city_district", z + 1 ), ok, new Runnable() { public void run() {
				Regen.forceCityRegen( Urban.ZONE_DEPTHS[zone] );
				GLog.p( msg( "regrown_city", zone + 1 ) );
				OpenWorld.saveRun();
			}});
		}
		b.add( msg( "back" ), true, new Runnable() { public void run() { showTower(); } } );
		b.show( msg( "tower_city" ), msg( "tower_city_desc" ) );
	}

	//asks for a floor number: to travel there (gate) or to make it regrow (tower)
	private static void askDepth( final boolean regrow ){
		final int max = maxDepth();
		GameScene.show( new WndTextInput(
				msg( regrow ? "tower_floor" : "gate_travel", max ),
				msg( "depth_range", max ),
				"1", 2, false, msg( "ok" ), msg( "cancel" ) ){
			@Override
			public void onSelect( boolean positive, String text ) {
				if (!positive) return;
				int d;
				try {
					d = Integer.parseInt( text.trim() );
				} catch (NumberFormatException e){
					return;
				}
				if (d < 1 || d > max){
					GLog.w( msg( "depth_range", max ) );
					return;
				}
				if (regrow){
					if (Regen.forceFloorRegen( d )){
						GLog.p( msg( "regrown_floor", d ) );
						OpenWorld.saveRun();
					} else {
						GLog.w( msg( "regrow_corpse" ) );
					}
				} else {
					OpenWorld.travelTo( d, 0 );
				}
			}
		} );
	}

	//--- altar of legacy -------------------------------------------------------------------------

	private static void showAltar(){
		WndFacilities.Builder b = new WndFacilities.Builder();
		if (!Legacy.altarBuilt()){
			b.add( msg( "build", cost( ALTAR_AMULETS, 0 ) ), amulets() >= ALTAR_AMULETS, new Runnable() { public void run() {
				if (pay( ALTAR_AMULETS, 0 )){
					Legacy.buildAltar();
					built( "altar_done" );
				}
			}});
		} else {
			b.add( msg( "altar_offer" ), amulets() >= 1, new Runnable() { public void run() {
				if (spendAmulets( 1 )){
					Legacy.addPoints( 1 );
					Sample.INSTANCE.play( Assets.Sounds.LEVELUP );
					GLog.p( msg( "altar_offered" ) );
					OpenWorld.saveRun();
				}
			}});
			b.add( Messages.get( Legacy.class, "title" ), true, new Runnable() { public void run() { Legacy.show(); } } );
		}
		b.add( msg( "back" ), true, new Runnable() { public void run() { show(); } } );
		b.show( msg( "altar_name" ), msg( "altar_desc" ) );
	}

	//--- saving ----------------------------------------------------------------------------------

	private static final String NODE = "efpd_yendor_works";

	public static void storeInBundle( Bundle bundle ){
		Bundle n = new Bundle();
		n.put( "gate", gate );
		n.put( "tower", tower );
		n.put( "free_lab", freeLabDay );
		bundle.put( NODE, n );
	}

	public static void restoreFromBundle( Bundle bundle ){
		reset();
		if (!bundle.contains( NODE )) return;
		Bundle n = bundle.getBundle( NODE );
		gate = n.getBoolean( "gate" );
		tower = n.getBoolean( "tower" );
		freeLabDay = n.contains( "free_lab" ) ? n.getInt( "free_lab" ) : -1;
	}

}
