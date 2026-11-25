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

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

/**
 * A high value target's dog tag: proof of the kill. Worth a bounty when sold, more for targets
 * from the outer districts.
 */
public class DogTag extends Item {

	public static final int BOUNTY_PER_ZONE = 120;

	{
		image = ItemSpriteSheet.DOG_TAG;
		stackable = true;
		unique = true;
	}

	private int zone = 0;
	private int kind = 0;

	public DogTag forZone( int zone, int kind ){
		this.zone = zone;
		this.kind = kind;
		return this;
	}

	@Override
	public boolean isSimilar( Item item ) {
		return super.isSimilar( item ) && ((DogTag) item).zone == zone && ((DogTag) item).kind == kind;
	}

	@Override
	public String name() {
		return Messages.get( this, "name", HvtBoss.nameOf( kind ) );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", HvtBoss.nameOf( kind ), zone + 1, BOUNTY_PER_ZONE * (zone + 1) );
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
		return BOUNTY_PER_ZONE * (zone + 1) * quantity;
	}

	private static final String ZONE = "zone";
	private static final String KIND = "kind";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( ZONE, zone );
		bundle.put( KIND, kind );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		zone = bundle.getInt( ZONE );
		kind = bundle.getInt( KIND );
	}

}
