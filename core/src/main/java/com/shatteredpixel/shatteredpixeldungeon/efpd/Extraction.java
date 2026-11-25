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
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

import java.io.IOException;

/**
 * Handles extraction points: the floor 1 entrance, and the up stairs of every boss floor
 * once its boss is slain. Extracting ends the run and moves the hero's loot to the stash.
 */
public class Extraction {

	public static void showEntranceWindow( final Hero hero ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				String msg = Messages.get(Extraction.class, "entrance_desc");
				if (!Stash.canWithdraw()){
					msg += "\n\n" + Messages.get(Extraction.class, "entrance_locked");
				}
				GameScene.show( new WndOptions( Icons.get(Icons.STAIRS),
						Messages.get(Extraction.class, "entrance_title"),
						msg,
						Messages.get(Extraction.class, "extract"),
						Messages.get(Extraction.class, "open_stash"),
						Messages.get(Extraction.class, "cancel") ){
					@Override
					protected void onSelect(int index) {
						if (index == 0){
							confirmExtraction( hero );
						} else if (index == 1){
							GameScene.show( new WndStash() );
						}
					}
				});
			}
		});
	}

	//called whenever a boss dies (GameScene.bossSlain)
	public static void onBossSlain(){
		if (Dungeon.branch == 0 && Dungeon.bossLevel()){
			boolean first = (Statistics.efpdBossesCleared & (1 << Dungeon.depth)) == 0;
			Statistics.efpdBossesCleared |= 1 << Dungeon.depth;
			if (OpenWorld.isActive()){
				Statistics.efpdBossKills++;
				//open world: the next region opens on the region map
				int next = OpenWorld.regionOf(Dungeon.depth) + 1;
				if (first && next < OpenWorld.REGION_START.length){
					GLog.p( Messages.get(OpenWorld.class, "region_unlocked", Messages.get(OpenWorld.class, "region_" + next)) );
				} else if (!first){
					GLog.p( Messages.get(Regen.class, "boss_again", Regen.BOSS_REGEN_TURNS / DayNight.DAY_LENGTH) );
				}
			} else {
				GLog.p( Messages.get(Extraction.class, "boss_exit_open") );
				//the late bosses may carry a Lab keycard
				com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard.rollForBoss( Dungeon.depth );
			}
		}
	}

	public static boolean bossExitAvailable( Hero hero, LevelTransition transition ){
		return !OpenWorld.isActive()
				&& transition.type == LevelTransition.Type.REGULAR_ENTRANCE
				&& Dungeon.branch == 0
				&& Dungeon.bossLevel()
				&& (Statistics.efpdBossesCleared & (1 << Dungeon.depth)) != 0
				//heroes carrying the amulet keep the original ascent
				&& hero.belongings.getItem( Amulet.class ) == null;
	}

	public static void showBossExitWindow( final Hero hero, final LevelTransition transition ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndOptions( Icons.get(Icons.STAIRS),
						Messages.get(Extraction.class, "boss_exit_title"),
						Messages.get(Extraction.class, "boss_exit_desc"),
						Messages.get(Extraction.class, "extract"),
						Messages.get(Extraction.class, "ascend"),
						Messages.get(Extraction.class, "cancel") ){
					@Override
					protected void onSelect(int index) {
						if (index == 0){
							confirmExtraction( hero );
						} else if (index == 1){
							Dungeon.level.performTransition( hero, transition );
						}
					}
				});
			}
		});
	}

	private static void confirmExtraction( final Hero hero ){
		GameScene.show( new WndOptions( Icons.get(Icons.STAIRS),
				Messages.get(Extraction.class, "confirm_title"),
				Messages.get(Extraction.class, "confirm_desc"),
				Messages.get(Extraction.class, "confirm_yes"),
				Messages.get(Extraction.class, "cancel") ){
			@Override
			protected void onSelect(int index) {
				if (index == 0){
					extract( hero );
				}
			}
		});
	}

	/**
	 * Extracting moves the hero into the hideout: the run stays alive (so the original
	 * shop and alchemy systems work on the hero's inventory) until they finish up.
	 */
	public static void extract( final Hero hero ){
		Statistics.efpdInHideout = true;
		Statistics.ascended = true;

		//the hideout is safe: clear out enemies on the floor
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY){
				//removed silently: destroy() would count as a kill and grant exp
				Actor.remove( m );
				Dungeon.level.mobs.remove( m );
				if (m.sprite != null) m.sprite.killAndErase();
			}
		}

		//stash gold and energy are usable in the hideout, they go back when finishing
		Stash.load();
		Dungeon.gold += Stash.gold;
		Dungeon.energy += Stash.energy;
		Stash.gold = 0;
		Stash.energy = 0;
		Stash.save();
		//refresh the gold shown in the inventory
		Item.updateQuickslot();

		try {
			Dungeon.saveAll();
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
		}
		GLog.p( Messages.get(Extraction.class, "hideout_arrive") );
		//GameScene shows the hideout menu as soon as no other window is open
	}

	/** Ends the run: everything the hero carries goes into the stash. */
	public static void finish( final Hero hero ){
		//leave GameScene first, so the run can't be auto-saved again after it is deleted
		Game.switchScene(TitleScene.class, new Game.SceneChangeCallback() {
			@Override
			public void beforeCreate() {}

			@Override
			public void afterCreate() {
				Statistics.efpdInHideout = false;
				int depth = Statistics.deepestFloor;
				int stored = Stash.depositAll( hero );
				Dungeon.deleteGame( GamesInProgress.curSlot, true );

				Game.scene().addToFront( new WndMessage(
						Messages.get(Extraction.class, "success", depth, stored, Stash.gold, Stash.energy) ) );
			}
		});
	}

}
