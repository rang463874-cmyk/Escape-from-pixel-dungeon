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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Material;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.SupplyPack;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.io.IOException;

/**
 * Open world base facilities. Each facility is built and upgraded (levels 1-3) with gold and
 * building materials, then used from the base keeper's menu. The data lives in the run's
 * Statistics bundle.
 *
 * All tuning values live here.
 */
public class Facilities {

	public enum Facility { BED, KITCHEN, INFIRMARY, FARM, FORGE }

	public static final int MAX_LEVEL = 3;

	//cost of building level 1, 2, 3: gold, cloth, wood, stone, metal
	private static final int[][][] COSTS = {
			/*BED*/       { {100, 3, 5, 0, 0}, {300, 6, 8, 0, 0},  {800, 10, 10, 4, 0} },
			/*KITCHEN*/   { {50,  0, 4, 0, 0}, {200, 0, 6, 4, 0},  {600, 0, 0, 6, 4} },
			/*INFIRMARY*/ { {200, 6, 4, 0, 0}, {500, 10, 0, 0, 2}, {1000, 14, 0, 0, 6} },
			/*FARM*/      { {100, 0, 6, 0, 0}, {300, 0, 10, 4, 0}, {700, 0, 14, 8, 0} },
			/*FORGE*/     { {300, 0, 4, 6, 0}, {700, 0, 0, 10, 4}, {1500, 0, 0, 0, 10} },
	};
	public static final Class<?>[] MATERIALS = {
			Material.Cloth.class, Material.Wood.class, Material.StoneBlock.class, Material.RefinedMetal.class
	};

	//bed: fraction of max HP restored by a night's sleep, per level
	public static final float[] SLEEP_HEAL = { 0.5f, 0.75f, 1f };
	//hunger and thirst gained per turn slept
	public static final float SLEEP_UPKEEP = 0.3f;

	//infirmary: cost per point of injury severity and for bleeding, multiplied per level
	public static final int     TREAT_PER_SEVERITY = 40;
	public static final int     TREAT_BLEEDING     = 30;
	public static final float[] TREAT_MULTI        = { 1f, 0.75f, 0.5f };

	//farm: plots per level and turns until a crop is ready
	public static final int[] FARM_PLOTS = { 2, 4, 6 };
	public static final int   GROW_TIME  = 900;

	//forge
	public static final int UNCURSE_COST  = 150;  //level 1
	public static final int IDENTIFY_COST = 80;   //level 2
	public static final int REINFORCE_GOLD = 400; //level 3, times the item's level + 1
	public static final int REINFORCE_METAL = 2;
	public static final int REINFORCE_MAX  = 3;

	private static int[] levels = new int[Facility.values().length];
	private static String[] plotSeed = new String[6];
	private static long[] plotTime = new long[6];

	public static void reset(){
		levels = new int[Facility.values().length];
		plotSeed = new String[6];
		plotTime = new long[6];
	}

	public static int level( Facility f ){
		return levels[f.ordinal()];
	}

	public static boolean built( Facility f ){
		return OpenWorld.isActive() && level(f) > 0;
	}

	public static String name( Facility f ){
		return Messages.get(Facilities.class, "name_" + f.name().toLowerCase());
	}

	//--- building --------------------------------------------------------------------------------

	public static int[] nextCost( Facility f ){
		int lvl = level(f);
		return lvl >= MAX_LEVEL ? null : COSTS[f.ordinal()][lvl];
	}

	//materials and gold count both what the hero carries and what is in the stash
	public static int haveGold(){
		Stash.load();
		return Dungeon.gold + Stash.gold;
	}

	public static int haveMaterial( int index ){
		int count = 0;
		for (Item i : Dungeon.hero.belongings){
			if (i.getClass() == MATERIALS[index]) count += i.quantity();
		}
		Stash.load();
		for (Item i : Stash.items){
			if (i.getClass() == MATERIALS[index]) count += i.quantity();
		}
		return count;
	}

	public static boolean canAfford( int[] cost ){
		if (cost == null || haveGold() < cost[0]) return false;
		for (int m = 0; m < MATERIALS.length; m++){
			if (haveMaterial(m) < cost[m+1]) return false;
		}
		return true;
	}

