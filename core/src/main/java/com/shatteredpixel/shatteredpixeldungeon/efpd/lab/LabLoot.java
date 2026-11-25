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

package com.shatteredpixel.shatteredpixeldungeon.efpd.lab;

import com.shatteredpixel.shatteredpixeldungeon.efpd.PermStats;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.FirstAidKit;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Tonic;
import com.shatteredpixel.shatteredpixeldungeon.efpd.urban.UrbanMobs;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.watabou.utils.Random;

/** What the Lab has lying around: the best loot in the game. */
public class LabLoot {

	public static Item normal(){
		switch (Random.chances( new float[]{ 30, 25, 20, 25 } )){
			case 0:  return Generator.random( Generator.Category.POTION );
			case 1:  return Generator.random( Generator.Category.SCROLL );
			case 2:  return new FirstAidKit();
			default: return Generator.random();
		}
	}

	public static Item rare(){
		Item item;
		switch (Random.chances( new float[]{ 20, 20, 15, 10, 20, 15 } )){
			case 0:
				item = UrbanMobs.randomGun();
				upgrade( item );
				break;
			case 1:
				item = Tonic.create( Random.element( PermStats.Stat.values() ), 5 );
				break;
			case 2:
				item = new ScrollOfUpgrade().quantity( Random.IntRange( 1, 2 ) );
				break;
			case 3:
				item = new PotionOfStrength();
				break;
			case 4:
				item = Generator.random( Random.element( new Generator.Category[]{
						Generator.Category.RING, Generator.Category.WAND, Generator.Category.ARTIFACT } ) );
				upgrade( item );
				break;
			default:
				item = Generator.random( Generator.Category.ARMOR );
				upgrade( item );
				break;
		}
		return item != null ? item : Generator.random();
	}

	private static void upgrade( Item item ){
		if (item == null || item instanceof Artifact || !item.isUpgradable()) return;
		item.cursed = false;
		int lvl = Random.IntRange( 1, 2 );
		for (int i = 0; i < lvl; i++) item.upgrade();
		item.identify();
	}

}
