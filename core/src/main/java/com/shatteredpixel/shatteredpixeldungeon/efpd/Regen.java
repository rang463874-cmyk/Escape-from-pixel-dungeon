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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.watabou.utils.Bundle;

/**
 * Open world region regeneration. Every dungeon floor remembers when the hero was last there.
 * Coming back after REGEN_TURNS (BOSS_REGEN_TURNS for boss floors) builds the floor anew:
 * new layout, enemies and loot, and on boss floors a new boss.
 *
 * Never regenerated: the base town, side branches (quest areas), the amulet floor and the
 * floor holding the hero's corpse bag.
 */
public class Regen {

	public static final int REGEN_TURNS      = 3 * DayNight.DAY_LENGTH;
	public static final int BOSS_REGEN_TURNS = 5 * DayNight.DAY_LENGTH;

	private static final int SIZE = 32; //depths 0-31 of branch 0
	private static long[] lastVisit  = new long[SIZE];
	private static int[]  generation = new int[SIZE];

	//open world visits to the ruined city: one entry per district, building floors included
	public static final int CITY_REGEN_TURNS = 3 * DayNight.DAY_LENGTH;
	private static long[] cityVisit = new long[8];
	private static int[]  cityGen   = new int[8];

	public static void reset(){
		lastVisit  = new long[SIZE];
		generation = new int[SIZE];
		cityVisit  = new long[8];
		cityGen    = new int[8];
	}

	private static boolean eligible( int depth, int branch ){
		if (!OpenWorld.isActive() || branch != 0) return false;
		if (depth < 1 || depth >= 26 || depth >= SIZE) return false;
		return !(Statistics.efpdCorpseDepth == depth && Statistics.efpdCorpseBranch == branch);
	}

	public static boolean heldByCorpse( int depth ){
		return Statistics.efpdCorpseDepth == depth && Statistics.efpdCorpseBranch == 0;
	}

	public static boolean isBossDepth( int depth ){
		return depth % 5 == 0;
	}

	/** Remembers that the hero is (or just was) on the current floor. */
	public static void markVisit(){
		if (Dungeon.branch == 0 && Dungeon.depth >= 0 && Dungeon.depth < SIZE){
			lastVisit[Dungeon.depth] = DayNight.turn();
		} else if (OpenWorld.isActive() && com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.inCity( Dungeon.branch )){
			cityVisit[com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.zoneIndex( Dungeon.depth )] = DayNight.turn();
		}
	}

	public static int cityTurnsUntilRegen( int depth ){
		return (int)Math.max( 0, cityVisit[com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.zoneIndex( depth )] + CITY_REGEN_TURNS - DayNight.turn() );
	}

	private static boolean cityHeldByCorpse( int depth ){
		return Statistics.efpdCorpseDepth == depth && com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.inCity( Statistics.efpdCorpseBranch );
	}

	//a whole district regrows at once: its street map and every building floor
	private static boolean shouldRegenerateCity( int depth ){
		int z = com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.zoneIndex( depth );
		if (cityVisit[z] <= 0 || cityTurnsUntilRegen( depth ) > 0 || cityHeldByCorpse( depth )) return false;
		cityGen[z]++;
		cityVisit[z] = 0;
		for (Integer key : Dungeon.generatedLevels.toArray( new Integer[0] )){
			int b = key / 1000, d = key % 1000;
			if (d == depth && com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.isFloorBranch( b )) Dungeon.generatedLevels.remove( key );
		}
		notice = true;
		return true;
	}

	public static int turnsUntilRegen( int depth ){
		long wait = isBossDepth( depth ) ? BOSS_REGEN_TURNS : REGEN_TURNS;
		return (int)Math.max( 0, lastVisit[depth] + wait - DayNight.turn() );
	}

	/**
	 * Called right before a generated floor would be loaded. Returns true if the floor should be
	 * built anew instead, and bumps its generation so it gets a fresh seed.
	 */
	public static boolean shouldRegenerate( int depth, int branch ){
		if (OpenWorld.isActive() && branch == com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.URBAN_BRANCH) return shouldRegenerateCity( depth );
		if (!eligible( depth, branch ) || lastVisit[depth] <= 0) return false;
		if (turnsUntilRegen( depth ) > 0) return false;
		generation[depth]++;
		notice = true;
		return true;
	}

	//set when a floor was just regenerated, shown once the hero arrives
	private static boolean notice = false;

	public static boolean consumeNotice(){
		boolean n = notice;
		notice = false;
		return n;
	}

	/** Mixes the floor's generation into its level seed, so a regenerated floor looks different. */
	public static long seedFor( int depth, int branch, long seed ){
		if (com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.inCity( branch )){
			int g = cityGen[com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.zoneIndex( depth )];
			return g == 0 ? seed : seed ^ (0x7F4A7C159E3779B9L * g);
		}
		if (branch != 0 || depth < 0 || depth >= SIZE || generation[depth] == 0) return seed;
		return seed ^ (0x9E3779B97F4A7C15L * generation[depth]);
	}

	private static final String NODE = "efpd_regen";
	private static final String VISIT = "visit";
	private static final String GEN = "gen";

	public static void storeInBundle( Bundle bundle ){
		Bundle node = new Bundle();
		node.put( VISIT, lastVisit );
		node.put( GEN, generation );
		node.put( "city_visit", cityVisit );
		node.put( "city_gen", cityGen );
		bundle.put( NODE, node );
	}

	public static void restoreFromBundle( Bundle bundle ){
		reset();
		if (!bundle.contains( NODE )) return;
		Bundle node = bundle.getBundle( NODE );
		long[] v = node.getLongArray( VISIT );
		if (v != null) System.arraycopy( v, 0, lastVisit, 0, Math.min(v.length, SIZE) );
		long[] cv = node.getLongArray( "city_visit" );
		if (cv != null) System.arraycopy( cv, 0, cityVisit, 0, Math.min(cv.length, cityVisit.length) );
		int[] cg = node.getIntArray( "city_gen" );
		if (cg != null) System.arraycopy( cg, 0, cityGen, 0, Math.min(cg.length, cityGen.length) );
		int[] g = node.getIntArray( GEN );
		if (g != null) System.arraycopy( g, 0, generation, 0, Math.min(g.length, SIZE) );
	}

}
