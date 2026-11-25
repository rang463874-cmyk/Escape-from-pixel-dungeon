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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Open world survival mode: a persistent world with a base town and regions.
 * See docs/design/open-world-survival.md. This class holds the mode's rules;
 * the original game calls into it from a few hooks marked //EFPD.
 */
public class OpenWorld {

	//the base town is stored as its own level, on an otherwise unused branch
	public static final int BASE_DEPTH  = 1;
	public static final int BASE_BRANCH = 2;

	//first floor of each region: sewers, prison, caves, city, halls
	public static final int[] REGION_START = { 1, 6, 11, 16, 21 };

	//mode picked on the hero select screen, applied when the new game is created
	public static boolean selectedMode = false;

	public static boolean isActive(){
		return Statistics.efpdOpenWorld;
	}

	public static boolean onBase(){
		return isActive() && Dungeon.branch == BASE_BRANCH;
	}

	public static int regionOf( int depth ){
		return Math.max( 0, Math.min( REGION_START.length - 1, (depth - 1) / 5 ) );
	}

	//a region opens once the boss of the region before it has been slain
	public static boolean regionUnlocked( int region ){
		if (region <= 0) return true;
		int bossDepth = REGION_START[region] - 1;
		return (Statistics.efpdBossesCleared & (1 << bossDepth)) != 0;
	}

	public static boolean isRegionStart( int depth, int branch ){
		if (branch != 0) return false;
		for (int d : REGION_START) if (d == depth) return true;
		return false;
	}

	//--- travel --------------------------------------------------------------------------------

	public static void travelTo( int depth, int branch ){
		Level.beforeTransition();
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = depth;
		InterlevelScene.returnBranch = branch;
		InterlevelScene.returnPos = -1;
		Game.switchScene( InterlevelScene.class );
	}

	public static void travelToBase(){
		travelTo( BASE_DEPTH, BASE_BRANCH );
	}

	/**
	 * While carrying the Amulet of Yendor back up, the shortcuts to town are closed: the hero
	 * has to climb all the way out. Says so and returns true if that's the case.
	 */
	public static boolean amuletBlocksShortcut( Hero hero ){
		if (hero == null || hero.belongings.getItem( com.shatteredpixel.shatteredpixeldungeon.items.Amulet.class ) == null) return false;
		GLog.w( Messages.get(OpenWorld.class, "amulet_blocks") );
		return true;
	}

