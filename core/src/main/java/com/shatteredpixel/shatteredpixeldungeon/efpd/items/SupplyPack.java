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
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.Ammo;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * Supply pack: a bag for building materials, essences and ammo that also raises the carry
 * capacity. Open world heroes start with one; it is upgraded at the base (levels 1-3).
 *
 * All tuning values live here.
 */
public class SupplyPack extends Bag {

	public static final int MAX_LEVEL = 3;
	//slots and carry capacity bonus per level (the inventory shows at most 19 slots in a bag)
	public static final int[]   SLOTS        = { 8, 13, 19 };
	public static final float[] WEIGHT_BONUS = { 4f, 8f, 12f };
	//upgrade to level 2 / 3: gold, cloth, wood, stone, metal
	public static final int[][] UPGRADE_COST = { {250, 6, 4, 0, 0}, {700, 10, 0, 0, 5} };

	private int level = 1;

	{
		image = ItemSpriteSheet.SUPPLY_PACK;
	}

	public int packLevel(){
		return level;
	}

	public void upgradePack(){
		level = Math.min( MAX_LEVEL, level + 1 );
	}

	public int[] nextCost(){
		return level >= MAX_LEVEL ? null : com.shatteredpixel.shatteredpixeldungeon.efpd.Facilities.withAllyDiscount( UPGRADE_COST[level - 1] );
	}

	public float weightBonus(){
		return WEIGHT_BONUS[level - 1];
	}

	//carry capacity bonus from the pack the hero carries (the best one, if several)
	public static float bonusFor( Hero hero ){
		float best = 0;
		for (Item i : hero.belongings.backpack.items){
			if (i instanceof SupplyPack) best = Math.max( best, ((SupplyPack) i).weightBonus() );
		}
		return best;
	}

	/** Small icon for bag tabs. */
	public static Image tabIcon(){
		ItemSprite s = new ItemSprite( ItemSpriteSheet.SUPPLY_PACK );
		s.scale.set( 0.8f );
		return s;
	}

	@Override
	public boolean canHold( Item item ) {
		if (item instanceof Material || item instanceof Essence || item instanceof Ammo){
			return super.canHold( item );
		}
		return false;
	}

	@Override
	public int capacity() {
		return SLOTS[level - 1];
	}

	@Override
	public String name() {
		return Messages.get(SupplyPack.class, "leveled", super.name(), level);
	}

	@Override
	public String desc() {
		return super.desc() + "\n\n" + Messages.get(SupplyPack.class, "stats",
				capacity(), Math.round(weightBonus()));
	}

	@Override
	public int value() {
		return 40 * level;
	}

	private static final String LEVEL = "pack_level";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEVEL, level );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		//the level decides the capacity, so it is restored before the contents
		level = Math.max( 1, Math.min( MAX_LEVEL, bundle.getInt( LEVEL ) ) );
		super.restoreFromBundle( bundle );
	}

}
