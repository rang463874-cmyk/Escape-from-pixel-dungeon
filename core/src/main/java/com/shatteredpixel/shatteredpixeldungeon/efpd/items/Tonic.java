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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.efpd.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.efpd.PermStats;
import com.shatteredpixel.shatteredpixeldungeon.efpd.PermStats.Stat;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.plants.Fadeleaf;
import com.shatteredpixel.shatteredpixeldungeon.plants.Icecap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Mageroyal;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * Permanent tonic: raises the tonic level of one open world stat by one.
 * A tonic of grade G can raise the tonic level up to G, so the higher tonic levels need tonics
 * brewed with essences from deeper regions. Only drinkable in open world mode.
 */
public abstract class Tonic extends Item {

	public static final String AC_DRINK = "DRINK";

	protected int grade = 1;

	{
		stackable = true;
		defaultAction = AC_DRINK;
	}

	public abstract Stat stat();

	public int grade(){
		return grade;
	}

	public Tonic grade( int grade ){
		this.grade = Math.max( 1, Math.min( PermStats.MAX_TONIC, grade ) );
		return this;
	}

	//the seed brewed into each tonic
	public static final Class<?>[] SEEDS = {
			Earthroot.Seed.class,    //strength
			Sungrass.Seed.class,     //vitality
			Swiftthistle.Seed.class, //agility
			Fadeleaf.Seed.class,     //skill
			Icecap.Seed.class,       //endurance
			Mageroyal.Seed.class     //resilience
	};

	public static final Class<?>[] TONICS = {
			Strength.class, Vitality.class, Agility.class, Skill.class, Endurance.class, Resilience.class
	};

	public static Tonic create( Stat stat, int grade ){
		return ((Tonic) Reflection.newInstance( TONICS[stat.ordinal()] )).grade( grade );
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_DRINK );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );

		if (action.equals( AC_DRINK )) {
			if (!OpenWorld.isActive()){
				GLog.w( Messages.get(Tonic.class, "only_open_world") );
				return;
			}
			int lvl = PermStats.tonicLevel( stat() );
			if (lvl >= PermStats.MAX_TONIC){
				GLog.w( Messages.get(Tonic.class, "maxed", PermStats.name(stat())) );
				return;
			}
			if (grade < lvl + 1){
				GLog.w( Messages.get(Tonic.class, "too_weak", lvl + 1) );
				return;
			}

			detach( hero.belongings.backpack );
			PermStats.drinkTonic( stat() );

			hero.sprite.operate( hero.pos );
			hero.busy();
			Sample.INSTANCE.play( Assets.Sounds.DRINK );
			hero.spend( 1f );
			updateQuickslot();
		}
	}

	@Override
	public boolean isSimilar( Item item ) {
		return super.isSimilar( item ) && ((Tonic) item).grade == grade;
	}

	@Override
	public String name() {
		return Messages.get(Tonic.class, "graded", super.name(), grade);
	}

	@Override
	public String desc() {
		String desc = super.desc() + "\n\n" + Messages.get(Tonic.class, "rules", grade, grade);
		if (OpenWorld.isActive()){
			desc += "\n\n" + Messages.get(Tonic.class, "current",
					PermStats.name(stat()), PermStats.tonicLevel(stat()), PermStats.MAX_TONIC);
		} else {
			desc += "\n\n" + Messages.get(Tonic.class, "only_open_world");
		}
		return desc;
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
		return 60 * grade * quantity;
	}

	private static final String GRADE = "grade";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( GRADE, grade );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		grade( bundle.getInt( GRADE ) );
	}

	public static class Strength extends Tonic {
		{ image = ItemSpriteSheet.TONIC_STRENGTH; }
		@Override public Stat stat() { return Stat.STRENGTH; }
	}

	public static class Vitality extends Tonic {
		{ image = ItemSpriteSheet.TONIC_VITALITY; }
		@Override public Stat stat() { return Stat.VITALITY; }
	}

	public static class Agility extends Tonic {
		{ image = ItemSpriteSheet.TONIC_AGILITY; }
		@Override public Stat stat() { return Stat.AGILITY; }
	}

	public static class Skill extends Tonic {
		{ image = ItemSpriteSheet.TONIC_SKILL; }
		@Override public Stat stat() { return Stat.SKILL; }
	}

	public static class Endurance extends Tonic {
		{ image = ItemSpriteSheet.TONIC_ENDURANCE; }
		@Override public Stat stat() { return Stat.ENDURANCE; }
	}

	public static class Resilience extends Tonic {
		{ image = ItemSpriteSheet.TONIC_RESILIENCE; }
		@Override public Stat stat() { return Stat.RESILIENCE; }
	}

	/**
	 * Alchemy: 1 matching seed + 2 essences of one region = 1 tonic of that region's grade.
	 * The alchemy pot loads one unit per slot, so this is a three ingredient recipe.
	 */
	public static class Brew extends Recipe {

		public static final int ESSENCES = 2;

		private static int seedIndex( Item item ){
			for (int i = 0; i < SEEDS.length; i++){
				if (item.getClass() == SEEDS[i]) return i;
			}
			return -1;
		}

		private static Item seed( ArrayList<Item> ingredients ){
			for (Item i : ingredients){
				if (seedIndex(i) >= 0) return i;
			}
			return null;
		}

		private static Essence essence( ArrayList<Item> ingredients ){
			for (Item i : ingredients){
				if (i instanceof Essence) return (Essence) i;
			}
			return null;
		}

		@Override
		public boolean testIngredients( ArrayList<Item> ingredients ) {
			Item s = seed( ingredients );
			Essence e = essence( ingredients );
			if (s == null || e == null) return false;
			int seeds = 0, essences = 0;
			for (Item i : ingredients){
				if (i.getClass() == s.getClass())      seeds += i.quantity();
				else if (i.getClass() == e.getClass()) essences += i.quantity();
				else return false; //anything else (or essences of mixed regions) spoils the brew
			}
			return seeds >= 1 && essences >= ESSENCES;
		}

		@Override
		public int cost( ArrayList<Item> ingredients ) {
			Essence e = essence( ingredients );
			return 2 + 2 * (e == null ? 1 : e.grade());
		}

		@Override
		public Item brew( ArrayList<Item> ingredients ) {
			if (!testIngredients( ingredients )) return null;
			Item result = sampleOutput( ingredients );
			Class<?> seedClass = seed( ingredients ).getClass();
			int seedsNeeded = 1, essNeeded = ESSENCES;
			for (Item i : ingredients){
				int take;
				if (i.getClass() == seedClass){
					take = Math.min( seedsNeeded, i.quantity() );
					seedsNeeded -= take;
				} else {
					take = Math.min( essNeeded, i.quantity() );
					essNeeded -= take;
				}
				i.quantity( i.quantity() - take );
			}
			return result;
		}

		@Override
		public Item sampleOutput( ArrayList<Item> ingredients ) {
			Item s = seed( ingredients );
			Essence e = essence( ingredients );
			if (s == null || e == null) return null;
			return create( Stat.values()[seedIndex(s)], e.grade() );
		}
	}

}
