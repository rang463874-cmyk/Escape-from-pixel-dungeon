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

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

/**
 * The region map: pick a region to travel to its first floor.
 * Locked regions open when the boss before them is slain.
 */
public class WndWorldMap extends WndOptions {

	public static void show(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndWorldMap() );
			}
		});
	}

	private static String[] labels(){
		int n = OpenWorld.REGION_START.length;
		String[] labels = new String[n + 2];
		for (int r = 0; r < n; r++){
			String name = Messages.get(OpenWorld.class, "region_" + r);
			labels[r] = OpenWorld.regionUnlocked(r)
					? Messages.get(WndWorldMap.class, "region", name, OpenWorld.REGION_START[r], OpenWorld.REGION_START[r] + 4)
					: Messages.get(WndWorldMap.class, "locked", name);
		}
		labels[n] = Messages.get(WndWorldMap.class, "city");
		labels[n+1] = Messages.get(WndWorldMap.class, "stay");
		return labels;
	}

	private static String desc(){
		String desc = Messages.get(WndWorldMap.class, "desc");
		String corpse = OpenWorld.corpseLocation();
		desc += "\n\n" + (corpse == null
				? Messages.get(WndWorldMap.class, "no_corpse")
				: Messages.get(WndWorldMap.class, "corpse", corpse));
		return desc;
	}

	public WndWorldMap(){
		super( Icons.get(Icons.STAIRS), Messages.get(WndWorldMap.class, "title"), desc(), labels() );
	}

	@Override
	protected boolean enabled( int index ) {
		return index >= OpenWorld.REGION_START.length || OpenWorld.regionUnlocked( index );
	}

	@Override
	protected void onSelect( int index ) {
		if (index < OpenWorld.REGION_START.length && OpenWorld.regionUnlocked( index )){
			OpenWorld.travelTo( OpenWorld.REGION_START[index], 0 );
		} else if (index == OpenWorld.REGION_START.length){
			showCity();
		}
	}

	/** The ruined city's districts: each opens together with the dungeon region of its rank. */
	private static void showCity(){
		final int n = com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.ZONE_DEPTHS.length;
		String[] labels = new String[n + 1];
		for (int z = 0; z < n; z++){
			labels[z] = OpenWorld.regionUnlocked( z )
					? Messages.get(WndWorldMap.class, "city_district", z + 1)
					: Messages.get(WndWorldMap.class, "locked", Messages.get(WndWorldMap.class, "city_district", z + 1));
		}
		labels[n] = Messages.get(WndWorldMap.class, "stay");
		GameScene.show( new WndOptions( Icons.get(Icons.STAIRS), Messages.get(WndWorldMap.class, "city_title"),
				Messages.get(WndWorldMap.class, "city_desc"), labels ){
			@Override
			protected boolean enabled( int index ) {
				return index >= n || OpenWorld.regionUnlocked( index );
			}
			@Override
			protected void onSelect( int index ) {
				if (index < n && OpenWorld.regionUnlocked( index )){
					com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.arrivalFrom = -1;
					OpenWorld.travelTo( com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.ZONE_DEPTHS[index],
							com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.URBAN_BRANCH );
				}
			}
		});
	}

}
