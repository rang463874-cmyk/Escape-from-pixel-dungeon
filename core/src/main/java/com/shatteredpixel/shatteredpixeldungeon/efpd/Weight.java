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

import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.Ammo;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.Gun;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Bandage;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Canteen;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Essence;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Material;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.RecallCharm;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Tonic;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.FirstAidKit;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Splint;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.SupplyPack;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Honeypot;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Spell;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;

import java.util.Locale;

/**
 * Weight based inventory. Everything the hero carries (equipped or not) has a weight,
 * and carrying more than the hero's capacity slows them down and makes them easier to hit.
 *
 * All tuning values live here.
 */
public class Weight {

	//capacity = BASE + PER_STR * strength
	public static final float CAPACITY_BASE    = 5f;
	public static final float CAPACITY_PER_STR = 2f;

	//load levels: 0 = fine, 1 = burdened, 2 = overloaded, 3 = crushed
	public static final float[] LEVEL_THRESHOLDS = { 1.00f, 1.25f, 1.50f }; //fraction of capacity
	public static final float[] SPEED_MULTI      = { 1f, 0.85f, 0.67f, 0.5f };
	public static final float[] EVASION_MULTI    = { 1f, 0.85f, 0.67f, 0.5f };

	public static final int[] LEVEL_COLORS = { 0xFFFFFF, 0xFFFF44, 0xFF8800, 0xFF4444 };

	//weight of a single unit of an item
	public static float unitWeight( Item item ){
		if (item instanceof Bag)            return 0.5f;

		//guns and ammo
		if (item instanceof Gun)            return ((Gun) item).weight();
		if (item instanceof Ammo)           return 0.02f;

		//weapons
		if (item instanceof Dart)           return 0.1f;
		if (item instanceof MissileWeapon)  return 0.25f * ((MissileWeapon) item).tier;
		if (item instanceof SpiritBow)      return 1.5f;
		if (item instanceof MagesStaff)     return 2f;
		if (item instanceof MeleeWeapon)    return 1.5f * ((MeleeWeapon) item).tier;

		//armor, ClassArmor uses the tier of the armor it was made from
		if (item instanceof Armor)          return 2.5f * Math.max(1, ((Armor) item).tier);

		//magic items
		if (item instanceof Wand)           return 0.5f;
		if (item instanceof Ring)           return 0.1f;
		if (item instanceof Artifact)       return 1f;
		if (item instanceof Trinket)        return 0.2f;

		//consumables
		if (item instanceof Potion)         return 0.4f;
		if (item instanceof Scroll)         return 0.1f;
		if (item instanceof Spell)          return 0.2f;
		if (item instanceof Runestone)      return 0.2f;
		if (item instanceof Plant.Seed)     return 0.05f;
		if (item instanceof Bomb)           return 1f;
		if (item instanceof SmallRation)    return 0.5f;
		if (item.getClass() == Food.class)  return 1f;
		if (item instanceof Food)           return 0.5f;

		//medical supplies
		if (item instanceof Bandage)        return 0.1f;
		if (item instanceof Splint)         return 0.5f;
		if (item instanceof FirstAidKit)    return 1f;

		//open world materials
		if (item instanceof Essence)        return 0.1f;
		if (item instanceof Material)       return ((Material) item).unitWeight();
		if (item instanceof RecallCharm)    return 0.1f;
		if (item instanceof Tonic)          return 0.4f;

		//misc
		if (item instanceof Waterskin)      return 0.8f;
		if (item instanceof Canteen)        return ((Canteen) item).weight();
		if (item instanceof Honeypot)       return 1f;

		return 0.5f;
	}

	public static float of( Item item ){
		return unitWeight(item) * Math.max(1, item.quantity());
	}

	public static float carried( Hero hero ){
		float total = 0;
		//iterates equipped items, the backpack and the contents of every bag
		for (Item i : hero.belongings){
			total += of(i);
		}
		return total;
	}

	public static float capacity( Hero hero ){
		return CAPACITY_BASE + CAPACITY_PER_STR * hero.STR() + SupplyPack.bonusFor( hero );
	}

	public static int level( Hero hero ){
		float ratio = carried(hero) / capacity(hero);
		int lvl = 0;
		for (float t : LEVEL_THRESHOLDS){
			if (ratio > t) lvl++;
		}
		return lvl;
	}

	public static float speedMultiplier( Hero hero ){
		return SPEED_MULTI[level(hero)];
	}

	public static float evasionMultiplier( Hero hero ){
		return EVASION_MULTI[level(hero)];
	}

	public static String format( float weight ){
		return String.format(Locale.ROOT, "%.1f", weight);
	}

	//e.g. "12.3/25.0"
	public static String label( Hero hero ){
		return format(carried(hero)) + "/" + format(capacity(hero));
	}

	//line appended to item descriptions
	public static String infoLine( Item item ){
		if (item.quantity() > 1){
			return Messages.get(Weight.class, "info_stack", format(unitWeight(item)), format(of(item)));
		} else {
			return Messages.get(Weight.class, "info", format(of(item)));
		}
	}

}
