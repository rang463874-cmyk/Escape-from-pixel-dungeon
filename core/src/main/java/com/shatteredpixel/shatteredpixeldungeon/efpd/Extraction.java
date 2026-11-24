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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
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
			Statistics.efpdBossesCleared |= 1 << Dungeon.depth;
			GLog.p( Messages.get(Extraction.class, "boss_exit_open") );
		}
	}

	public static boolean bossExitAvailable( Hero hero, LevelTransition transition ){
		return transition.type == LevelTransition.Type.REGULAR_ENTRANCE
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

	public static void extract( final Hero hero ){
		//leave GameScene first, so the run can't be auto-saved again after it is deleted
		Statistics.ascended = true;
		Game.switchScene(TitleScene.class, new Game.SceneChangeCallback() {
			@Override
			public void beforeCreate() {}

			@Override
			public void afterCreate() {
				int depth = Statistics.deepestFloor;
				int goldBefore = Dungeon.gold;
				int stored = Stash.depositAll( hero );
				Dungeon.deleteGame( GamesInProgress.curSlot, true );

				Game.scene().addToFront( new WndMessage(
						Messages.get(Extraction.class, "success", depth, stored, goldBefore) ) );
			}
		});
	}

}
