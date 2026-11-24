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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.AmmoSupplies;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.MedicalSupplies;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * One floor of a city building: an upper floor (more loot the higher it is, a chest on the top
 * floor) or a basement (which may hold the subway stairs to the next district).
 */
public class BuildingLevel extends Level {

	private int building, floor;

	{
		viewDistance = 8;
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_CITY;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_SEWERS;
	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.play( Assets.Music.CITY_1, true );
	}

	private final ArrayList<Integer> lootCells = new ArrayList<>();
	private int chestCell = -1;

	@Override
	protected boolean build() {
		building = Urban.buildingOf( Dungeon.branch );
		floor = Urban.floorOf( Dungeon.branch );
		int[] spec = Urban.spec( Dungeon.depth, building );
		if (spec == null) spec = new int[]{ 10, 10, 1, 0, 0 };
		int w = Math.max( 6, spec[Urban.SPEC_W] ), h = Math.max( 6, spec[Urban.SPEC_H] );
		int floors = spec[Urban.SPEC_FLOORS];
		boolean subway = spec[Urban.SPEC_SUBWAY] == 1;

		setSize( w, h );
		lootCells.clear();
		chestCell = -1;
		int tile = floor < 0 ? Terrain.EMPTY : UrbanLevel.INDOOR;
		for (int y = 1; y < h-1; y++){
			for (int x = 1; x < w-1; x++){
				//basements are damp
				map[x + y*w] = floor < 0 && Random.Int(10) == 0 ? Terrain.WATER : tile;
			}
		}
		Rooms.partition( map, w, 0, 0, w-1, h-1, tile );

		int d = Dungeon.depth;
		if (floor >= 1){
			int down = Rooms.freeCell( map, w, 0, 0, w-1, h-1, tile );
			map[down] = Terrain.EXIT;
			transitions.add( new LevelTransition( this, down, LevelTransition.Type.REGULAR_EXIT,
					d, floor == 1 ? Urban.URBAN_BRANCH : Urban.floorBranch( building, floor-1 ),
					LevelTransition.Type.REGULAR_ENTRANCE ) );
			if (floor < floors){
				int up = farFrom( down, w, h, tile );
				map[up] = Terrain.ENTRANCE;
				transitions.add( new LevelTransition( this, up, LevelTransition.Type.REGULAR_ENTRANCE,
						d, Urban.floorBranch( building, floor+1 ), LevelTransition.Type.REGULAR_EXIT ) );
			} else {
				chestCell = farFrom( down, w, h, tile );
			}
		} else {
			int up = Rooms.freeCell( map, w, 0, 0, w-1, h-1, tile );
			map[up] = Terrain.ENTRANCE;
			transitions.add( new LevelTransition( this, up, LevelTransition.Type.REGULAR_ENTRANCE,
					d, Urban.URBAN_BRANCH, LevelTransition.Type.REGULAR_EXIT ) );
			if (subway){
				int s = farFrom( up, w, h, tile );
				map[s] = Terrain.EXIT;
				transitions.add( new LevelTransition( this, s, LevelTransition.Type.BRANCH_EXIT,
						Urban.nextZoneDepth( d ), Urban.URBAN_BRANCH, LevelTransition.Type.BRANCH_ENTRANCE ) );
			}
		}

		Rooms.furnish( map, w, 0, 0, w-1, h-1, tile );

		//higher floors hold more
		int items = floor < 0 ? Random.IntRange( 1, 2 ) : 1 + floor + Random.Int( 2 );
		for (int i = 0; i < items; i++){
			int cell = Rooms.freeCell( map, w, 0, 0, w-1, h-1, tile );
			if (cell != -1 && cell != chestCell) lootCells.add( cell );
		}
		return true;
	}

	private int farFrom( int from, int w, int h, int tile ){
		int best = -1;
		for (int tries = 0; tries < 20; tries++){
			int c = Rooms.freeCell( map, w, 0, 0, w-1, h-1, tile );
			if (c == -1) continue;
			if (best == -1 || distance( c, from ) > distance( best, from )) best = c;
		}
		return best;
	}

	@Override
	protected void createMobs() {
		int count = Random.IntRange( 1, 2 ) + (floor >= 2 ? 1 : 0);
		for (int i = 0; i < count; i++){
			Mob mob = UrbanMobs.create( this );
			for (int tries = 0; tries < 40; tries++){
				int cell = Random.Int( length() );
				if (!passable[cell] || findMob( cell ) != null || isTransitionCell( cell )) continue;
				boolean nearStairs = false;
				for (LevelTransition t : transitions) if (distance( cell, t.centerCell ) < 4) nearStairs = true;
				if (nearStairs) continue;
				mob.pos = cell;
				mobs.add( mob );
				break;
			}
		}
	}

	@Override
	public Mob createMob() {
		return UrbanMobs.create( this );
	}

	@Override
	public com.shatteredpixel.shatteredpixeldungeon.actors.Actor addRespawner() {
		//cleared buildings stay cleared
		return null;
	}

	@Override
	protected void createItems() {
		for (int cell : lootCells){
			Item item = Generator.random();
			if (item != null) drop( item, cell );
		}
		if (floor < 0){
			for (Item i : AmmoSupplies.levelLoot()) drop( i, randomFloorCell() );
			if (Random.Int(3) == 0) for (Item i : MedicalSupplies.levelLoot()) drop( i, randomFloorCell() );
		}
		//the top floor keeps a locked-away stash
		if (chestCell != -1){
			Item prize = Random.Int(10) < 3 ? UrbanMobs.randomGun() : Generator.random();
			Heap heap = drop( prize, chestCell );
			heap.type = Heap.Type.CHEST;
			Item second = Generator.random();
			if (second != null) heap.drop( second );
		}
	}

	private int randomFloorCell(){
		for (int tries = 0; tries < 50; tries++){
			int cell = Random.Int( length() );
			if (passable[cell] && !isTransitionCell( cell )) return cell;
		}
		return transitions.get(0).centerCell;
	}

	@Override
	public boolean activateTransition( Hero hero, LevelTransition transition ) {
		if (locked) return false;
		Urban.arrivalFrom = Dungeon.branch;
		Urban.leftStart = true;
		return performTransition( hero, transition );
	}

	private static final String BUILDING = "efpd_building";
	private static final String FLOOR = "efpd_floor";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( BUILDING, building );
		bundle.put( FLOOR, floor );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		building = bundle.getInt( BUILDING );
		floor = bundle.getInt( FLOOR );
	}


	//safe during generation (Level.getTransition(int) reads the current level)
	private boolean isTransitionCell( int cell ){
		for (LevelTransition t : transitions) if (t.centerCell == cell) return true;
		return false;
	}

}
