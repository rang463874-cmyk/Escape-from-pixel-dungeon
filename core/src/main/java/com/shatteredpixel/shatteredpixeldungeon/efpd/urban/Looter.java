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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Bandage;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.watabou.utils.Random;

/** Looter: a fast scavenger with a crowbar. Stronger in the outer districts. */
public class Looter extends Mob {

	{
		spriteClass = LooterSprite.class;
		baseSpeed = 1.5f;
		lootChance = 0.4f;
	}

	private int zone(){
		return Urban.zoneIndex( Dungeon.depth );
	}

	public Looter(){
		super();
		int z = zone();
		HP = HT = 10 + 7*z;
		defenseSkill = 4 + 3*z;
		EXP = 3 + 2*z;
		maxLvl = 7 + 5*z;
	}

	@Override
	public int damageRoll() {
		int z = zone();
		return Random.NormalIntRange( 1 + 2*z, 4 + 4*z );
	}

	@Override
	public int attackSkill( Char target ) {
		return 10 + 4*zone();
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 1 + zone() );
	}

	@Override
	public Item createLoot() {
		switch (Random.Int(3)){
			case 0:  return new Gold().random();
			case 1:  return new Bandage();
			default: return new SmallRation();
		}
	}

}
