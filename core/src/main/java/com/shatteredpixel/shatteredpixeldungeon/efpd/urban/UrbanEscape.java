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

package com.shatteredpixel.shatteredpixeldungeon.efpd.urban;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.efpd.Extraction;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

/** The city gates: walking through one ends the run as a successful extraction. */
public class UrbanEscape {

	public static void showGateWindow( final Hero hero ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndOptions( Icons.get(Icons.STAIRS),
						Messages.get(UrbanEscape.class, "title"),
						Messages.get(UrbanEscape.class, "desc"),
						Messages.get(UrbanEscape.class, "escape"),
						Messages.get(UrbanEscape.class, "stay") ){
					@Override
					protected void onSelect( int index ) {
						if (index == 0) Extraction.extract( hero );
					}
				});
			}
		});
	}

}
