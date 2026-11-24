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

public class PumpShotgun extends Gun {

	{
		image = ItemSpriteSheet.PUMP_SHOTGUN;
		tier = 3;

		ammoType    = Ammo.Type.SHELL;
		magSize     = 5;
		damageMulti = 1.5f;
		shotDelay   = 1.2f;
		reloadTime  = 4.0f;
		closeAcc    = 1.4f;
		farAcc      = 0.5f;
		falloff     = true;
		noise       = 10;
		weight      = 4.0f;
		extraStr    = 0;
	}

}
