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
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Open world building materials, spent on base facilities.
 * Each region favours different materials: cloth and wood up top, stone in the caves,
 * refined metal in the city and the halls.
 */
public abstract class Material extends Item {

	//chance for an enemy to drop a material, and how many material piles a floor gets
	public static final float MOB_DROP_CHANCE = 0.06f;

	{
		stackable = true;
	}

	public abstract float unitWeight();

	//relative chance of each material per region: cloth, wood, stone, metal
	private static final float[][] REGION_WEIGHTS = {
			{ 5, 5, 0, 0 },  //sewers
			{ 4, 5, 1, 0 },  //prison
			{ 1, 3, 5, 1 },  //caves
			{ 1, 1, 3, 5 },  //city
			{ 1, 1, 2, 6 },  //halls
	};

	public static Material random( int region ){
		region = Math.max( 0, Math.min( REGION_WEIGHTS.length-1, region ) );
		switch (Random.chances( REGION_WEIGHTS[region] )){
			case 0: default: return new Cloth();
			case 1:          return new Wood();
			case 2:          return new StoneBlock();
			case 3:          return new RefinedMetal();
		}
	}

	//2 to 3 piles of 1-3 materials on every open world dungeon floor
	public static ArrayList<Item> levelLoot(){
		ArrayList<Item> loot = new ArrayList<>();
		if (!OpenWorld.isActive()) return loot;
		int region = OpenWorld.regionOf( Dungeon.depth );
		int piles = 2 + Random.Int( 2 );
		for (int i = 0; i < piles; i++){
			Material m = random( region );
			m.quantity( Random.IntRange( 1, 3 ) );
			loot.add( m );
		}
		return loot;
	}

	public static void onMobDeath( Mob mob ){
		if (!OpenWorld.isActive() || OpenWorld.onBase() || mob.alignment != Char.Alignment.ENEMY) return;
		if (mob.EXP <= 0 || Random.Float() >= MOB_DROP_CHANCE) return;
		Dungeon.level.drop( random( OpenWorld.regionOf( Dungeon.depth ) ), mob.pos ).sprite.drop();
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
		return 5 * quantity;
	}

	public static class Cloth extends Material {
		{ image = ItemSpriteSheet.MAT_CLOTH; }
		@Override public float unitWeight() { return 0.1f; }
	}

	public static class Wood extends Material {
		{ image = ItemSpriteSheet.MAT_WOOD; }
		@Override public float unitWeight() { return 0.4f; }
	}

	public static class StoneBlock extends Material {
		{ image = ItemSpriteSheet.MAT_STONE; }
		@Override public float unitWeight() { return 0.8f; }
		@Override public int value() { return 8 * quantity; }
	}

	public static class RefinedMetal extends Material {
		{ image = ItemSpriteSheet.MAT_METAL; }
		@Override public float unitWeight() { return 0.6f; }
		@Override public int value() { return 12 * quantity; }
	}

}
