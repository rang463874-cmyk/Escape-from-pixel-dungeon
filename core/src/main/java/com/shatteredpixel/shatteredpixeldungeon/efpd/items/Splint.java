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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.efpd.Injuries;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/** Fully heals the worst injured limb (arms, legs or feet). */
public class Splint extends MedicalItem {

	{
		image = ItemSpriteSheet.SPLINT;
	}

	private static final Injuries.Part[] LIMBS = { Injuries.Part.ARMS, Injuries.Part.LEGS, Injuries.Part.FEET };

	private static Injuries.Part worstLimb( Hero hero ){
		Injuries injuries = hero.buff( Injuries.class );
		if (injuries == null) return null;
		Injuries.Part worst = null;
		for (Injuries.Part p : LIMBS){
			if (injuries.severity(p) > 0 && (worst == null || injuries.severity(p) > injuries.severity(worst))){
				worst = p;
			}
		}
		return worst;
	}

	@Override
	protected boolean canTreat( Hero hero ) {
		return worstLimb( hero ) != null;
	}

	@Override
	protected void treat( Hero hero ) {
		hero.buff( Injuries.class ).heal( worstLimb( hero ) );
	}

	@Override
	protected float useTime() {
		return 3f;
	}

	@Override
	public int value() {
		return 25 * quantity;
	}

}
