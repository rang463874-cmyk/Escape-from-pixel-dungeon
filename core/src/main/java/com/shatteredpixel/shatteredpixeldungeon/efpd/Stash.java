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
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
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
 *    (climbs out of the dungeon entrance on floor 1).
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

	public static ArrayList<Item> items = new ArrayList<>();
	public static int gold = 0;

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
		try {
			Bundle bundle = FileUtils.bundleFromFile( STASH_FILE );
			for (Bundlable b : bundle.getCollection( ITEMS )){
				if (b instanceof Item) items.add( (Item)b );
			}
			gold = bundle.getInt( GOLD );
		} catch (IOException e) {
			//no stash yet, start empty
		}
	}

	public static void save(){
		Bundle bundle = new Bundle();
		bundle.put( ITEMS, items );
		bundle.put( GOLD, gold );
		try {
			FileUtils.bundleToFile( STASH_FILE, bundle );
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
		}
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
		return Dungeon.depth == 1 && Dungeon.branch == 0 && Statistics.deepestFloor <= 1;
	}

	/**
	 * Moves everything the hero carries (equipped items, backpack, bag contents)
	 * plus their gold into the stash. Returns the number of item stacks stored.
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
		//top level backpack items only, bags keep their own contents when bundled
		carried.addAll( b.backpack.items );

		int stored = 0;
		for (Item i : carried){
			if (!stashable(i)) continue;
			i.identify(false);
			items.add(i);
			stored++;
		}

		gold += Dungeon.gold;
		Dungeon.gold = 0;

		save();
		return stored;
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
