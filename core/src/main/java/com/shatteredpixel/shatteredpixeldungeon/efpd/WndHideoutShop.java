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
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Bandage;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.FirstAidKit;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Splint;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Image;
import com.watabou.utils.Reflection;

/**
 * Fixed stock sold at the hideout. Bought items go into the hero's inventory,
 * and into the stash when the hero finishes up.
 */
public class WndHideoutShop extends WndOptions {

	//stock and prices, edit here to change what the hideout sells
	private static final Class<?>[] STOCK  = { Bandage.class, Splint.class, FirstAidKit.class, Food.class, PotionOfHealing.class };
	private static final int[]      PRICES = { 40,            100,          200,               50,         300 };

	private static Item make( int i ){
		Item item = (Item) Reflection.newInstance( STOCK[i] );
		item.identify();
		return item;
	}

	private static String[] labels(){
		String[] labels = new String[STOCK.length + 1];
		for (int i = 0; i < STOCK.length; i++){
			labels[i] = Messages.get(WndHideoutShop.class, "entry", Messages.titleCase(make(i).name()), PRICES[i]);
		}
		labels[STOCK.length] = Messages.get(WndHideoutShop.class, "back");
		return labels;
	}

	public WndHideoutShop(){
		super( new ItemSprite(ItemSpriteSheet.GOLD),
				Messages.get(WndHideoutShop.class, "title"),
				Messages.get(WndHideoutShop.class, "desc", Dungeon.gold),
				labels() );
	}

	@Override
	protected boolean enabled( int index ) {
		return index >= STOCK.length || Dungeon.gold >= PRICES[index];
	}

	@Override
	protected boolean hasIcon( int index ) {
		return index < STOCK.length;
	}

	@Override
	protected Image getIcon( int index ) {
		return new ItemSprite( make(index) );
	}

	@Override
	protected void onSelect( int index ) {
		if (index >= STOCK.length || Dungeon.gold < PRICES[index]) return;

		Dungeon.gold -= PRICES[index];
		Item item = make(index);
		if (!item.collect()){
			Dungeon.level.drop( item, Dungeon.hero.pos ).sprite.drop();
		}
		GLog.i( Messages.get(WndHideoutShop.class, "bought", item.name()) );
		//keep shopping
		GameScene.show( new WndHideoutShop() );
	}

	@Override
	public void onBackPressed() {
		hide();
	}

}
