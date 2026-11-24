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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

/**
 * Open world permanent stats. Each stat has two independent level tracks:
 *  - use levels (0..MAX_USE), earned by doing the matching action (proficiency XP)
 *  - tonic levels (0..MAX_TONIC), earned by drinking permanent tonics brewed at the alchemy pot
 *
 * Stats are kept through death and only grow / apply in open world mode.
 * The data lives in the run's Statistics bundle.
 *
 * All tuning values live here.
 */
public class PermStats {

	public enum Stat {
		STRENGTH,   //+1 strength per level (raises carry capacity too)
		VITALITY,   //+3 max HP per level
		AGILITY,    //+5% evasion per level
		SKILL,      //+5% accuracy per level
		ENDURANCE,  //-5% hunger per level
		RESILIENCE  //+10% natural regen and -5% injury chance per level
	}

	public static final int MAX_USE   = 5;
	public static final int MAX_TONIC = 5;

	//effect per total level
	public static final int   STR_PER_LVL   = 1;
	public static final int   HP_PER_LVL    = 3;
	public static final float EVA_PER_LVL   = 0.05f;
	public static final float ACC_PER_LVL   = 0.05f;
	public static final float HUNGER_PER_LVL= 0.05f;
	public static final float REGEN_PER_LVL = 0.10f;
	public static final float INJURY_PER_LVL= 0.05f;

	//proficiency XP for each action
	public static final float XP_MELEE_HIT   = 3f;   //strength
	public static final float XP_BURDEN_STEP = 0.4f; //strength, per step while carrying too much
	public static final float XP_DAMAGE_HT   = 40f;  //vitality, for taking damage equal to max HP
	public static final float XP_DODGE       = 3f;   //agility
	public static final float XP_ANY_HIT     = 2f;   //skill
	public static final float XP_STEP        = 0.15f;//endurance
	public static final float XP_TREAT       = 15f;  //resilience
	//enemies whose max level is below the hero's level barely teach anything
	public static final float WEAK_ENEMY_MULTI = 0.1f;

	private static final int N = Stat.values().length;
	private static int[]   useLvl   = new int[N];
	private static int[]   tonicLvl = new int[N];
	private static float[] xp       = new float[N];

	public static void reset(){
		useLvl   = new int[N];
		tonicLvl = new int[N];
		xp       = new float[N];
	}

	public static boolean active(){
		return OpenWorld.isActive();
	}

	//--- levels --------------------------------------------------------------------------------

	public static int useLevel( Stat s )   { return useLvl[s.ordinal()]; }
	public static int tonicLevel( Stat s ) { return tonicLvl[s.ordinal()]; }
	public static float xp( Stat s )       { return xp[s.ordinal()]; }

	public static int total( Stat s ){
		return useLvl[s.ordinal()] + tonicLvl[s.ordinal()];
	}

	//levels that actually apply right now (0 outside open world)
	public static int effective( Stat s ){
		return active() ? total(s) : 0;
	}

	public static int xpToNext( Stat s ){
		return (int)Math.round( 100 * Math.pow( 1.5, useLvl[s.ordinal()] ) );
	}

	//--- growth --------------------------------------------------------------------------------

	public static void gain( Stat s, float amount ){
		if (!active() || amount <= 0) return;
		int i = s.ordinal();
		if (useLvl[i] >= MAX_USE) return;
		xp[i] += amount;
		while (useLvl[i] < MAX_USE && xp[i] >= xpToNext(s)){
			xp[i] -= xpToNext(s);
			useLvl[i]++;
			onLevelUp( s, "level_up_use" );
		}
		if (useLvl[i] >= MAX_USE) xp[i] = 0;
	}

	//returns false if the tonic is too weak or the tonic track is full
	public static boolean drinkTonic( Stat s ){
		int i = s.ordinal();
		if (tonicLvl[i] >= MAX_TONIC) return false;
		tonicLvl[i]++;
		onLevelUp( s, "level_up_tonic" );
		return true;
	}

	private static void onLevelUp( Stat s, String key ){
		Hero hero = Dungeon.hero;
		GLog.p( Messages.get(PermStats.class, key, name(s), total(s)) );
		Sample.INSTANCE.play( Assets.Sounds.LEVELUP, 0.7f, 1.2f );
		if (hero != null){
			if (hero.sprite != null){
				hero.sprite.showStatus( CharSprite.POSITIVE, name(s) + " +1" );
			}
			if (s == Stat.VITALITY){
				hero.updateHT( true );
			}
		}
	}

	public static String name( Stat s ){
		return Messages.get(PermStats.class, "stat_" + s.name().toLowerCase());
	}

