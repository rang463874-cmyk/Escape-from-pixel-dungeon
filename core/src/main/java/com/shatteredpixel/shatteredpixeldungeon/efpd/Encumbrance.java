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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * Permanent hero buff that tracks the load level and shows an icon while overloaded.
 * The actual speed/evasion penalties are applied in Hero via {@link Weight}, so they
 * take effect immediately when items are picked up or dropped.
 */
public class Encumbrance extends Buff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	private int lastLevel = -1;

	@Override
	public boolean act() {
		if (target instanceof Hero){
			int lvl = Weight.level( (Hero) target );
			if (lvl != lastLevel){
				//don't announce the initial state when loading a game
				if (lastLevel != -1){
					if (lvl > lastLevel){
						GLog.w( Messages.get(this, "heavier_" + lvl) );
					} else {
						GLog.p( Messages.get(this, "lighter_" + lvl) );
					}
				}
				lastLevel = lvl;
				BuffIndicator.refreshHero();
			}
		}
		spend( TICK );
		return true;
	}

	private static final String LAST_LEVEL = "last_level";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( LAST_LEVEL, lastLevel );
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		lastLevel = bundle.contains(LAST_LEVEL) ? bundle.getInt( LAST_LEVEL ) : -1;
	}

	private int level(){
		return target instanceof Hero ? Weight.level( (Hero) target ) : 0;
	}

	@Override
	public int icon() {
		return level() == 0 ? BuffIndicator.NONE : BuffIndicator.WEIGHT;
	}

	@Override
	public void tintIcon(Image icon) {
		int c = Weight.LEVEL_COLORS[level()];
		icon.hardlight( ((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f );
	}

	@Override
	public String iconTextDisplay() {
		return target instanceof Hero ? Integer.toString(Math.round(100 * Weight.carried((Hero) target) / Weight.capacity((Hero) target))) + "%" : "";
	}

	@Override
	public String name() {
		return Messages.get(this, "name_" + Math.max(1, level()));
	}

	@Override
	public String desc() {
		int lvl = level();
		Hero hero = (Hero) target;
		return Messages.get(this, "desc",
				Weight.format(Weight.carried(hero)),
				Weight.format(Weight.capacity(hero)),
				Math.round(100 * (1f - Weight.SPEED_MULTI[lvl])),
				Math.round(100 * (1f - Weight.EVASION_MULTI[lvl])));
	}

}