	/** Shown at the up stairs of a region's first floor. */
	public static void showReturnWindow( final Hero hero, final LevelTransition transition ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				//the sewers' first floor leads straight to the surface, other regions also lead up a floor
				final boolean canGoUp = transition != null && Dungeon.depth > 1;
				String[] options = canGoUp
						? new String[]{ Messages.get(OpenWorld.class, "go_base"), Messages.get(OpenWorld.class, "go_up"), Messages.get(OpenWorld.class, "cancel") }
						: new String[]{ Messages.get(OpenWorld.class, "go_base"), Messages.get(OpenWorld.class, "cancel") };
				GameScene.show( new WndOptions( Icons.get(Icons.STAIRS),
						Messages.get(OpenWorld.class, "return_title"),
						Messages.get(OpenWorld.class, "return_desc"),
						options ){
					@Override
					protected void onSelect( int index ) {
						if (index == 0){
							if (!amuletBlocksShortcut( hero )) travelToBase();
						} else if (index == 1 && canGoUp){
							Dungeon.level.performTransition( hero, transition );
						}
					}
				});
			}
		});
	}

	//shown once the hero is back in town after the ending
	private static boolean pendingSurvival = false;

	/**
	 * Bringing the amulet out of the dungeon: the ending plays, the amulet goes into the stash
	 * as a trophy, a survival is recorded, and the world goes on in the base town.
	 */
	public static void escapeWithAmulet( final Hero hero ){
		if (!Statistics.efpdSurvived){
			Statistics.efpdSurvived = true;
			Statistics.efpdSurvivedTurn = (int) Statistics.duration;
		}
		Statistics.ascended = true;
		com.shatteredpixel.shatteredpixeldungeon.items.Amulet amulet =
				hero.belongings.getItem( com.shatteredpixel.shatteredpixeldungeon.items.Amulet.class );
		if (amulet != null) Stash.depositOne( hero, amulet );
		com.shatteredpixel.shatteredpixeldungeon.Badges.validateHappyEnd();
		com.shatteredpixel.shatteredpixeldungeon.Badges.saveGlobal();
		pendingSurvival = true;
		saveRun();
		com.shatteredpixel.shatteredpixeldungeon.scenes.SurfaceScene.efpdContinueRun = true;
		Game.switchScene( com.shatteredpixel.shatteredpixeldungeon.scenes.SurfaceScene.class );
	}

	/** Called on arrival in town: the survival message after the ending. */
	public static void showPendingSurvival(){
		if (!pendingSurvival) return;
		pendingSurvival = false;
		GameScene.show( new com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage(
				Messages.get(OpenWorld.class, "survived", Statistics.efpdSurvivedTurn, Dungeon.hero.lvl)
				+ "\n\n" + Messages.get(OpenWorld.class, "amulet_stashed") ) );
	}

	/** Bringing the amulet out of the dungeon records a survival, then the game goes on. */
	public static void recordSurvival( Hero hero ){
		if (!Statistics.efpdSurvived){
			Statistics.efpdSurvived = true;
			Statistics.efpdSurvivedTurn = (int) Statistics.duration;
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage(
							Messages.get(OpenWorld.class, "survived", Statistics.efpdSurvivedTurn, Dungeon.hero.lvl) ) );
				}
			});
		}
		travelToBase();
	}

	//--- death ---------------------------------------------------------------------------------

	/**
	 * Instead of a game over, the hero drops everything they carry in a corpse bag
	 * and wakes up in the base town with half health.
	 */
	public static void onHeroDeath( final Hero hero, Object cause ){
		Statistics.efpdDeaths++;

		CorpseBag bag = new CorpseBag();
		bag.id = ++Statistics.efpdCorpseId;
		//the ghost ally keeps the hero's gold safe
		if (!Allies.has( Allies.Ally.GHOST )){
			bag.gold = Dungeon.gold;
			Dungeon.gold = 0;
		}

		Belongings b = hero.belongings;
		Item[] equipped = { b.weapon, b.armor, b.artifact, b.misc, b.ring, b.secondWep };
		for (Item i : equipped){
			if (i instanceof EquipableItem){
				//cursed items normally can't be removed, let death remove them anyway
				boolean cursed = i.cursed;
				i.cursed = false;
				((EquipableItem) i).doUnequip( hero, false, false );
				i.cursed = cursed;
				bag.items.add( i );
			}
		}
		b.weapon = b.secondWep = null;
		b.armor = null;
		b.artifact = null;
		b.misc = null;
		b.ring = null;

		bag.items.addAll( new ArrayList<>( b.backpack.items ) );
		b.backpack.items.clear();
		Dungeon.quickslot.reset();
		Item.updateQuickslot();

		if (!bag.items.isEmpty() || bag.gold > 0){
			Heap heap = Dungeon.level.drop( bag, hero.pos );
			heap.type = Heap.Type.REMAINS;
			Statistics.efpdCorpseDepth = Dungeon.depth;
			Statistics.efpdCorpseBranch = Dungeon.branch;
		} else {
			Statistics.efpdCorpseDepth = -1;
		}

		//revive: fresh buffs (injuries, bleeding, hunger cleared) and half health
		hero.live();
		hero.HP = Allies.has( Allies.Ally.GHOST ) ? hero.HT : Math.max( 1, hero.HT / 2 );
		Actor.fixTime();

		GLog.n( Messages.get(OpenWorld.class, "died") );
		if (Allies.has( Allies.Ally.GHOST )) GLog.p( Messages.get(Allies.class, "ghost_saved") );

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				travelToBase();
			}
		});
	}

	public static void removeStaleCorpses( Level level ){
		for (Heap heap : level.heaps.valueList()){
			for (Item i : new ArrayList<>( heap.items )){
				if (i instanceof CorpseBag && ((CorpseBag) i).id != Statistics.efpdCorpseId){
					heap.items.remove( i );
				}
			}
			if (heap.items.isEmpty()){
				level.heaps.remove( heap.pos );
			}
		}
	}

	public static String corpseLocation(){
		if (Statistics.efpdCorpseDepth < 0) return null;
		if (Statistics.efpdCorpseBranch == com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Lab.LAB_BRANCH){
			return Messages.get(WndWorldMap.class, "lab");
		}
		if (com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.inCity( Statistics.efpdCorpseBranch )){
			return Messages.get(WndWorldMap.class, "city_district",
					com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.zoneIndex( Statistics.efpdCorpseDepth ) + 1);
		}
		return Messages.get(OpenWorld.class, "region_" + regionOf(Statistics.efpdCorpseDepth))
				+ " " + Messages.get(OpenWorld.class, "floor", Statistics.efpdCorpseDepth);
	}

	public static void saveRun(){
		try {
			Dungeon.saveAll();
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
		}
	}

}
