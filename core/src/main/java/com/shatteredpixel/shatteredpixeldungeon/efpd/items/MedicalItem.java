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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.efpd.PermStats;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * Base class for medical supplies that treat injuries and bleeding.
 */
public abstract class MedicalItem extends Item {

	public static final String AC_APPLY = "APPLY";

	{
		stackable = true;
		defaultAction = AC_APPLY;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_APPLY );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );

		if (action.equals( AC_APPLY )) {
			if (!canTreat( hero )){
				GLog.w( Messages.get(this, "no_need") );
				return;
			}

			detach( hero.belongings.backpack );
			treat( hero );
			PermStats.onTreated();
			GLog.p( Messages.get(this, "used") );

			hero.sprite.operate( hero.pos );
			hero.busy();
			Sample.INSTANCE.play( Assets.Sounds.DRINK, 0.6f, 1.4f );
			hero.spend( useTime() );
			updateQuickslot();
		}
	}

	protected abstract boolean canTreat( Hero hero );

	protected abstract void treat( Hero hero );

	protected float useTime(){
		return 2f;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

}
