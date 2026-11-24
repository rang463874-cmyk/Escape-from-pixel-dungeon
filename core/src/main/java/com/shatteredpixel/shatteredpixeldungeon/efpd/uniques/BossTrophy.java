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

package com.shatteredpixel.shatteredpixeldungeon.efpd.uniques;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * Boss trophies added by the mod, crafted into unique weapons. The Goo's blobs and DM-300's
 * metal shards already exist in the original game; Tengu and the Dwarf King now drop these.
 */
public abstract class BossTrophy extends Item {

	{
		stackable = true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 40 * quantity;
	}

	@Override
	public int energyVal() {
		return 3 * quantity;
	}

	/** Drops 2-4 trophies around a slain boss, like the Goo and DM-300 do. */
	public static void dropAround( int pos, Class<? extends BossTrophy> cls ){
		int count = Random.chances( new float[]{0, 0, 6, 3, 1} );
		for (int i = 0; i < count; i++){
			int cell = pos;
			for (int tries = 0; tries < 20; tries++){
				int c = pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
				if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c]){
					cell = c;
					break;
				}
			}
			try {
				Dungeon.level.drop( cls.newInstance(), cell ).sprite.drop( pos );
			} catch (Exception e) {
				//not reachable, trophies have public no-arg constructors
			}
		}
	}

	public static class TenguFeather extends BossTrophy {
		{ image = ItemSpriteSheet.TENGU_FEATHER; }
	}

	public static class RoyalSeal extends BossTrophy {
		{ image = ItemSpriteSheet.ROYAL_SEAL; }
	}

}
