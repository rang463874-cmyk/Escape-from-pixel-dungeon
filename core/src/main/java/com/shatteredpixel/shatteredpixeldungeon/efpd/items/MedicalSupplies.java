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

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Extra loot generated on every regular floor. All tuning values live here.
 */
public class MedicalSupplies {

	public static final float SECOND_BANDAGE_CHANCE = 0.5f; //one bandage is guaranteed
	public static final float SPLINT_CHANCE         = 0.4f;
	public static final float FIRST_AID_KIT_CHANCE  = 0.15f;

	public static ArrayList<Item> levelLoot(){
		ArrayList<Item> loot = new ArrayList<>();
		loot.add( new Bandage().quantity( Random.Float() < SECOND_BANDAGE_CHANCE ? 2 : 1 ) );
		if (Random.Float() < SPLINT_CHANCE)        loot.add( new Splint() );
		if (Random.Float() < FIRST_AID_KIT_CHANCE) loot.add( new FirstAidKit() );
		return loot;
	}

}
