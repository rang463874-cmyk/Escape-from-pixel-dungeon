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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MobSpawner;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.Gun;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/** Picks the enemies of the city: original dungeon monsters mixed with new urban enemies. */
public class UrbanMobs {

	//share of spawns that are urban enemies instead of dungeon monsters
	public static final float URBAN_SHARE = 0.4f;

	public static Mob create( Level level ){
		int zone = Urban.zoneIndex( Dungeon.depth );
		if (Random.Float() < URBAN_SHARE){
			//gunmen get more common in the outer districts
			return Random.Float() < 0.3f + 0.1f*zone ? new Gunman() : new Looter();
		}
		ArrayList<Class<? extends Mob>> rotation = MobSpawner.getMobRotation( Dungeon.depth );
		Mob m = Reflection.newInstance( Random.element( rotation ) );
		ChampionEnemy.rollForChampion( m );
		return m;
	}

	/** A random gun of the tier this district's loot table would roll. */
	public static Item randomGun(){
		Item wep = null;
		for (int tries = 0; tries < 30; tries++){
			Item i = Generator.random( Generator.Category.WEAPON );
			if (i instanceof Gun){
				wep = i;
				break;
			}
		}
		return wep != null ? wep : Generator.random( Generator.Category.WEAPON );
	}

}
