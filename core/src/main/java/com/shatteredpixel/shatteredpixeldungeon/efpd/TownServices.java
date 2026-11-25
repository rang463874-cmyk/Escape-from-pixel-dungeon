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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Tonic;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.io.IOException;

/**
 * Open world: more ways to spend gold in the base town. Gold carried and gold in the stash both
 * pay (carried first).
 *  - training grounds: buy proficiency XP for a permanent stat, a few sessions a day
 *  - rare goods: upgrade scrolls and strength potions whose price climbs with every purchase,
 *    plus a few fixed price specialities
 *  - the peddler's mystery crate: a random item, a few a day
 *
 * All tuning values live here.
 */
public class TownServices {

	//training: gold = TRAIN_GOLD * (use level + 1), gives TRAIN_XP proficiency XP
	public static final int   TRAIN_GOLD    = 120;
	public static final float TRAIN_XP      = 60f;
	public static final int   TRAIN_PER_DAY = 3;

	//rare goods: base price + step per earlier purchase
	public static final int UPGRADE_BASE  = 1000, UPGRADE_STEP  = 500;
	public static final int STRENGTH_BASE = 1500, STRENGTH_STEP = 750;
	public static final int MAPPING_PRICE = 200;
	public static final int INSPIRE_PRICE = 2500;

	//mystery crate
	public static final int CRATE_GOLD    = 300;
	public static final int CRATE_PER_DAY = 3;

	private static int upgradesBought = 0;
	private static int strengthBought = 0;
	private static int trainDay = -1, trainCount = 0;
	private static int crateDay = -1, crateCount = 0;

	public static void reset(){
		upgradesBought = strengthBought = 0;
		trainDay = crateDay = -1;
		trainCount = crateCount = 0;
	}

	private static int gold(){
		return Facilities.haveGold();
	}

	private static boolean pay( int amount ){
		return Facilities.spendGold( amount );
	}

	private static void give( Item item ){
		if (!item.collect()) Dungeon.level.drop( item, Dungeon.hero.pos ).sprite.drop();
		Item.updateQuickslot();
	}

	private static void save(){
		try {
			Dungeon.saveAll();
		} catch (IOException e) {
			com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon.reportException( e );
		}
	}

	private static String msg( String key, Object... args ){
		return Messages.get( TownServices.class, key, args );
	}

	//--- training grounds ------------------------------------------------------------------------

	private static int trainingsLeft(){
		if (trainDay != DayNight.day()) return TRAIN_PER_DAY;
		return Math.max( 0, TRAIN_PER_DAY - trainCount );
	}

	public static int trainCost( PermStats.Stat s ){
		return TRAIN_GOLD * (PermStats.useLevel( s ) + 1);
	}

