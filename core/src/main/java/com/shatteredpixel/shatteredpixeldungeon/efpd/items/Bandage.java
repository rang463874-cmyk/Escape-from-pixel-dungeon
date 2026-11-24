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

package com.shatteredpixel.shatteredpixeldungeon.efpd.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.efpd.Hemorrhage;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** Stops bleeding. Does not heal injuries. */
public class Bandage extends MedicalItem {

	{
		image = ItemSpriteSheet.BANDAGE;
	}

	@Override
	protected boolean canTreat( Hero hero ) {
		return hero.buff( Hemorrhage.class ) != null;
	}

	@Override
	protected void treat( Hero hero ) {
		Buff.detach( hero, Hemorrhage.class );
	}

	@Override
	protected float useTime() {
		return 1f;
	}

	@Override
	public int value() {
		return 10 * quantity;
	}

}
