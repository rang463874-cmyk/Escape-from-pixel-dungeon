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

import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Random;

/**
 * Shared helpers that lay out the inside of a building (on the street map or on its own floor).
 * Rectangles are inclusive and include the outer wall.
 */
class Rooms {

	static boolean is( int[] map, int w, int x, int y, int terrain ){
		return map[x + y*w] == terrain;
	}

	/** Splits a building into 1, 2 or 4 rooms with inner walls, keeping every room connected. */
	static void partition( int[] map, int W, int x0, int y0, int x1, int y1, int floor ){
		int w = x1-x0+1, h = y1-y0+1;
		int px = -1, py = -1;
		if (w >= 10) px = x0 + Random.IntRange( 4, w-5 );
		if (h >= 10 && (px == -1 || Random.Int(2) == 0)) py = y0 + Random.IntRange( 4, h-5 );

		if (px != -1) for (int y = y0+1; y < y1; y++) map[px + y*W] = Terrain.WALL;
		if (py != -1) for (int x = x0+1; x < x1; x++) map[x + py*W] = Terrain.WALL;

		//doorways: the vertical wall needs one per side of the horizontal wall, and vice versa
		if (px != -1){
			if (py == -1){
				map[px + Random.IntRange( y0+1, y1-1 )*W] = Terrain.DOOR;
			} else {
				map[px + Random.IntRange( y0+1, py-1 )*W] = Terrain.DOOR;
				map[px + Random.IntRange( py+1, y1-1 )*W] = Terrain.DOOR;
			}
		}
		if (py != -1){
			if (px == -1){
				map[Random.IntRange( x0+1, x1-1 ) + py*W] = Terrain.DOOR;
			} else {
				map[Random.IntRange( x0+1, px-1 ) + py*W] = Terrain.DOOR;
				map[Random.IntRange( px+1, x1-1 ) + py*W] = Terrain.DOOR;
			}
		}
	}

	/** Opens a door in the outer wall, where the room inside is walkable. */
	static int addOuterDoor( int[] map, int W, int x0, int y0, int x1, int y1, int floor ){
		for (int tries = 0; tries < 30; tries++){
			int x, y, ix, iy;
			switch (Random.Int(4)){
				case 0:  x = Random.IntRange( x0+1, x1-1 ); y = y0; ix = x; iy = y+1; break;
				case 1:  x = Random.IntRange( x0+1, x1-1 ); y = y1; ix = x; iy = y-1; break;
				case 2:  y = Random.IntRange( y0+1, y1-1 ); x = x0; ix = x+1; iy = y; break;
				default: y = Random.IntRange( y0+1, y1-1 ); x = x1; ix = x-1; iy = y; break;
			}
			if (is( map, W, ix, iy, floor ) && is( map, W, x, y, Terrain.WALL )){
				map[x + y*W] = Terrain.DOOR;
				return x + y*W;
			}
		}
		return -1;
	}

	private static boolean nearDoor( int[] map, int W, int x, int y ){
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				int t = map[(x+dx) + (y+dy)*W];
				if (t == Terrain.DOOR || t == Terrain.ENTRANCE || t == Terrain.EXIT) return true;
			}
		}
		return false;
	}

	/** A random floor cell inside the building, not next to a door or stairs. -1 if none. */
	static int freeCell( int[] map, int W, int x0, int y0, int x1, int y1, int floor ){
		for (int tries = 0; tries < 60; tries++){
			int x = Random.IntRange( x0+1, x1-1 ), y = Random.IntRange( y0+1, y1-1 );
			if (is( map, W, x, y, floor ) && !nearDoor( map, W, x, y )) return x + y*W;
		}
		return -1;
	}

	/** Shelves and statues in room corners: they never block a path through a room. */
	static void furnish( int[] map, int W, int x0, int y0, int x1, int y1, int floor ){
		for (int y = y0+1; y < y1; y++){
			for (int x = x0+1; x < x1; x++){
				if (!is( map, W, x, y, floor ) || nearDoor( map, W, x, y )) continue;
				boolean wallV = is( map, W, x, y-1, Terrain.WALL ) || is( map, W, x, y+1, Terrain.WALL );
				boolean wallH = is( map, W, x-1, y, Terrain.WALL ) || is( map, W, x+1, y, Terrain.WALL );
				if (wallV && wallH && Random.Float() < 0.35f){
					map[x + y*W] = Random.Int(5) < 3 ? Terrain.BOOKSHELF : Terrain.STATUE;
				}
			}
		}
	}

}
