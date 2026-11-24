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

package com.shatteredpixel.shatteredpixeldungeon.efpd.urban;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.watabou.utils.Bundle;

import java.util.HashMap;

/**
 * Urban warfare mode: a wide procedural city instead of dungeon floors. The hero starts in the
 * middle of the first district and escapes through gaps in the city wall (extraction rules).
 *
 * Levels are saved per (depth, branch):
 *  - a district (outdoors) uses branch URBAN_BRANCH and a depth from ZONE_DEPTHS, which sets
 *    how strong enemies and loot are (the depths skip boss and shop floors)
 *  - a building floor uses branch 100 + building*10 + (floor+1), floor -1 being the basement
 *
 * Building layouts are recorded here when a district is generated, so their floors can be
 * built later with the right size.
 */
public class Urban {

	public static final int URBAN_BRANCH = 3;
	public static final int[] ZONE_DEPTHS = { 2, 7, 12, 17, 22 };

	//set by the mode picker before a new game starts
	public static boolean selectedMode = false;

	public static boolean isActive(){
		return Statistics.efpdUrban;
	}

	public static int zoneIndex( int depth ){
		for (int i = 0; i < ZONE_DEPTHS.length; i++) if (ZONE_DEPTHS[i] == depth) return i;
		return 0;
	}

	public static boolean lastZone( int depth ){
		return zoneIndex( depth ) >= ZONE_DEPTHS.length - 1;
	}

	public static int nextZoneDepth( int depth ){
		return ZONE_DEPTHS[Math.min( ZONE_DEPTHS.length - 1, zoneIndex( depth ) + 1 )];
	}

	public static int prevZoneDepth( int depth ){
		return ZONE_DEPTHS[Math.max( 0, zoneIndex( depth ) - 1 )];
	}

	//--- building floors -------------------------------------------------------------------------

	public static int floorBranch( int building, int floor ){
		return 100 + building*10 + (floor + 1);
	}

	public static boolean isFloorBranch( int branch ){
		return branch >= 100;
	}

	public static int buildingOf( int branch ){
		return (branch - 100) / 10;
	}

	public static int floorOf( int branch ){
		return (branch - 100) % 10 - 1;
	}

	//the ruined city, in either game mode
	public static boolean inCity( int branch ){
		return branch == URBAN_BRANCH || isFloorBranch( branch );
	}

	public static boolean inCity(){
		return inCity( Dungeon.branch );
	}

	public static boolean outdoors(){
		return isActive() && Dungeon.branch == URBAN_BRANCH;
	}

	//building spec: width, height, floors above ground, has basement, has subway, interior floor tile
	public static final int SPEC_W = 0, SPEC_H = 1, SPEC_FLOORS = 2, SPEC_BASEMENT = 3, SPEC_SUBWAY = 4;
	public static final int SPEC_SIZE = 5;

	private static HashMap<Integer, int[]> specs = new HashMap<>(); //zone depth -> flattened specs

	public static void recordBuildings( int depth, int[] flat ){
		specs.put( depth, flat );
	}

	public static int[] spec( int depth, int building ){
		int[] flat = specs.get( depth );
		if (flat == null || (building+1)*SPEC_SIZE > flat.length) return null;
		int[] s = new int[SPEC_SIZE];
		System.arraycopy( flat, building*SPEC_SIZE, s, 0, SPEC_SIZE );
		return s;
	}

	public static boolean canWithdraw(){
		if (leftStart || Dungeon.depth != ZONE_DEPTHS[0] || Dungeon.branch != URBAN_BRANCH) return false;
		if (!(Dungeon.level instanceof UrbanLevel) || Dungeon.hero == null) return false;
		return Dungeon.level.distance( Dungeon.hero.pos, ((UrbanLevel) Dungeon.level).startCell ) <= 5;
	}

	//--- travel bookkeeping ----------------------------------------------------------------------

	//the branch the hero is leaving, used to put them on the right stairs on arrival
	public static int arrivalFrom = -1;
	//the hero left the starting plaza, the stash is closed from now on
	public static boolean leftStart = false;

	//set when the hero comes up from the subway into a new district, shown once on arrival
	private static boolean zoneArrival = false;

	public static void markZoneArrival(){
		zoneArrival = true;
	}

	public static boolean consumeZoneArrival(){
		boolean a = zoneArrival;
		zoneArrival = false;
		return a;
	}

	public static void reset(){
		specs = new HashMap<>();
		arrivalFrom = -1;
		leftStart = false;
	}

	private static final String NODE  = "efpd_urban";
	private static final String ZONES = "zones";
	private static final String LEFT  = "left_start";

	public static void storeInBundle( Bundle bundle ){
		Bundle node = new Bundle();
		int[] zones = new int[specs.size()];
		int i = 0;
		for (Integer d : specs.keySet()){
			zones[i++] = d;
			node.put( "z" + d, specs.get( d ) );
		}
		node.put( ZONES, zones );
		node.put( LEFT, leftStart );
		bundle.put( NODE, node );
	}

	public static void restoreFromBundle( Bundle bundle ){
		reset();
		if (!bundle.contains( NODE )) return;
		Bundle node = bundle.getBundle( NODE );
		int[] zones = node.getIntArray( ZONES );
		if (zones != null) for (int d : zones){
			int[] flat = node.getIntArray( "z" + d );
			if (flat != null) specs.put( d, flat );
		}
		leftStart = node.getBoolean( LEFT );
	}

}
