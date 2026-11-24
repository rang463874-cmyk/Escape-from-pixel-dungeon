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

package com.shatteredpixel.shatteredpixeldungeon.efpd.uniques;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.MetalShard;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * Unique weapons, one per boss, brewed at an alchemy pot from two of that boss's trophies.
 * They reuse original weapon art with a glow of their own, and each has a special effect.
 * Effect chances grow a little with upgrades.
 */
public abstract class UniqueWeapon extends MeleeWeapon {

	public static final int TROPHIES = 2;

	protected abstract int glowColor();

	@Override
	public ItemSprite.Glowing glowing() {
		ItemSprite.Glowing ench = super.glowing();
		return ench != null ? ench : new ItemSprite.Glowing( glowColor(), 2f );
	}

	protected float procChance( float base ){
		return base + 0.02f * Math.max( 0, buffedLvl() );
	}

	@Override
	public String desc() {
		return super.desc() + "\n\n" + Messages.get(this, "effect");
	}

	@Override
	public int value() {
		return 100 * tier + super.value();
	}

	//--- Goo: caustic slime blade (tier 2) ---------------------------------------------------------
	public static class OozeBlade extends UniqueWeapon {
		{
			image = ItemSpriteSheet.DIRK;
			hitSound = Assets.Sounds.HIT_STAB;
			tier = 2;
		}
		@Override protected int glowColor() { return 0x33AA22; }
		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (defender.isAlive() && Random.Float() < procChance( 0.33f )){
				Buff.affect( defender, Ooze.class ).set( Ooze.DURATION );
			}
			return super.proc( attacker, defender, damage );
		}
	}

	//--- Tengu: swift assassin's talon, can blind (tier 3) -----------------------------------------
	public static class TenguTalon extends UniqueWeapon {
		{
			image = ItemSpriteSheet.SAI;
			hitSound = Assets.Sounds.HIT_SLASH;
			hitSoundPitch = 1.2f;
			tier = 3;
			DLY = 0.67f; //1.5x speed
		}
		@Override protected int glowColor() { return 0xCC2222; }
		@Override
		public int max( int lvl ) {
			return Math.round( 3.5f*(tier+1) ) + lvl*Math.round( 0.75f*(tier+1) ); //14 base, down from 20
		}
		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (defender.isAlive() && Random.Float() < procChance( 0.15f )){
				Buff.prolong( defender, Blindness.class, 3f );
				defender.sprite.emitter().burst( Speck.factory( Speck.STEAM ), 3 );
			}
			return super.proc( attacker, defender, damage );
		}
	}

	//--- DM-300: piston hammer, can stun (tier 4) --------------------------------------------------
	public static class PistonHammer extends UniqueWeapon {
		{
			image = ItemSpriteSheet.WAR_HAMMER;
			hitSound = Assets.Sounds.HIT_CRUSH;
			hitSoundPitch = 0.9f;
			tier = 4;
		}
		@Override protected int glowColor() { return 0xDDAA33; }
		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (defender.isAlive() && !Char.hasProp( defender, Char.Property.BOSS )
					&& Random.Float() < procChance( 0.20f )){
				Buff.prolong( defender, Paralysis.class, 2f );
			}
			return super.proc( attacker, defender, damage );
		}
	}

	//--- Dwarf King: royal glaive, reach and life drain (tier 5) ----------------------------------
	public static class RoyalGlaive extends UniqueWeapon {
		{
			image = ItemSpriteSheet.GLAIVE;
			hitSound = Assets.Sounds.HIT_SLASH;
			hitSoundPitch = 0.8f;
			tier = 5;
			RCH = 2;
		}
		@Override protected int glowColor() { return 0x8833DD; }
		@Override
		public int max( int lvl ) {
			return Math.round( 6.67f*(tier+1) ) + lvl*(tier+1); //40 base, like the glaive
		}
		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (attacker instanceof Hero && damage > 0){
				int heal = Math.max( 1, Math.round( damage * 0.15f ) );
				heal = Math.min( heal, attacker.HT - attacker.HP );
				if (heal > 0){
					attacker.HP += heal;
					attacker.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(heal), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING );
				}
			}
			return super.proc( attacker, defender, damage );
		}
	}

	//--- recipes: two trophies -> unique weapon ----------------------------------------------------

	public static class BrewOozeBlade extends Recipe.SimpleRecipe {
		{
			inputs = new Class[]{ GooBlob.class }; inQuantity = new int[]{ TROPHIES };
			cost = 8; output = OozeBlade.class; outQuantity = 1;
		}
	}

	public static class BrewTenguTalon extends Recipe.SimpleRecipe {
		{
			inputs = new Class[]{ BossTrophy.TenguFeather.class }; inQuantity = new int[]{ TROPHIES };
			cost = 12; output = TenguTalon.class; outQuantity = 1;
		}
	}

	public static class BrewPistonHammer extends Recipe.SimpleRecipe {
		{
			inputs = new Class[]{ MetalShard.class }; inQuantity = new int[]{ TROPHIES };
			cost = 16; output = PistonHammer.class; outQuantity = 1;
		}
	}

	public static class BrewRoyalGlaive extends Recipe.SimpleRecipe {
		{
			inputs = new Class[]{ BossTrophy.RoyalSeal.class }; inQuantity = new int[]{ TROPHIES };
			cost = 20; output = RoyalGlaive.class; outQuantity = 1;
		}
	}

}
