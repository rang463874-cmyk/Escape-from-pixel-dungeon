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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CeremonialCandle;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Embers;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.VaultBeacon;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * The persistent stash ("hideout storage") that survives between runs.
 *
 * Rules of the extraction loop:
 *  - Items are only added to the stash when the hero successfully extracts
 *    (floor 1 entrance, or the up stairs of a boss floor after the boss is slain).
 *  - The starting kit and bags are never stored (bag contents are).
 *  - If the hero dies, everything they carried into the dungeon is lost.
 *  - Items can only be withdrawn at the start of a run, before descending.
 *
 * The stash is always re-read from disk before use, so items are instantiated
 * inside the current run (potion colours, ring gems etc. follow that run's seed).
 */
public class Stash {

	public static final String STASH_FILE = "stash.dat";

	private static final String ITEMS = "items";
	private static final String GOLD  = "gold";
	private static final String ENERGY = "energy";
	private static final String LAB_CLEARS = "lab_clears";

	public static ArrayList<Item> items = new ArrayList<>();
	public static int gold = 0;
	//alchemical energy, only usable at the hideout alchemy station
	public static int energy = 0;
	//EFPD: times the hero escaped the Lab, the final destination
	public static int labClears = 0;

	//quest items and other run-bound items which make no sense in a new run
	private static final Class<?>[] NOT_STASHABLE = new Class<?>[]{
			Amulet.class,
			CorpseDust.class,
			Embers.class,
			DwarfToken.class,
			CeremonialCandle.class,
			Pickaxe.class,
			EscapeCrystal.class,
			VaultBeacon.class
	};

	public static void load(){
		items = new ArrayList<>();
		gold = 0;
		energy = 0;
		labClears = 0;
		try {
			Bundle bundle = FileUtils.bundleFromFile( STASH_FILE );
			for (Bundlable b : bundle.getCollection( ITEMS )){
				if (b instanceof Item) items.add( (Item)b );
			}
			gold = bundle.getInt( GOLD );
			energy = bundle.getInt( ENERGY );
			labClears = bundle.getInt( LAB_CLEARS );
			//stashes saved before sorting existed are shown sorted too
			sort();
		} catch (IOException e) {
			//no stash yet, start empty
		}
	}

	public static void save(){
		sort();
		Bundle bundle = new Bundle();
		bundle.put( ITEMS, items );
		bundle.put( GOLD, gold );
		bundle.put( ENERGY, energy );
		bundle.put( LAB_CLEARS, labClears );
		try {
			FileUtils.bundleToFile( STASH_FILE, bundle );
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
		}
	}

	//category order for the stash display, unlisted items go last
	private static final Class<?>[] SORT_ORDER = {
			com.shatteredpixel.shatteredpixeldungeon.efpd.uniques.UniqueWeapon.class,
			com.shatteredpixel.shatteredpixeldungeon.efpd.guns.Gun.class,
			com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.class,
			com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon.class,
			com.shatteredpixel.shatteredpixeldungeon.efpd.guns.Ammo.class,
			Armor.class,
			com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.class,
			com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring.class,
			com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact.class,
			com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket.class,
			com.shatteredpixel.shatteredpixeldungeon.efpd.items.Tonic.class,
			com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.class,
			com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll.class,
			com.shatteredpixel.shatteredpixeldungeon.items.spells.Spell.class,
			com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone.class,
			com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb.class,
			com.shatteredpixel.shatteredpixeldungeon.efpd.items.MedicalItem.class,
			com.shatteredpixel.shatteredpixeldungeon.items.food.Food.class,
			com.shatteredpixel.shatteredpixeldungeon.plants.Plant.Seed.class,
			com.shatteredpixel.shatteredpixeldungeon.efpd.uniques.BossTrophy.class,
			com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob.class,
			com.shatteredpixel.shatteredpixeldungeon.items.quest.MetalShard.class,
			com.shatteredpixel.shatteredpixeldungeon.efpd.items.Essence.class,
			com.shatteredpixel.shatteredpixeldungeon.efpd.items.Material.class,
	};

	private static int category( Item item ){
		for (int i = 0; i < SORT_ORDER.length; i++){
			if (SORT_ORDER[i].isInstance( item )) return i;
		}
		return SORT_ORDER.length;
	}

	/** Sorts the stash by category, then by name. Stable, so equal items keep their order. */
	public static void sort(){
		java.util.Collections.sort( items, new java.util.Comparator<Item>() {
			@Override
			public int compare( Item a, Item b ) {
				int c = Integer.compare( category(a), category(b) );
				if (c != 0) return c;
				//within weapons and armor, higher tiers first
				int ta = tierOf(a), tb = tierOf(b);
				if (ta != tb) return Integer.compare( tb, ta );
				return a.name().compareTo( b.name() );
			}
		});
	}

	private static int tierOf( Item i ){
		if (i instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon)
			return ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon) i).tier;
		if (i instanceof Armor) return ((Armor) i).tier;
		return 0;
	}

	public static boolean isEmpty(){
		load();
		return items.isEmpty() && gold <= 0;
	}

	public static boolean stashable( Item item ){
		for (Class<?> c : NOT_STASHABLE){
			if (c.isInstance(item)) return false;
		}
		return true;
	}

	//withdrawing is only possible while preparing for a raid: on floor 1, before ever descending
	public static boolean canWithdraw(){
		//in the hideout withdrawn items go back into the stash when the hero finishes up
		if (Statistics.efpdInHideout) return true;
		//the Lab: only in the starting room, before touching anything
		if (com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Lab.isActive()) return com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Lab.canWithdraw();
		//open world: only in the base town
		if (OpenWorld.isActive()) return OpenWorld.onBase();
		//urban warfare: only on the starting plaza, before going anywhere
		if (com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.isActive()){
			return com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.canWithdraw();
		}
		return Dungeon.depth == 1 && Dungeon.branch == 0 && Statistics.deepestFloor <= 1;
	}

	/**
	 * Marks everything the hero starts with, so it is never moved into the stash.
	 * Called right after the starting kit is handed out.
	 */
	public static void markStartingKit( Hero hero ){
		for (Item i : hero.belongings){
			i.starterQuantity = i.quantity();
		}
		//the warrior's broken seal is stored inside his armor, not in the inventory
		if (hero.belongings.armor != null && hero.belongings.armor.checkSeal() != null){
			BrokenSeal seal = hero.belongings.armor.checkSeal();
			seal.starterQuantity = seal.quantity();
		}
	}

	/**
	 * Moves what the hero carries (equipped items, backpack, bag contents) plus their gold
	 * into the stash. Starting kit items and the bags themselves are left behind.
	 * Returns the number of item stacks stored.
	 */
	public static int depositAll( Hero hero ){
		load();

		Belongings b = hero.belongings;
		ArrayList<Item> carried = new ArrayList<>();
		//equipped items; secondWep is the champion's off-hand weapon
		Item[] equipped = {b.weapon, b.armor, b.artifact, b.misc, b.ring, b.secondWep};
		for (Item i : equipped){
			if (i != null) carried.add(i);
		}
		for (Item i : b.backpack.items){
			if (i instanceof Bag){
				//bags are not stored, but their contents are
				carried.addAll( ((Bag) i).items );
			} else {
				carried.add(i);
			}
		}

		int stored = 0;
		for (Item i : carried){
			if (prepareForStash(i)){
				items.add(i);
				stored++;
			}
		}

		gold += Dungeon.gold;
		Dungeon.gold = 0;
		energy += Dungeon.energy;
		Dungeon.energy = 0;

		save();
		return stored;
	}

	//strips starting kit parts from an item, returns false if nothing is left to store
	private static boolean prepareForStash( Item i ){
		if (i instanceof Bag || !stashable(i)) return false;

		int keep = i.quantity() - Math.min(i.starterQuantity, i.quantity());
		if (keep <= 0) return false;
		if (keep < i.quantity()) i.quantity(keep);
		i.starterQuantity = 0;

		//a starting broken seal stays behind even when affixed to found armor
		if (i instanceof Armor){
			BrokenSeal seal = ((Armor) i).checkSeal();
			if (seal != null && seal.starterQuantity > 0){
				((Armor) i).detachSeal();
			}
		}

		i.identify(false);
		return true;
	}

	//whether a single item can be put into the stash (open world base)
	public static boolean canDeposit( Item item ){
		if (item instanceof Bag || !stashable( item )) return false;
		return item.quantity() > Math.min( item.starterQuantity, item.quantity() );
	}

	/** Puts one carried item (or its non-starting-kit units) into the stash. */
	public static boolean depositOne( Hero hero, Item item ){
		if (!canDeposit( item )) return false;
		load();

		int keep = item.quantity() - Math.min( item.starterQuantity, item.quantity() );
		Item stored;
		if (keep < item.quantity()){
			//starting kit units stay with the hero
			stored = item.duplicate();
			stored.quantity( keep );
			stored.starterQuantity = 0;
			item.quantity( item.quantity() - keep );
			Item.updateQuickslot();
		} else {
			stored = item.detachAll( hero.belongings.backpack );
		}

		if (!prepareForStash( stored )) return false;
		items.add( stored );
		save();
		saveRun();
		return true;
	}

	public static void depositGold(){
		load();
		gold += Dungeon.gold;
		Dungeon.gold = 0;
		save();
		Item.updateQuickslot();
		saveRun();
	}

	/** Removes an item from the stash and gives it to the hero. */
	public static void withdraw( Hero hero, Item item ){
		if (!items.remove(item)) return;
		save();

		//the item was identified when it was stored; re-apply that knowledge in this run
		item.identify(false);
		if (!item.collect()){
			Dungeon.level.drop( item, hero.pos ).sprite.drop();
		}
		saveRun();
	}

	public static void withdrawGold(){
		if (gold <= 0) return;
		Dungeon.gold += gold;
		gold = 0;
		save();
		saveRun();
	}

	//save the run right away so a crash can't duplicate or delete withdrawn items
	private static void saveRun(){
		try {
			Dungeon.saveAll();
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
		}
	}

}
