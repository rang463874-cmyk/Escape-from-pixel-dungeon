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
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * Everything the hero carried when they died in open world mode. Picking it up
 * gives everything back. Only the newest bag exists: older ones turn to dust.
 */
public class CorpseBag extends Item {

	{
		image = ItemSpriteSheet.BACKPACK;
		unique = true;
	}

	public int id;
	public int gold;
	public ArrayList<Item> items = new ArrayList<>();

	@Override
	public boolean doPickUp( Hero hero, int pos ) {
		if (id != Statistics.efpdCorpseId){
			GLog.w( Messages.get(this, "dust") );
			hero.spendAndNext( pickupDelay() );
			return true;
		}

		for (Item i : items){
			if (!i.collect()){
				Dungeon.level.drop( i, pos ).sprite.drop();
			}
		}
		Dungeon.gold += gold;
		items.clear();
		Statistics.efpdCorpseDepth = -1;
		updateQuickslot();

		Sample.INSTANCE.play( Assets.Sounds.ITEM );
		GLog.p( Messages.get(this, "recovered") );
		hero.spendAndNext( pickupDelay() );
		return true;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", items.size(), gold);
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	private static final String ID    = "id";
	private static final String GOLD  = "gold";
	private static final String ITEMS = "items";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( ID, id );
		bundle.put( GOLD, gold );
		bundle.put( ITEMS, items );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		id = bundle.getInt( ID );
		gold = bundle.getInt( GOLD );
		items.clear();
		for (Bundlable b : bundle.getCollection( ITEMS )){
			if (b instanceof Item) items.add( (Item) b );
		}
	}

}
