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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * Wound bleeding caused by injuries. Unlike the original Bleeding debuff it does not
 * stop on its own: a bandage, first aid kit or potion of healing is needed.
 */
public class Hemorrhage extends Buff implements Hero.Doom {

	public static final int LIGHT = 1;
	public static final int HEAVY = 2;

	//HP lost per turn, as a fraction of max HP. Natural regen is 1 HP per 10 turns,
	//so light bleeding roughly cancels regeneration early on.
	public static final float[] LOSS_PER_TURN = { 0f, 1/200f, 1/60f };

	{
		//neutral, so cleansing effects don't stop it
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int level = LIGHT;
	private float partial = 0;

	public static void start( Char ch, int level ){
		Hemorrhage h = ch.buff( Hemorrhage.class );
		if (h == null){
			h = Buff.affect( ch, Hemorrhage.class );
			h.level = level;
			GLog.n( Messages.get(Hemorrhage.class, level == HEAVY ? "on_heavy" : "on_light") );
		} else if (h.level < HEAVY){
			//bleeding again on top of light bleeding makes it heavy
			h.level = HEAVY;
			GLog.n( Messages.get(Hemorrhage.class, "on_heavy") );
		}
		BuffIndicator.refreshHero();
	}

	public int level(){
		return level;
	}

	@Override
	public boolean act() {
		if (target.isAlive()){
			partial += target.HT * LOSS_PER_TURN[level];
			if (partial >= 1){
				int dmg = (int) partial;
				partial -= dmg;
				if (target.sprite != null && target.sprite.visible){
					Splash.at( target.sprite.center(), -(float)Math.PI/2, (float)Math.PI/6,
							target.sprite.blood(), Math.min( 10 * dmg, 20 ) );
				}
				//Hero.die() calls onDeath() since this is a Hero.Doom
				target.damage( dmg, this );
			}
			spend( TICK );
		} else {
			detach();
		}
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.BLEEDING;
	}

	@Override
	public void tintIcon(Image icon) {
		if (level == HEAVY) icon.hardlight(1f, 0.2f, 0.2f);
	}

	@Override
	public String name() {
		return Messages.get(this, level == HEAVY ? "name_heavy" : "name_light");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, level == HEAVY ? "desc_heavy" : "desc_light");
	}

	@Override
	public void onDeath() {
		Dungeon.fail( this );
		GLog.n( Messages.get(this, "ondeath") );
	}

	private static final String LEVEL   = "level";
	private static final String PARTIAL = "partial";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEVEL, level );
		bundle.put( PARTIAL, partial );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		level = Math.max( LIGHT, bundle.getInt( LEVEL ) );
		partial = bundle.getFloat( PARTIAL );
	}

}
