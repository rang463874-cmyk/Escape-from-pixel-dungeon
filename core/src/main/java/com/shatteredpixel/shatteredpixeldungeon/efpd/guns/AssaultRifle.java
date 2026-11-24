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

public class AssaultRifle extends Gun {

	{
		image = ItemSpriteSheet.ASSAULT_RIFLE;
		tier = 4;

		ammoType    = Ammo.Type.RIFLE;
		magSize     = 20;
		damageMulti = 0.9f;
		shotDelay   = 0.7f;
		reloadTime  = 3.0f;
		closeAcc    = 1.0f;
		farAcc      = 1.05f;
		falloff     = false;
		noise       = 11;
		weight      = 4.5f;
		extraStr    = 0;
	}

}
