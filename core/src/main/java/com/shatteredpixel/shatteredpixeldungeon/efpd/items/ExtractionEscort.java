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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.efpd.Extraction;
import com.shatteredpixel.shatteredpixeldungeon.efpd.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Lab;
import com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

/**
 * Extraction mode shop service: a guide's escort back to the hideout. It can't be carried:
 * the moment it is bought (or otherwise taken), the hero is extracted with everything they
 * carry, like at an extraction point. Costs twice as much as an ankh.
 */
public class ExtractionEscort extends Item {

	{
		image = ItemSpriteSheet.RETURN_BEACON;
		unique = true;
	}

	/** Sold in extraction mode shops only. */
	public static boolean sold(){
		return !OpenWorld.isActive() && !Urban.isActive() && !Lab.isActive() && Dungeon.branch == 0;
	}

	@Override
	public int value() {
		return 2 * new Ankh().value();
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
	public ItemSprite.Glowing glowing() {
		return new ItemSprite.Glowing( 0x66CCFF, 1f );
	}

	@Override
	public boolean doPickUp( final Hero hero, int pos ) {
		//never goes into the inventory: the escort leaves right away
		Sample.INSTANCE.play( Assets.Sounds.TELEPORT );
		GLog.p( Messages.get( this, "used" ) );
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				Extraction.extract( hero );
			}
		} );
		return true;
	}

}
