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

package com.shatteredpixel.shatteredpixeldungeon.efpd.hvt;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.Ammo;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.Gun;
import com.shatteredpixel.shatteredpixeldungeon.efpd.uniques.UniqueWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/** The personal gear of high value targets, dropped when they die (unless they carried a vault key). */
public class HvtGear {

	public static Item forKind( int kind, int zone ){
		Weapon w;
		switch (kind){
			case Hvt.WARLORD: w = new CommandersCarbine(); break;
			case Hvt.WARLOCK: w = new HexStaff(); break;
			default:          w = new ButcherCleaver(); break;
		}
		w.cursed = false;
		w.enchant( null );
		int lvl = zone / 2;
		if (lvl > 0) w.upgrade( lvl );
		w.identify();
		if (w instanceof Gun) ((Gun) w).fill();
		return w;
	}

	//--- warlord: a well kept carbine, accurate at range and quiet ---------------------------------
	public static class CommandersCarbine extends Gun {
		{
			image = ItemSpriteSheet.ASSAULT_RIFLE;
			tier = 4;

			ammoType    = Ammo.Type.RIFLE;
			magSize     = 24;
			damageMulti = 1.05f;
			shotDelay   = 0.6f;
			reloadTime  = 2.5f;
			closeAcc    = 1.0f;
			farAcc      = 1.25f;
			falloff     = false;
			noise       = 6;
			weight      = 4f;
			extraStr    = 0;
		}

		@Override
		public ItemSprite.Glowing glowing() {
			ItemSprite.Glowing ench = super.glowing();
			return ench != null ? ench : new ItemSprite.Glowing( 0xDDAA33, 2f );
		}

		@Override
		public String desc() {
			return super.desc() + "\n\n" + Messages.get( this, "effect" );
		}

		@Override
		public int value() {
			return 400 + super.value();
		}
	}

	//--- warlock: a hexing staff that leaves its victims weak and exposed (tier 3) ------------------
	public static class HexStaff extends UniqueWeapon {
		{
			image = ItemSpriteSheet.QUARTERSTAFF;
			hitSound = Assets.Sounds.HIT_MAGIC;
			hitSoundPitch = 0.9f;
			tier = 3;
		}
		@Override protected int glowColor() { return 0x8822CC; }
		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (defender.isAlive() && Random.Float() < procChance( 0.25f )){
				Buff.prolong( defender, Weakness.class, 4f );
				Buff.prolong( defender, Vulnerable.class, 4f );
			}
			return super.proc( attacker, defender, damage );
		}
	}

	//--- butcher: a heavy cleaver that opens bleeding wounds (tier 4) -------------------------------
	public static class ButcherCleaver extends UniqueWeapon {
		{
			image = ItemSpriteSheet.BATTLE_AXE;
			hitSound = Assets.Sounds.HIT_SLASH;
			hitSoundPitch = 0.8f;
			tier = 4;
		}
		@Override protected int glowColor() { return 0xCC1111; }
		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (defender.isAlive() && Random.Float() < procChance( 0.30f )){
				Buff.affect( defender, Bleeding.class ).set( Math.max( 2, damage / 2f ) );
			}
			return super.proc( attacker, defender, damage );
		}
	}

}
