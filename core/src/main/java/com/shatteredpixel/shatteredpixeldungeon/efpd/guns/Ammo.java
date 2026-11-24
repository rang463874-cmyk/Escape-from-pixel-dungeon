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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/**
 * Ammunition for guns. Loaded into a gun's magazine with the RELOAD action.
 */
public abstract class Ammo extends Item {

	public enum Type {
		LIGHT,  //pistols, revolvers, SMGs
		RIFLE,  //rifles, machine guns
		SHELL   //shotguns
	}

	{
		stackable = true;
	}

	public abstract Type type();

	public static Ammo find( Hero hero, Type type ){
		for (Item i : hero.belongings.backpack){
			if (i instanceof Ammo && ((Ammo) i).type() == type) return (Ammo) i;
		}
		return null;
	}

	@Override
	public String info() {
		return super.info() + "\n\n" + Messages.get(Ammo.class, "used_by", Messages.get(Ammo.class, "guns_" + type().name().toLowerCase()));
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
		return quantity;
	}

}
