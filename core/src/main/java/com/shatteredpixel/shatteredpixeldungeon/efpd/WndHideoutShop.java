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

import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.LightAmmo;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.RifleAmmo;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.ShotgunShell;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Bandage;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Canteen;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.RecallCharm;
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

import java.util.ArrayList;

/**
 * Fixed stock sold at the hideout. Bought items go into the hero's inventory,
 * and into the stash when the hero finishes up.
 */
public class WndHideoutShop extends WndOptions {

	//stock and prices, edit here to change what the hideout sells
	private static final Class<?>[] STOCK    = { Bandage.class, Splint.class, FirstAidKit.class, Food.class, PotionOfHealing.class,
	                                             LightAmmo.class, RifleAmmo.class, ShotgunShell.class,
	                                             Canteen.class, RecallCharm.class };
	private static final int[]      PRICES   = { 40,  100, 200, 50, 300,   40, 60, 50,   60, 150 };
	private static final int[]      QUANTITY = { 1,   1,   1,   1,  1,     10, 10, 6,    1,  1   };
	//only sold in open world mode
	private static final boolean[]  OPEN_WORLD_ONLY = { false, false, false, false, false, false, false, false, true, true };

	//stock indices on display; set before the window is built, since WndOptions asks for its
	//buttons from its own constructor
	private static int[] shown = new int[0];

	private static void pickStock(){
		ArrayList<Integer> list = new ArrayList<>();
		for (int i = 0; i < STOCK.length; i++){
			if (!OPEN_WORLD_ONLY[i] || OpenWorld.isActive()) list.add( i );
		}
		shown = new int[list.size()];
		for (int i = 0; i < shown.length; i++) shown[i] = list.get(i);
	}

	private static Item make( int i ){
		Item item = (Item) Reflection.newInstance( STOCK[i] );
		if (item instanceof Canteen) ((Canteen) item).fill();
		item.identify();
		if (QUANTITY[i] > 1) item.quantity( QUANTITY[i] );
		return item;
	}

	private static String[] labels(){
		pickStock();
		String[] labels = new String[shown.length + 1];
		for (int n = 0; n < shown.length; n++){
			int i = shown[n];
			String name = Messages.titleCase(make(i).name());
			if (QUANTITY[i] > 1) name += " x" + QUANTITY[i];
			labels[n] = Messages.get(WndHideoutShop.class, "entry", name, PRICES[i]);
		}
		labels[shown.length] = Messages.get(WndHideoutShop.class, "back");
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
		return index >= shown.length || Dungeon.gold >= PRICES[shown[index]];
	}

	@Override
	protected boolean hasIcon( int index ) {
		return index < shown.length;
	}

	@Override
	protected Image getIcon( int index ) {
		return new ItemSprite( make(shown[index]) );
	}

	@Override
	protected void onSelect( int index ) {
		if (index >= shown.length || Dungeon.gold < PRICES[shown[index]]) return;

		Dungeon.gold -= PRICES[shown[index]];
		Item item = make(shown[index]);
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
