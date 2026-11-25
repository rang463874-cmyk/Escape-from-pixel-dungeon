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
import com.shatteredpixel.shatteredpixeldungeon.efpd.PermStats;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Essence;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.FirstAidKit;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.RecallCharm;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Tonic;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Random;

/**
 * Rare city loot, found in the chests on the top floors of buildings (and now and then on any
 * building floor). Deeper districts give better tonics and essences, which tie the city to the
 * open world's permanent stats.
 */
public class CityLoot {

	//chance for a regular item on a building floor to be rare instead
	public static final float RARE_FLOOR_CHANCE = 0.15f;

	public static Item rare(){
		int z = Urban.zoneIndex( Dungeon.depth );
		//from district 3 on, a Lab keycard now and then
		if (z >= 2 && com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard.canDrop() && Random.Float() < com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard.CITY_CHEST_CHANCE){
			return new com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard();
		}
		Item item;
		switch (Random.chances( new float[]{ 30, 20, 15, 10, 10, 15 } )){
			case 0:  item = UrbanMobs.randomGun(); break;
			case 1:  item = Tonic.create( Random.element( PermStats.Stat.values() ), Math.min( 5, z + 1 ) ); break;
			case 2:  item = Essence.forRegion( z ).quantity( 3 ); break;
			case 3:  item = new FirstAidKit(); break;
			case 4:  item = new RecallCharm().quantity( 2 ); break;
			default:
				Generator.Category cat = Random.element( new Generator.Category[]{
						Generator.Category.RING, Generator.Category.WAND, Generator.Category.ARTIFACT } );
				item = Generator.random( cat );
				break;
		}
		return item != null ? item : Generator.random();
	}

	/** A regular building item, sometimes swapped for a rare one. */
	public static Item floorItem(){
		return Random.Float() < RARE_FLOOR_CHANCE ? rare() : Generator.random();
	}

}
