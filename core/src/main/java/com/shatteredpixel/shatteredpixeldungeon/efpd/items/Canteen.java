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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.efpd.Nausea;
import com.shatteredpixel.shatteredpixeldungeon.efpd.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.efpd.Thirst;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Canteen: holds up to MAX_SIPS sips of water.
 *  - Filled at the base well or spring (clean), or from any water in the dungeon (raw).
 *  - Raw water can upset the stomach (Nausea). Boiling it in town makes it clean.
 *  - Mixing raw water into a canteen spoils the whole canteen.
 */
public class Canteen extends Item {

	public static final String AC_DRINK  = "DRINK";
	public static final String AC_FILL   = "FILL";
	public static final String AC_SOURCE = "SOURCE"; //drink straight from the water source
	public static final String AC_BOIL   = "BOIL";

	public static final int MAX_SIPS = 5;

	private int sips = 0;
	private boolean raw = false;

	{
		image = ItemSpriteSheet.CANTEEN;
		defaultAction = AC_DRINK;
	}

	public Canteen fill(){
		sips = MAX_SIPS;
		raw = false;
		return this;
	}

	public int sips(){
		return sips;
	}

	public boolean raw(){
		return raw;
	}

	public float weight(){
		return 0.3f + 0.2f * sips;
	}

	//--- water sources ---------------------------------------------------------------------------

	//-1 = no water nearby, 0 = raw water, 1 = clean water
	public static int sourceNear( Hero hero ){
		int result = -1;
		for (int i : PathFinder.NEIGHBOURS9){
			int cell = hero.pos + i;
			if (cell < 0 || cell >= Dungeon.level.length()) continue;
			int t = Dungeon.level.map[cell];
			if (t == Terrain.EMPTY_WELL || t == Terrain.WELL){
				return 1;
			}
			if (Dungeon.level.water[cell]){
				//the town spring is clean
				result = OpenWorld.onBase() ? 1 : Math.max( result, 0 );
			}
		}
		return result;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_DRINK );
		if (sourceNear( hero ) >= 0){
			actions.add( AC_FILL );
			actions.add( AC_SOURCE );
		}
		if (OpenWorld.onBase() && raw && sips > 0){
			actions.add( AC_BOIL );
		}
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );

		if (action.equals( AC_DRINK )) {
			if (sips <= 0){
				GLog.w( Messages.get(this, "empty") );
				return;
			}
			if (drink( hero, raw )){
				sips--;
				if (sips == 0) raw = false;
				updateQuickslot();
			}

		} else if (action.equals( AC_SOURCE )) {
			int src = sourceNear( hero );
			if (src < 0) return;
			drink( hero, src == 0 );

		} else if (action.equals( AC_FILL )) {
			int src = sourceNear( hero );
			if (src < 0) return;
			if (sips >= MAX_SIPS){
				GLog.i( Messages.get(this, "full") );
				return;
			}
			if (src == 0){
				raw = true;
			} else if (sips == 0){
				raw = false;
			}
			sips = MAX_SIPS;
			GLog.i( Messages.get(this, raw ? "filled_raw" : "filled") );
			Sample.INSTANCE.play( Assets.Sounds.WATER );
			hero.sprite.operate( hero.pos );
			hero.spendAndNext( 2f );
			updateQuickslot();

		} else if (action.equals( AC_BOIL )) {
			raw = false;
			GLog.p( Messages.get(this, "boiled") );
			Sample.INSTANCE.play( Assets.Sounds.BURNING );
			hero.sprite.operate( hero.pos );
			hero.spendAndNext( 3f );
		}
	}

	//returns true if the hero actually drank
	private boolean drink( Hero hero, boolean rawWater ){
		Thirst thirst = hero.buff( Thirst.class );
		if (thirst == null){
			GLog.i( Messages.get(this, "not_thirsty") );
			return false;
		}
		if (Nausea.blocks( hero )) return false;

		thirst.quench( Thirst.SIP );
		GLog.i( Messages.get(this, "drink") );
		if (rawWater && Random.Float() < Nausea.RAW_WATER_CHANCE){
			Buff.affect( hero, Nausea.class, Nausea.DURATION );
			GLog.n( Messages.get(this, "sick") );
		}
		Sample.INSTANCE.play( Assets.Sounds.DRINK );
		hero.sprite.operate( hero.pos );
		hero.spendAndNext( 1f );
		return true;
	}

	@Override
	public String status() {
		return sips + "/" + MAX_SIPS;
	}

	@Override
	public String info() {
		String info = super.info() + "\n\n" + Messages.get(this, "contents", sips, MAX_SIPS);
		if (sips > 0) info += " " + Messages.get(this, raw ? "is_raw" : "is_clean");
		return info;
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
		return 20;
	}

	private static final String SIPS = "sips";
	private static final String RAW  = "raw";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( SIPS, sips );
		bundle.put( RAW, raw );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		sips = bundle.getInt( SIPS );
		raw = bundle.getBoolean( RAW );
	}

}
