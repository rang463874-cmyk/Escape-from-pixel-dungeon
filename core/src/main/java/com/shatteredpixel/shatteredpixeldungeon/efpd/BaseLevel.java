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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.noosa.audio.Music;

/**
 * The open world base town: a safe walled square with the base keeper (stash, shop,
 * region map), an alchemy pot, and the dungeon entrance stairs that open the region map.
 */
public class BaseLevel extends Level {

	private static final int W = 17;
	private static final int H = 13;

	//layout: # wall, . floor, , decorated floor, " grass, E dungeon entrance, A alchemy pot, K keeper, ~ water, T statue, W well
	private static final String[] MAP = {
			"#################",
			"#\"\"\".........\"\"\"#",
			"#\"T...........T\"#",
			"#.....,,,,,.....#",
			"#.....,.K.,.....#",
			"#..A..,,,,,..~~.#",
			"#............~~.#",
			"#.......,.......#",
			"#..W...,E,......#",
			"#.......,.......#",
			"#\"T...........T\"#",
			"#\"\"\".........\"\"\"#",
			"#################",
	};

	{
		color1 = 0x4b6636;
		color2 = 0xf2f2f2;
		viewDistance = 12;
	}

	private int keeperPos;

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
		Music.INSTANCE.play( Assets.Music.THEME_1, true );
	}

	@Override
	protected boolean build() {
		setSize( W, H );
		int exit = 0;
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				int cell = x + y * W;
				switch (MAP[y].charAt(x)){
					case '#': map[cell] = Terrain.WALL; break;
					case ',': map[cell] = Terrain.EMPTY_DECO; break;
					case '"': map[cell] = Terrain.GRASS; break;
					case '~': map[cell] = Terrain.WATER; break;
					case 'T': map[cell] = Terrain.STATUE; break;
					case 'W': map[cell] = Terrain.EMPTY_WELL; break;
					case 'A': map[cell] = Terrain.ALCHEMY; break;
					case 'E': map[cell] = Terrain.EXIT; exit = cell; break;
					case 'K': map[cell] = Terrain.EMPTY_SP; keeperPos = cell; break;
					default:  map[cell] = Terrain.EMPTY; break;
				}
			}
		}
		//the hero arrives on (and leaves from) the dungeon entrance
		transitions.add( new LevelTransition( this, exit, LevelTransition.Type.REGULAR_EXIT ) );
		return true;
	}

	@Override
	public boolean activateTransition( Hero hero, LevelTransition transition ) {
		WndWorldMap.show();
		return false;
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	protected void createMobs() {
		BaseKeeper keeper = new BaseKeeper();
		keeper.pos = keeperPos;
		mobs.add( keeper );
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		return -1;
	}

}
