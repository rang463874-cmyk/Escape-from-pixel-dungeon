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
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

/**
 * Open world thirst. Works like hunger but rises about twice as fast:
 * thirsty after THIRSTY turns, dehydrated after DEHYDRATED turns.
 * Dehydration slowly damages the hero and makes them easier to hit.
 * Only exists in open world mode.
 *
 * All tuning values live here.
 */
public class Thirst extends Buff implements Hero.Doom {

	public static final float THIRSTY    = 150f;
	public static final float DEHYDRATED = 250f;

	//one sip of water
	public static final float SIP = 150f;

	//fraction of max HP lost per turn while dehydrated (starving is 1/1000)
	public static final float DAMAGE_PER_TURN = 1/700f;
	public static final float DEHYDRATED_EVASION = 0.75f;

	private float level;
	private float partialDamage;

	{
		type = buffType.NEGATIVE;
	}

	/** Adds or removes the buff to match the current game mode. Called after a game is loaded. */
	public static void sync( Hero hero ){
		if (hero == null) return;
		if (OpenWorld.isActive()){
			if (hero.buff( Thirst.class ) == null) Buff.affect( hero, Thirst.class );
		} else {
			Buff.detach( hero, Thirst.class );
		}
	}

	public static boolean dehydrated( Char ch ){
		Thirst t = ch.buff( Thirst.class );
		return t != null && t.isDehydrated();
	}

	public static float evasionMultiplier( Char ch ){
		return dehydrated( ch ) ? DEHYDRATED_EVASION : 1f;
	}

	@Override
	public boolean act() {
		if (Dungeon.level.locked || SPDSettings.intro() || !target.isAlive()){
			spend( TICK );
			return true;
		}

		if (isDehydrated()){
			partialDamage += target.HT * DAMAGE_PER_TURN;
			if (partialDamage > 1){
				target.damage( (int)partialDamage, this );
				partialDamage -= (int)partialDamage;
			}
		} else {
			//open world endurance slows thirst as well as hunger
			float newLevel = level + PermStats.hungerMultiplier();
			if (newLevel >= DEHYDRATED){
				GLog.n( Messages.get(this, "on_dehydrated") );
				if (target instanceof Hero) ((Hero) target).interrupt();
				newLevel = DEHYDRATED;
			} else if (newLevel >= THIRSTY && level < THIRSTY){
				GLog.w( Messages.get(this, "on_thirsty") );
			}
			level = newLevel;
			BuffIndicator.refreshHero();
		}

		spend( TICK );
		return true;
	}

	public void quench( float amount ){
		level = Math.max( 0, level - amount );
		BuffIndicator.refreshHero();
	}

	public boolean isThirsty(){
		return level >= THIRSTY;
	}

	public boolean isDehydrated(){
		return level >= DEHYDRATED;
	}

	public int thirst(){
		return (int)Math.ceil( level );
	}

	@Override
	public int icon() {
		if (level < THIRSTY)     return BuffIndicator.NONE;
		if (level < DEHYDRATED)  return BuffIndicator.THIRST;
		return BuffIndicator.DEHYDRATED;
	}

	@Override
	public String name() {
		return Messages.get(this, isDehydrated() ? "dehydrated" : "thirsty");
	}

	@Override
	public String desc() {
		return Messages.get(this, isDehydrated() ? "desc_dehydrated" : "desc_thirsty");
	}

	@Override
	public void onDeath() {
		Dungeon.fail( this );
		GLog.n( Messages.get(this, "ondeath") );
	}

	private static final String LEVEL = "level";
	private static final String PARTIAL = "partial";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEVEL, level );
		bundle.put( PARTIAL, partialDamage );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		level = bundle.getFloat( LEVEL );
		partialDamage = bundle.getFloat( PARTIAL );
	}

}
