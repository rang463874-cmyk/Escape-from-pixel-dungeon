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
import com.shatteredpixel.shatteredpixeldungeon.efpd.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

/**
 * Lab keycard: the entry ticket to the Lab. Taken out of the stash when a Lab run starts.
 * Rare finds in the extraction modes, see the chances below.
 */
public class Keycard extends Item {

	//drop chances
	public static final float DM300_CHANCE      = 0.20f;
	public static final float DWARF_KING_CHANCE = 0.40f;
	public static final float YOG_CHANCE        = 1.00f;
	public static final float HVT_BASE          = 0.10f; //+10% per city district
	public static final float VAULT_CHANCE      = 0.35f;
	public static final float CITY_CHEST_CHANCE = 0.04f; //top floor chests from district 3 on

	{
		image = ItemSpriteSheet.KEYCARD;
		stackable = true;
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
		return 250 * quantity;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return new ItemSprite.Glowing( 0x33CCFF, 1.5f );
	}

	/** Keycards drop everywhere but the Lab itself and daily runs. */
	public static boolean canDrop(){
		return !Lab.isActive() && !Lab.inLab() && !Dungeon.daily;
	}

	/** Rolls a keycard for a slain dungeon boss in extraction mode; it drops at the hero's feet. */
	public static void rollForBoss( int depth ){
		if (!canDrop() || Dungeon.hero == null) return;
		float chance = depth == 15 ? DM300_CHANCE : depth == 20 ? DWARF_KING_CHANCE : depth == 25 ? YOG_CHANCE : 0;
		if (chance > 0 && Random.Float() < chance){
			Dungeon.level.drop( new Keycard(), Dungeon.hero.pos ).sprite.drop();
			GLog.p( Messages.get( Keycard.class, "found" ) );
		}
	}

}
