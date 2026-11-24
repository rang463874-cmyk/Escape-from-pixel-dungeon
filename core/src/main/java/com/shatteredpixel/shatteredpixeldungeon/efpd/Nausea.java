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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * Upset stomach from drinking raw water: while it lasts the hero can't eat or drink.
 */
public class Nausea extends FlavourBuff {

	public static final float DURATION = 100f;
	//chance that a sip of raw (unboiled) water causes nausea
	public static final float RAW_WATER_CHANCE = 0.3f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public static boolean blocks( Char ch ){
		if (ch.buff( Nausea.class ) != null){
			GLog.w( Messages.get(Nausea.class, "blocked") );
			return true;
		}
		return false;
	}

	@Override
	public int icon() {
		return BuffIndicator.NAUSEA;
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns());
	}

}