	public static String costText( int[] cost ){
		StringBuilder sb = new StringBuilder( Messages.get(Facilities.class, "cost_gold", cost[0]) );
		for (int m = 0; m < MATERIALS.length; m++){
			if (cost[m+1] > 0){
				sb.append(", ").append( Messages.get(Facilities.class, "cost_mat",
						Messages.titleCase(((Item) Reflection.newInstance(MATERIALS[m])).name()), cost[m+1]) );
			}
		}
		return sb.toString();
	}

	public static String haveText(){
		StringBuilder sb = new StringBuilder( Messages.get(Facilities.class, "have_gold", haveGold()) );
		for (int m = 0; m < MATERIALS.length; m++){
			sb.append(", ").append( Messages.get(Facilities.class, "cost_mat",
					Messages.titleCase(((Item) Reflection.newInstance(MATERIALS[m])).name()), haveMaterial(m)) );
		}
		return sb.toString();
	}

	public static boolean build( Facility f ){
		int[] cost = nextCost( f );
		if (!canAfford( cost )) return false;
		spendGold( cost[0] );
		for (int m = 0; m < MATERIALS.length; m++){
			spendMaterial( m, cost[m+1] );
		}
		levels[f.ordinal()]++;
		saveRun();
		return true;
	}

	//carried gold first, then the stash
	public static boolean spendGold( int amount ){
		if (haveGold() < amount) return false;
		int fromHand = Math.min( Dungeon.gold, amount );
		Dungeon.gold -= fromHand;
		amount -= fromHand;
		if (amount > 0){
			Stash.load();
			Stash.gold -= amount;
			Stash.save();
		}
		Item.updateQuickslot();
		return true;
	}

	//carried materials first, then the stash
	private static void spendMaterial( int index, int amount ){
		Hero hero = Dungeon.hero;
		for (Item i : hero.belongings.backpack.items.toArray(new Item[0])){
			if (amount <= 0) break;
			if (i.getClass() == MATERIALS[index]){
				int take = Math.min( amount, i.quantity() );
				if (take >= i.quantity()) i.detachAll( hero.belongings.backpack );
				else i.quantity( i.quantity() - take );
				amount -= take;
			}
		}
		if (amount > 0){
			Stash.load();
			for (Item i : Stash.items.toArray(new Item[0])){
				if (amount <= 0) break;
				if (i.getClass() == MATERIALS[index]){
					int take = Math.min( amount, i.quantity() );
					if (take >= i.quantity()) Stash.items.remove( i );
					else i.quantity( i.quantity() - take );
					amount -= take;
				}
			}
			Stash.save();
		}
		Item.updateQuickslot();
	}

	//--- bed -------------------------------------------------------------------------------------

	public static int turnsUntilDawn(){
		return DayNight.DAY_LENGTH - DayNight.timeOfDay();
	}

	/** Sleeps until the next morning. Returns the number of turns slept. */
	public static int sleep( Hero hero ){
		int turns = turnsUntilDawn();
		Statistics.duration += turns;

		float upkeep = turns * SLEEP_UPKEEP;
		Hunger hunger = hero.buff( Hunger.class );
		if (hunger != null) hunger.affectHunger( -Math.min( upkeep, Math.max(0, Hunger.STARVING - 1 - hunger.hunger()) ) );
		Thirst thirst = hero.buff( Thirst.class );
		if (thirst != null) thirst.quench( -Math.min( upkeep, Math.max(0, Thirst.DEHYDRATED - 1 - thirst.thirst()) ) );

		//timed effects don't tick while the world clock jumps, a night's sleep settles the stomach
		Buff.detach( hero, Nausea.class );

		int heal = Math.round( hero.HT * SLEEP_HEAL[level(Facility.BED) - 1] );
		hero.HP = Math.min( hero.HT, hero.HP + heal );
		if (level(Facility.BED) >= MAX_LEVEL){
			Buff.detach( hero, Hemorrhage.class );
		}
		saveRun();
		return turns;
	}

	//--- infirmary -------------------------------------------------------------------------------

	public static int treatmentCost( Hero hero ){
		int points = 0;
		Injuries inj = hero.buff( Injuries.class );
		if (inj != null){
			for (Injuries.Part p : Injuries.Part.values()) points += inj.severity( p );
		}
		int cost = points * TREAT_PER_SEVERITY;
		if (hero.buff( Hemorrhage.class ) != null) cost += TREAT_BLEEDING;
		return Math.round( cost * TREAT_MULTI[Math.max(0, level(Facility.INFIRMARY) - 1)] );
	}

