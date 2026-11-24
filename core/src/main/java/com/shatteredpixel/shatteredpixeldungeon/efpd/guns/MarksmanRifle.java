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

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

public class MarksmanRifle extends Gun {

	{
		image = ItemSpriteSheet.MARKSMAN_RIFLE;
		tier = 4;

		ammoType    = Ammo.Type.RIFLE;
		magSize     = 8;
		damageMulti = 1.4f;
		shotDelay   = 1.3f;
		reloadTime  = 3.0f;
		closeAcc    = 0.7f;
		farAcc      = 1.4f;
		falloff     = false;
		noise       = 11;
		weight      = 5.0f;
		extraStr    = 0;
	}

}
