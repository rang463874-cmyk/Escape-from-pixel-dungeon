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

package com.shatteredpixel.shatteredpixeldungeon.efpd.guns;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Ammo generated on every regular floor. Deeper regions favour rifle ammo and shells.
 * All tuning values live here.
 */
public class AmmoSupplies {

	public static final float DROP_CHANCE = 0.7f;   //chance for one stack per floor
	public static final float SECOND_DROP = 0.25f;  //chance for a second stack

	//relative chances of LIGHT / RIFLE / SHELL by region (sewers, prison, caves, city, halls)
	private static final float[][] TYPE_CHANCES = {
			{ 70, 15, 15 },
			{ 50, 25, 25 },
			{ 35, 35, 30 },
			{ 30, 40, 30 },
			{ 25, 45, 30 },
	};

	public static Item randomStack(){
		int region = Math.max(0, Math.min(4, (Dungeon.depth - 1) / 5));
		switch (Random.chances( TYPE_CHANCES[region] )){
			default: case 0: return new LightAmmo().quantity( Random.IntRange(6, 12) );
			case 1:          return new RifleAmmo().quantity( Random.IntRange(4, 8) );
			case 2:          return new ShotgunShell().quantity( Random.IntRange(3, 6) );
		}
	}

	public static ArrayList<Item> levelLoot(){
		ArrayList<Item> loot = new ArrayList<>();
		if (Random.Float() < DROP_CHANCE) loot.add( randomStack() );
		if (Random.Float() < SECOND_DROP) loot.add( randomStack() );
		return loot;
	}

}
