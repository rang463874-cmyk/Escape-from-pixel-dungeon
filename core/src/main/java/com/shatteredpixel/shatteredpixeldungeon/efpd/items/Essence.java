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

package com.shatteredpixel.shatteredpixeldungeon.efpd.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.efpd.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * Regional essence, an open world crafting material. Enemies of a region sometimes drop the
 * essence of that region, bosses always drop several. Two essences and a seed brew a permanent
 * tonic at the alchemy pot; deeper regions brew higher grade tonics.
 */
public abstract class Essence extends Item {

	//chance for a regular enemy to drop an essence, and how many a boss drops
	public static final float DROP_CHANCE = 0.10f;
	public static final int   BOSS_DROP   = 4;

	{
		stackable = true;
	}

	//0 = sewers ... 4 = demon halls
	public abstract int region();

	//the grade of tonics brewed with this essence
	public int grade(){
		return region() + 1;
	}

	public static Essence forRegion( int region ){
		switch (region){
			case 0: default: return new Sewers();
			case 1: return new Prison();
			case 2: return new Caves();
			case 3: return new City();
			case 4: return new Halls();
		}
	}

	//called whenever an enemy dies
	public static void onMobDeath( Mob mob ){
		if (!OpenWorld.isActive() || mob.alignment != Char.Alignment.ENEMY) return;
		if (Dungeon.level == null || OpenWorld.onBase()) return;

		int amount = 0;
		if (Char.hasProp( mob, Char.Property.BOSS )){
			amount = BOSS_DROP;
		} else if (mob.EXP > 0 && Random.Float() < DROP_CHANCE){
			amount = 1;
		}
		if (amount <= 0) return;

		Essence e = forRegion( OpenWorld.regionOf( Dungeon.depth ) );
		e.quantity( amount );
		Dungeon.level.drop( e, mob.pos ).sprite.drop();
	}

	@Override
	public String desc() {
		return super.desc() + "\n\n" + Messages.get(Essence.class, "recipe", grade());
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 15 * grade() * quantity;
	}

	public static class Sewers extends Essence {
		{ image = ItemSpriteSheet.ESSENCE_SEWERS; }
		@Override public int region() { return 0; }
	}

	public static class Prison extends Essence {
		{ image = ItemSpriteSheet.ESSENCE_PRISON; }
		@Override public int region() { return 1; }
	}

	public static class Caves extends Essence {
		{ image = ItemSpriteSheet.ESSENCE_CAVES; }
		@Override public int region() { return 2; }
	}

	public static class City extends Essence {
		{ image = ItemSpriteSheet.ESSENCE_CITY; }
		@Override public int region() { return 3; }
	}

	public static class Halls extends Essence {
		{ image = ItemSpriteSheet.ESSENCE_HALLS; }
		@Override public int region() { return 4; }
	}

}