	public static boolean treat( Hero hero ){
		int cost = treatmentCost( hero );
		if (cost <= 0 || !spendGold( cost )) return false;
		Injuries inj = hero.buff( Injuries.class );
		if (inj != null) inj.healAll();
		Buff.detach( hero, Hemorrhage.class );
		PermStats.onTreated();
		saveRun();
		return true;
	}

	//--- farm ------------------------------------------------------------------------------------

	public static int plots(){
		int lvl = level( Facility.FARM );
		return lvl <= 0 ? 0 : FARM_PLOTS[lvl - 1];
	}

	public static Class<?> plotSeed( int plot ){
		return plotSeed[plot] == null ? null : Reflection.forName( plotSeed[plot] );
	}

	public static int turnsUntilRipe( int plot ){
		return (int)Math.max( 0, plotTime[plot] + GROW_TIME - DayNight.turn() );
	}

	public static void plant( int plot, Plant.Seed seed ){
		seed.detach( Dungeon.hero.belongings.backpack );
		plotSeed[plot] = seed.getClass().getName();
		plotTime[plot] = DayNight.turn();
		saveRun();
	}

	/** Harvests a ripe plot: two seeds of the same kind (sometimes three), blandfruit for its seed. */
	//how many units the last harvest gave (the crop item may merge into an existing stack)
	public static int lastHarvest = 0;

	public static Item harvest( int plot ){
		Class<?> cls = plotSeed( plot );
		if (cls == null || turnsUntilRipe( plot ) > 0) return null;
		Item crop;
		if (cls == BlandfruitBush.Seed.class){
			crop = new Blandfruit().quantity( 2 );
		} else {
			crop = ((Item) Reflection.newInstance( cls )).quantity( Random.Float() < 0.25f ? 3 : 2 );
		}
		plotSeed[plot] = null;
		lastHarvest = crop.quantity();
		if (!crop.collect()) Dungeon.level.drop( crop, Dungeon.hero.pos ).sprite.drop();
		saveRun();
		return crop;
	}

	//--- supply pack -------------------------------------------------------------------------------

	public static boolean upgradePack( SupplyPack pack ){
		int[] cost = pack.nextCost();
		if (!canAfford( cost )) return false;
		spendGold( cost[0] );
		for (int m = 0; m < MATERIALS.length; m++){
			spendMaterial( m, cost[m+1] );
		}
		pack.upgradePack();
		Item.updateQuickslot();
		saveRun();
		return true;
	}

	//--- forge -------------------------------------------------------------------------------------

	public static boolean reinforce( Item item, int gold ){
		if (haveGold() < gold || haveMaterial( 3 ) < REINFORCE_METAL) return false;
		if (!item.isUpgradable() || item.level() >= REINFORCE_MAX) return false;
		spendGold( gold );
		spendMaterial( 3, REINFORCE_METAL );
		item.upgrade();
		Item.updateQuickslot();
		saveRun();
		return true;
	}

	//--- saving ----------------------------------------------------------------------------------

	private static void saveRun(){
		try {
			Dungeon.saveAll();
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException( e );
		}
	}

	private static final String NODE   = "efpd_facilities";
	private static final String LEVELS = "levels";
	private static final String SEEDS  = "seeds";
	private static final String TIMES  = "times";

	public static void storeInBundle( Bundle bundle ){
		Bundle node = new Bundle();
		node.put( LEVELS, levels );
		String[] seeds = new String[plotSeed.length];
		for (int i = 0; i < seeds.length; i++) seeds[i] = plotSeed[i] == null ? "" : plotSeed[i];
		node.put( SEEDS, seeds );
		node.put( TIMES, plotTime );
		bundle.put( NODE, node );
	}

	public static void restoreFromBundle( Bundle bundle ){
		reset();
		if (!bundle.contains( NODE )) return;
		Bundle node = bundle.getBundle( NODE );
		int[] l = node.getIntArray( LEVELS );
		if (l != null) System.arraycopy( l, 0, levels, 0, Math.min(l.length, levels.length) );
		String[] s = node.getStringArray( SEEDS );
		if (s != null) for (int i = 0; i < Math.min(s.length, plotSeed.length); i++){
			plotSeed[i] = s[i] == null || s[i].isEmpty() ? null : s[i];
		}
		long[] t = node.getLongArray( TIMES );
		if (t != null) System.arraycopy( t, 0, plotTime, 0, Math.min(t.length, plotTime.length) );
	}

}
