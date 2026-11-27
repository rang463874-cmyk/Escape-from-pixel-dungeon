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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Lab;
import com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban;

/**
 * Enemies in deeper dungeon regions are tougher: their max HP and damage are multiplied by
 * 1 + PER_REGION * region (sewers x1.0, prison x1.3, caves x1.6, city x1.9, halls x2.2).
 * Applied once, when an enemy first appears on a dungeon floor (any mode). The ruined city and
 * the Lab have their own scaling and are left alone.
 */
public class RegionScaling {

	public static final float PER_REGION = 0.3f;

	public static int region( int depth ){
		return Math.max( 0, Math.min( 4, (depth - 1) / 5 ) );
	}

	private static boolean applies(){
		if (Urban.inCity()) return false;
		if (Dungeon.branch == Lab.LAB_BRANCH || Dungeon.branch == OpenWorld.BASE_BRANCH) return false;
		return Dungeon.depth >= 1;
	}

	public static float multiplierFor( Mob mob ){
		if (mob.alignment != Char.Alignment.ENEMY || !applies()) return 1f;
		return 1f + PER_REGION * region( Dungeon.depth );
	}

	//Yog-Dzewa's phases are fixed HP steps, so only its minions scale
	public static boolean scalesHP( Mob mob ){
		return !(mob instanceof YogDzewa);
	}

}