	public static String effectDesc( Stat s ){
		int t = total(s);
		switch (s){
			case STRENGTH:  return Messages.get(PermStats.class, "eff_strength", t*STR_PER_LVL);
			case VITALITY:  return Messages.get(PermStats.class, "eff_vitality", t*HP_PER_LVL);
			case AGILITY:   return Messages.get(PermStats.class, "eff_agility", Math.round(t*EVA_PER_LVL*100));
			case SKILL:     return Messages.get(PermStats.class, "eff_skill", Math.round(t*ACC_PER_LVL*100));
			case ENDURANCE: return Messages.get(PermStats.class, "eff_endurance", Math.round(t*HUNGER_PER_LVL*100));
			default:        return Messages.get(PermStats.class, "eff_resilience", Math.round(t*REGEN_PER_LVL*100), Math.round(t*INJURY_PER_LVL*100));
		}
	}

	//--- effects, called from the original game code -------------------------------------------

	public static int strBonus(){
		return effective(Stat.STRENGTH) * STR_PER_LVL;
	}

	public static int htBonus(){
		return effective(Stat.VITALITY) * HP_PER_LVL;
	}

	public static float evasionMultiplier(){
		return 1f + effective(Stat.AGILITY) * EVA_PER_LVL;
	}

	public static float accuracyMultiplier(){
		return 1f + effective(Stat.SKILL) * ACC_PER_LVL;
	}

	public static float hungerMultiplier(){
		return 1f - effective(Stat.ENDURANCE) * HUNGER_PER_LVL;
	}

	public static float regenMultiplier(){
		return 1f + effective(Stat.RESILIENCE) * REGEN_PER_LVL;
	}

	public static float injuryAvoidChance(){
		return effective(Stat.RESILIENCE) * INJURY_PER_LVL;
	}

	//--- growth hooks --------------------------------------------------------------------------

	private static float enemyFactor( Hero hero, Object enemy ){
		if (enemy instanceof Mob && ((Mob) enemy).maxLvl < hero.lvl) return WEAK_ENEMY_MULTI;
		return 1f;
	}

	//the hero landed an attack (melee, thrown or a gun)
	public static void onHeroHit( Hero hero, Char enemy ){
		if (!active() || enemy == null || enemy.alignment != Char.Alignment.ENEMY) return;
		float f = enemyFactor( hero, enemy );
		gain( Stat.SKILL, XP_ANY_HIT * f );
		if (!(hero.belongings.attackingWeapon() instanceof MissileWeapon)){
			gain( Stat.STRENGTH, XP_MELEE_HIT * f );
		}
	}

	public static void onHeroDodge( Hero hero, Char enemy ){
		if (!active() || enemy == null || enemy.alignment != Char.Alignment.ENEMY) return;
		gain( Stat.AGILITY, XP_DODGE * enemyFactor( hero, enemy ) );
	}

	public static void onHeroDamaged( Hero hero, int damage, Object src ){
		if (!active() || !hero.isAlive() || src instanceof Hunger) return;
		gain( Stat.VITALITY, XP_DAMAGE_HT * damage / (float)hero.HT * enemyFactor( hero, src ) );
	}

	public static void onHeroStep( Hero hero ){
		if (!active() || OpenWorld.onBase()) return;
		gain( Stat.ENDURANCE, XP_STEP );
		if (Weight.level( hero ) > 0){
			gain( Stat.STRENGTH, XP_BURDEN_STEP );
		}
	}

	public static void onTreated(){
		gain( Stat.RESILIENCE, XP_TREAT );
	}

	//--- saving --------------------------------------------------------------------------------

	private static final String NODE  = "efpd_perm_stats";
	private static final String USE   = "use";
	private static final String TONIC = "tonic";
	private static final String XP    = "xp";

	public static void storeInBundle( Bundle bundle ){
		Bundle node = new Bundle();
		node.put( USE, useLvl );
		node.put( TONIC, tonicLvl );
		node.put( XP, xp );
		bundle.put( NODE, node );
	}

	public static void restoreFromBundle( Bundle bundle ){
		reset();
		if (!bundle.contains( NODE )) return;
		Bundle node = bundle.getBundle( NODE );
		copy( node.getIntArray( USE ), useLvl );
		copy( node.getIntArray( TONIC ), tonicLvl );
		float[] x = node.getFloatArray( XP );
		if (x != null) System.arraycopy( x, 0, xp, 0, Math.min(x.length, N) );
	}

	private static void copy( int[] from, int[] to ){
		if (from != null) System.arraycopy( from, 0, to, 0, Math.min(from.length, N) );
	}

}
