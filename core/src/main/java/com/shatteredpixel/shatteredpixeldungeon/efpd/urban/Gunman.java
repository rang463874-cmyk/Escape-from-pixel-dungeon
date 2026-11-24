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
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.LightAmmo;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.RifleAmmo;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Random;

/**
 * Gunman: shoots from up to RANGE tiles away when it has a clear line of fire, and clubs you
 * with the grip up close. Drops ammo, sometimes its gun.
 */
public class Gunman extends Mob {

	public static final int RANGE = 7;

	{
		spriteClass = GunmanSprite.class;
		lootChance = 0.6f;
	}

	private int zone(){
		return Urban.zoneIndex( Dungeon.depth );
	}

	public Gunman(){
		super();
		int z = zone();
		HP = HT = 7 + 6*z;
		defenseSkill = 3 + 3*z;
		EXP = 4 + 2*z;
		maxLvl = 8 + 5*z;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )) return true;
		return Dungeon.level.distance( pos, enemy.pos ) <= RANGE
				&& new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE ).collisionPos == enemy.pos;
	}

	@Override
	public int damageRoll() {
		int z = zone();
		return Random.NormalIntRange( 2 + 2*z, 5 + 4*z );
	}

	@Override
	public int attackSkill( Char target ) {
		return 12 + 4*zone();
	}

	@Override
	public Item createLoot() {
		if (Random.Int( 15 ) == 0) return UrbanMobs.randomGun();
		Item ammo = zone() >= 2 && Random.Int(2) == 0 ? new RifleAmmo() : new LightAmmo();
		return ammo.quantity( Random.IntRange( 4, 8 ) );
	}

}