	public static void showTraining(){
		WndFacilities.Builder b = new WndFacilities.Builder();
		final int left = trainingsLeft();
		for (final PermStats.Stat s : PermStats.Stat.values()){
			boolean maxed = PermStats.useLevel( s ) >= PermStats.MAX_USE;
			final int cost = trainCost( s );
			String label = maxed
					? msg( "train_maxed", PermStats.name( s ) )
					: msg( "train_entry", PermStats.name( s ), PermStats.useLevel( s ),
							Math.round( PermStats.xp( s ) ), PermStats.xpToNext( s ), cost );
			b.add( label, !maxed && left > 0 && gold() >= cost, new Runnable() {
				@Override
				public void run() {
					if (trainingsLeft() <= 0 || !pay( cost )) return;
					if (trainDay != DayNight.day()){
						trainDay = DayNight.day();
						trainCount = 0;
					}
					trainCount++;
					PermStats.gain( s, TRAIN_XP );
					Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 0.6f, 1.1f );
					GLog.p( msg( "trained", PermStats.name( s ), Math.round( TRAIN_XP ) ) );
					save();
					showTraining();
				}
			} );
		}
		b.add( msg( "back" ), true, new Runnable() { public void run() {} } );
		b.show( msg( "train_title" ), msg( "train_desc", Math.round( TRAIN_XP ), left, TRAIN_PER_DAY, gold() ) );
	}

	//--- rare goods ------------------------------------------------------------------------------

	public static int upgradePrice(){
		return UPGRADE_BASE + UPGRADE_STEP * upgradesBought;
	}

	public static int strengthPrice(){
		return STRENGTH_BASE + STRENGTH_STEP * strengthBought;
	}

	public static void showMarket(){
		WndFacilities.Builder b = new WndFacilities.Builder();
		final int g = gold();
		final int up = upgradePrice(), str = strengthPrice();
		b.add( msg( "entry", Messages.titleCase( new ScrollOfUpgrade().name() ), up ), g >= up, new Runnable() {
			public void run() {
				if (!pay( up )) return;
				upgradesBought++;
				buy( new ScrollOfUpgrade() );
			}
		} );
		b.add( msg( "entry", Messages.titleCase( new PotionOfStrength().name() ), str ), g >= str, new Runnable() {
			public void run() {
				if (!pay( str )) return;
				strengthBought++;
				buy( new PotionOfStrength() );
			}
		} );
		b.add( msg( "entry", Messages.titleCase( new ScrollOfMagicMapping().name() ), MAPPING_PRICE ), g >= MAPPING_PRICE, new Runnable() {
			public void run() {
				if (!pay( MAPPING_PRICE )) return;
				buy( new ScrollOfMagicMapping() );
			}
		} );
		b.add( msg( "entry", Messages.titleCase( new PotionOfDivineInspiration().name() ), INSPIRE_PRICE ), g >= INSPIRE_PRICE, new Runnable() {
			public void run() {
				if (!pay( INSPIRE_PRICE )) return;
				buy( new PotionOfDivineInspiration() );
			}
		} );
		b.add( msg( "back" ), true, new Runnable() { public void run() {} } );
		b.show( msg( "market_title" ), msg( "market_desc", g ) );
	}

	private static void buy( Item item ){
		item.identify();
		give( item );
		GLog.i( msg( "bought", Messages.titleCase( item.name() ) ) );
		save();
		showMarket();
	}

	//--- mystery crate ---------------------------------------------------------------------------

	private static int cratesLeft(){
		if (crateDay != DayNight.day()) return CRATE_PER_DAY;
		return Math.max( 0, CRATE_PER_DAY - crateCount );
	}

	private static int highestRegion(){
		int r = 0;
		for (int i = 0; i < OpenWorld.REGION_START.length; i++) if (OpenWorld.regionUnlocked( i )) r = i;
		return r;
	}

	private static Item rollCrate(){
		switch (Random.chances( new float[]{ 55, 25, 12, 5, 3 } )){
			case 0:  return Generator.random();
			case 1:  return Generator.random( Random.element( new Generator.Category[]{
					Generator.Category.WAND, Generator.Category.RING, Generator.Category.ARTIFACT } ) );
			case 2:  return Tonic.create( Random.element( PermStats.Stat.values() ), Math.min( 5, highestRegion() + 1 ) );
			case 3:  return new ScrollOfUpgrade();
			default: return new PotionOfStrength();
		}
	}

	public static void showCrate(){
		WndFacilities.Builder b = new WndFacilities.Builder();
		final int left = cratesLeft();
		b.add( msg( "crate_buy", CRATE_GOLD ), left > 0 && gold() >= CRATE_GOLD, new Runnable() {
			public void run() {
				if (cratesLeft() <= 0 || !pay( CRATE_GOLD )) return;
				if (crateDay != DayNight.day()){
					crateDay = DayNight.day();
					crateCount = 0;
				}
				crateCount++;
				Item item = rollCrate();
				if (item == null) item = Generator.random();
				give( item );
				Sample.INSTANCE.play( Assets.Sounds.UNLOCK );
				GLog.p( msg( "crate_got", Messages.titleCase( item.name() ) ) );
				save();
				showCrate();
			}
		} );
		b.add( msg( "back" ), true, new Runnable() { public void run() {} } );
		b.show( msg( "crate_title" ), msg( "crate_desc", left, CRATE_PER_DAY, gold() ) );
	}

	//--- saving ----------------------------------------------------------------------------------

	private static final String NODE = "efpd_town_services";

	public static void storeInBundle( Bundle bundle ){
		Bundle node = new Bundle();
		node.put( "upgrades", upgradesBought );
		node.put( "strength", strengthBought );
		node.put( "train_day", trainDay );
		node.put( "train_count", trainCount );
		node.put( "crate_day", crateDay );
		node.put( "crate_count", crateCount );
		bundle.put( NODE, node );
	}

	public static void restoreFromBundle( Bundle bundle ){
		reset();
		if (!bundle.contains( NODE )) return;
		Bundle n = bundle.getBundle( NODE );
		upgradesBought = n.getInt( "upgrades" );
		strengthBought = n.getInt( "strength" );
		trainDay = n.contains( "train_day" ) ? n.getInt( "train_day" ) : -1;
		trainCount = n.getInt( "train_count" );
		crateDay = n.contains( "crate_day" ) ? n.getInt( "crate_day" ) : -1;
		crateCount = n.getInt( "crate_count" );
	}

}
