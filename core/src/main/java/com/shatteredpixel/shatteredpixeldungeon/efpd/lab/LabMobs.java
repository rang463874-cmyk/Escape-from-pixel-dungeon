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

package com.shatteredpixel.shatteredpixeldungeon.efpd.lab;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.watabou.utils.Random;

/** The Lab's guards: elite mercenaries and security golems, often with a champion trait. */
public class LabMobs {

	public static final float MERC_SHARE     = 0.55f;
	public static final float CHAMPION_CHANCE = 0.3f;

	@SuppressWarnings("unchecked")
	private static final Class<? extends ChampionEnemy>[] CHAMPIONS = new Class[]{
			ChampionEnemy.Blazing.class, ChampionEnemy.Projecting.class, ChampionEnemy.AntiMagic.class,
			ChampionEnemy.Giant.class, ChampionEnemy.Blessed.class, ChampionEnemy.Growing.class };

	public static Mob create(){
		Mob m = Random.Float() < MERC_SHARE ? new LabMercenary() : new LabGolem();
		if (Random.Float() < CHAMPION_CHANCE){
			Buff.affect( m, Random.element( CHAMPIONS ) );
		}
		return m;
	}

}
