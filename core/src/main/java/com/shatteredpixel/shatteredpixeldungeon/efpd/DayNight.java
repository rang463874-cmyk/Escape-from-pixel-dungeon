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
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * Open world day / night cycle, driven by the global turn counter.
 * A day is DAY_LENGTH turns: day, then evening, then night.
 *
 * At night (open world only, outside the lit town):
 *  - the hero's view distance drops unless they carry a light
 *  - enemies respawn twice as fast
 *  - newly spawned enemies have a chance to be champions
 *
 * All tuning values live here.
 */
public class DayNight {

	public static final int DAY_LENGTH = 1200;
	public static final int EVENING    = 700;  //evening starts
	public static final int NIGHT      = 800;  //night starts, lasts until the next day

	public static final float NIGHT_VISION       = 0.6f;
	public static final float NIGHT_RESPAWN      = 2f;   //respawn speed multiplier
	public static final float NIGHT_CHAMPION     = 0.15f;//chance for a spawned enemy to be a champion

	public enum Phase { DAY, EVENING, NIGHT }

	public static boolean active(){
		return OpenWorld.isActive();
	}

	public static long turn(){
		return (long)(Statistics.duration + Actor.now());
	}

	public static int day(){
		return (int)(turn() / DAY_LENGTH) + 1;
	}

	public static int timeOfDay(){
		return (int)(turn() % DAY_LENGTH);
	}

	public static Phase phase(){
		int t = timeOfDay();
		if (t >= NIGHT)   return Phase.NIGHT;
		if (t >= EVENING) return Phase.EVENING;
		return Phase.DAY;
	}

	//night effects apply in the open world dungeon, the town is lit
	public static boolean isNight(){
		return active() && phase() == Phase.NIGHT && !OpenWorld.onBase();
	}

	public static float visionMultiplier( Char ch ){
		if (!isNight() || ch.buff( Light.class ) != null) return 1f;
		return NIGHT_VISION;
	}

	public static float respawnMultiplier(){
		return isNight() ? NIGHT_RESPAWN : 1f;
	}

	//e.g. "Day 3 - Night"
	public static String label(){
		return Messages.get(DayNight.class, "label", day(),
				Messages.get(DayNight.class, phase().name().toLowerCase()));
	}

	public static int color(){
		switch (phase()){
			case DAY: default: return 0xFFFF88;
			case EVENING:      return 0xFFAA55;
			case NIGHT:        return 0x88AAFF;
		}
	}

	//announces phase changes, called regularly from the UI
	private static Phase lastPhase = null;
	private static int lastDay = -1;

	public static void checkTransition(){
		if (!active() || Dungeon.hero == null){
			lastPhase = null;
			return;
		}
		Phase p = phase();
		int d = day();
		if (lastPhase != null && (p != lastPhase || d != lastDay)){
			switch (p){
				case DAY:     GLog.p( Messages.get(DayNight.class, "dawn", d) ); break;
				case EVENING: GLog.i( Messages.get(DayNight.class, "dusk") ); break;
				case NIGHT:   GLog.w( Messages.get(DayNight.class, "nightfall") ); break;
			}
		}
		lastPhase = p;
		lastDay = d;
	}

}
