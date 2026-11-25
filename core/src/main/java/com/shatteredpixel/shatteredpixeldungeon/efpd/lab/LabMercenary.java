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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.RifleAmmo;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.ShotgunShell;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Bandage;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Random;

/**
 * Elite mercenary: heavy body armour and a rifle. Shoots from up to RANGE tiles away with a clear
 * line of fire and hits hard up close too.
 */
public class LabMercenary extends Mob {

	public static final int RANGE = 7;

	{
		spriteClass = LabMercSprite.class;

		HP = HT = 55;
		defenseSkill = 17;
		EXP = 14;
		maxLvl = 26;

		lootChance = 0.5f;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )) return true;
		return Dungeon.level.distance( pos, enemy.pos ) <= RANGE
				&& new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE ).collisionPos == enemy.pos;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 8, 16 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 28;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 2, 9 );
	}

	@Override
	public Item createLoot() {
		switch (Random.Int( 4 )){
			case 0:  return new Bandage().quantity( 2 );
			case 1:  return new ShotgunShell().quantity( Random.IntRange( 3, 6 ) );
			default: return new RifleAmmo().quantity( Random.IntRange( 5, 10 ) );
		}
	}

}
