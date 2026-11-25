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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.efpd.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/** Open world: carries the hero straight back to the base town. Doesn't work during boss fights. */
public class RecallCharm extends Item {

	public static final String AC_USE = "USE";

	{
		image = ItemSpriteSheet.RECALL_CHARM;
		stackable = true;
		defaultAction = AC_USE;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_USE );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );
		if (!action.equals( AC_USE )) return;

		if (!OpenWorld.isActive()){
			GLog.w( Messages.get(this, "only_open_world") );
		} else if (OpenWorld.onBase()){
			GLog.w( Messages.get(this, "already_home") );
		} else if (com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Lab.inLab()){
			GLog.w( Messages.get(this, "lab") );
		} else if (Dungeon.level.locked){
			GLog.w( Messages.get(this, "locked") );
		} else {
			detach( hero.belongings.backpack );
			Sample.INSTANCE.play( Assets.Sounds.TELEPORT );
			GLog.i( Messages.get(this, "used") );
			OpenWorld.travelToBase();
		}
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
		return 40 * quantity;
	}

}
