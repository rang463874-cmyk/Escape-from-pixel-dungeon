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

public class PocketRevolver extends Gun {

	{
		image = ItemSpriteSheet.POCKET_REVOLVER;
		tier = 1;

		ammoType    = Ammo.Type.LIGHT;
		magSize     = 5;
		damageMulti = 0.8f;
		shotDelay   = 1.0f;
		reloadTime  = 3.0f;
		closeAcc    = 1.0f;
		farAcc      = 0.8f;
		falloff     = false;
		noise       = 7;
		weight      = 1.0f;
		extraStr    = 0;
	}

}
