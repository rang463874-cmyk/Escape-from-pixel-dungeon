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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * Body part injuries. Enemies injure specific body parts (see {@link InjuryTable}),
 * each part has its own penalty, and injuries only heal with medical supplies
 * or a potion of healing.
 *
 * Severity per part: 0 = healthy, 1 = minor, 2 = severe.
 * The buff is attached while at least one part is injured.
 */
public class Injuries extends Buff {

	public enum Part {
		HEAD, TORSO, ARMS, LEGS, FEET
	}

	public static final int NONE   = 0;
	public static final int MINOR  = 1;
	public static final int SEVERE = 2;

	//--- tuning -------------------------------------------------------------------------------

	//chance for any damaging enemy hit to cause an injury
	public static final float BASE_CHANCE = 0.06f;
	//a single hit of at least this fraction of max HP always injures...
	public static final float BIG_HIT     = 0.25f;
	//...and a hit of at least this fraction causes a severe injury right away
	public static final float HUGE_HIT    = 0.40f;

	//chance that a new injury also starts bleeding, by resulting severity
	public static final float[] BLEED_CHANCE = { 0f, 0.30f, 0.70f };

	//penalties, indexed by severity
	public static final float[] HEAD_ACCURACY = { 1f, 0.90f, 0.75f };
	public static final float[] HEAD_VISION   = { 1f, 0.75f, 0.50f };
	public static final float[] TORSO_REGEN   = { 1f, 0.50f, 0f    };
	public static final float[] ARMS_ACCURACY = { 1f, 0.90f, 0.80f };
	public static final float[] ARMS_DAMAGE   = { 1f, 0.85f, 0.70f };
	public static final float[] LEGS_SPEED    = { 1f, 0.80f, 0.60f };
	public static final float[] FEET_EVASION  = { 1f, 0.80f, 0.60f };

	//------------------------------------------------------------------------------------------

	{
		//neutral, so cleansing effects don't remove it: only medical supplies heal injuries
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int[] severity = new int[Part.values().length];

	public int severity( Part part ){
		return severity[part.ordinal()];
	}

	public boolean injured(){
		for (int s : severity) if (s > 0) return true;
		return false;
	}

	@Override
	public boolean act() {
		//injuries don't expire on their own
		spend( TICK );
		return true;
	}

	//worsens a body part by the given amount, returns the new severity
	public int injure( Part part, int amount ){
		int i = part.ordinal();
		severity[i] = Math.min( SEVERE, severity[i] + amount );
		BuffIndicator.refreshHero();
		return severity[i];
	}

	public void heal( Part part ){
		severity[part.ordinal()] = NONE;
		BuffIndicator.refreshHero();
		if (!injured()) detach();
	}

	public void healAll(){
		for (int i = 0; i < severity.length; i++) severity[i] = NONE;
		detach();
	}

	//--- hooks called from the original game code ------------------------------------------------

	public static void onHeroDamaged( Hero hero, int damage, Object src ){
		if (!hero.isAlive() || !(src instanceof Mob)) return;
		Mob enemy = (Mob) src;
		if (enemy.alignment == Char.Alignment.ALLY) return;

		float ratio = damage / (float) hero.HT;
		int amount;
		if (ratio >= HUGE_HIT){
			amount = SEVERE;
		} else if (ratio >= BIG_HIT || Random.Float() < BASE_CHANCE){
			amount = MINOR;
		} else {
			return;
		}

		Part part = InjuryTable.partFor( enemy );
		Injuries injuries = Buff.affect( hero, Injuries.class );
		int before = injuries.severity(part);
		int after = injuries.injure( part, amount );

		if (after > before) {
			String partName = Messages.get(Injuries.class, "part_" + part.name().toLowerCase());
			if (after == SEVERE) {
				GLog.n( Messages.get(Injuries.class, "on_severe", partName) );
			} else {
				GLog.w( Messages.get(Injuries.class, "on_minor", partName) );
			}
		}

		if (Random.Float() < BLEED_CHANCE[after]){
			Hemorrhage.start( hero, after == SEVERE ? Hemorrhage.HEAVY : Hemorrhage.LIGHT );
		}
	}

	private static int sev( Char ch, Part part ){
		Injuries i = ch.buff( Injuries.class );
		return i == null ? NONE : i.severity(part);
	}

	public static float accuracyMultiplier( Char ch ){
		return HEAD_ACCURACY[sev(ch, Part.HEAD)] * ARMS_ACCURACY[sev(ch, Part.ARMS)];
	}

	public static float visionMultiplier( Char ch ){
		return HEAD_VISION[sev(ch, Part.HEAD)];
	}

	public static float regenMultiplier( Char ch ){
		return TORSO_REGEN[sev(ch, Part.TORSO)];
	}

	public static float damageMultiplier( Char ch ){
		return ARMS_DAMAGE[sev(ch, Part.ARMS)];
	}

	public static float speedMultiplier( Char ch ){
		return LEGS_SPEED[sev(ch, Part.LEGS)];
	}

	public static float evasionMultiplier( Char ch ){
		return FEET_EVASION[sev(ch, Part.FEET)];
	}

	//--- UI ------------------------------------------------------------------------------------

	private int worst(){
		int w = 0;
		for (int s : severity) w = Math.max(w, s);
		return w;
	}

	@Override
	public int icon() {
		return injured() ? BuffIndicator.INJURY : BuffIndicator.NONE;
	}

	@Override
	public void tintIcon(Image icon) {
		if (worst() == SEVERE) icon.hardlight(1f, 0.3f, 0.3f);
		else                   icon.hardlight(1f, 0.8f, 0.3f);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		StringBuilder sb = new StringBuilder( Messages.get(this, "desc") );
		for (Part p : Part.values()){
			int s = severity(p);
			if (s == NONE) continue;
			sb.append("\n\n_")
					.append( Messages.get(this, "part_" + p.name().toLowerCase()) )
					.append(" - ")
					.append( Messages.get(this, s == SEVERE ? "severe" : "minor") )
					.append("_\n")
					.append( effectText(p, s) );
		}
		sb.append("\n\n").append( Messages.get(this, "treat") );
		return sb.toString();
	}

	private static int pct( float multi ){
		return Math.round( 100 * (1f - multi) );
	}

	private String effectText( Part p, int s ){
		switch (p){
			case HEAD:  return Messages.get(this, "eff_head", pct(HEAD_ACCURACY[s]), pct(HEAD_VISION[s]));
			case TORSO: return Messages.get(this, "eff_torso", pct(TORSO_REGEN[s]));
			case ARMS:  return Messages.get(this, "eff_arms", pct(ARMS_ACCURACY[s]), pct(ARMS_DAMAGE[s]));
			case LEGS:  return Messages.get(this, "eff_legs", pct(LEGS_SPEED[s]));
			default:    return Messages.get(this, "eff_feet", pct(FEET_EVASION[s]));
		}
	}

	//--- saving --------------------------------------------------------------------------------

	private static final String SEVERITY = "severity";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( SEVERITY, severity );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		int[] saved = bundle.getIntArray( SEVERITY );
		if (saved != null){
			System.arraycopy( saved, 0, severity, 0, Math.min(saved.length, severity.length) );
		}
	}

}
