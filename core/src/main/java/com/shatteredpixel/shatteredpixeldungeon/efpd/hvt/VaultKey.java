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

package com.shatteredpixel.shatteredpixeldungeon.efpd.hvt;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

/**
 * Key to the secret vault of one city district, carried by its high value target. It opens the
 * vault's locked door in that district only (iron keys don't work outside the main dungeon).
 */
public class VaultKey extends Item {

	{
		image = ItemSpriteSheet.VAULT_KEY;
		unique = true;
	}

	private int depth = -1;

	public VaultKey forDepth( int depth ){
		this.depth = depth;
		return this;
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", Urban.zoneIndex( depth ) + 1 );
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
		return 30;
	}

	private static VaultKey find( Hero hero ){
		if (Dungeon.branch != Urban.URBAN_BRANCH) return null;
		for (VaultKey k : hero.belongings.getAllItems( VaultKey.class )){
			if (k.depth == Dungeon.depth) return k;
		}
		return null;
	}

	/** Called by the hero's unlock action outside the main dungeon. */
	public static boolean canOpen( Hero hero, int door ){
		return door == Terrain.LOCKED_DOOR && find( hero ) != null;
	}

	public static boolean open( Hero hero, int door, int cell ){
		if (door != Terrain.LOCKED_DOOR) return false;
		VaultKey k = find( hero );
		if (k == null) return false;
		k.detachAll( hero.belongings.backpack );
		Level.set( cell, Terrain.DOOR );
		GLog.p( Messages.get( VaultKey.class, "opened" ) );
		return true;
	}

	private static final String DEPTH = "depth";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( DEPTH, depth );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		depth = bundle.getInt( DEPTH );
	}

}
