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
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AlchemyScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;

/** The base keeper's menu: region map, shared stash, trading and alchemy. */
public class WndBase extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 150;
	private static final int BTN_HEIGHT = 16;
	private static final int GAP = 2;

	private float pos;
	private int width;

	public WndBase(){
		super();
		width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		pos = GAP;

		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get(this, "title"), 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (width - title.width())/2f, pos );
		add( title );
		pos = title.bottom() + 2*GAP;

		Stash.load();
		RenderedTextBlock info = PixelScene.renderTextBlock(
				Messages.get(this, "info", Dungeon.gold, Stash.gold, Stash.items.size()), 6 );
		info.maxWidth( width );
		info.setPos( 0, pos );
		add( info );
		pos = info.bottom() + 2*GAP;

		addButton( Messages.get(this, "map"), new Runnable() { public void run() {
			hide(); WndWorldMap.show();
		}});
		addButton( Messages.get(this, "facilities"), new Runnable() { public void run() {
			hide(); WndFacilities.show();
		}});
		addButton( Messages.get(WndSurvivalRecord.class, "title"), new Runnable() { public void run() {
			hide(); GameScene.show( new WndSurvivalRecord() );
		}});
		addButton( Messages.get(this, "stash"), new Runnable() { public void run() {
			hide(); GameScene.show( new WndStash() );
		}});
		addButton( Messages.get(this, "deposit"), new Runnable() { public void run() {
			hide(); selectDeposit();
		}});
		addButton( Messages.get(this, "deposit_gold"), new Runnable() { public void run() {
			hide();
			if (Dungeon.gold <= 0){
				GLog.w( Messages.get(WndBase.class, "no_gold") );
				return;
			}
			int amount = Dungeon.gold;
			Stash.depositGold();
			GLog.p( Messages.get(WndBase.class, "gold_stored", amount) );
		}});
		addButton( Messages.get(this, "market"), new Runnable() { public void run() {
			hide(); showMarketMenu();
		}});
		addButton( Messages.get(this, "training"), new Runnable() { public void run() {
			hide(); TownServices.showTraining();
		}});
		addButton( Messages.get(WndHideout.class, "alchemy"), new Runnable() { public void run() {
			hide();
			//the stash's alchemical energy is usable at the town's alchemy station
			Stash.load();
			if (Stash.energy > 0){
				Dungeon.energy += Stash.energy;
				Stash.energy = 0;
				Stash.save();
			}
			AlchemyScene.clearToolkit();
			ShatteredPixelDungeon.switchScene( AlchemyScene.class );
		}});

		resize( width, (int)pos );
	}

	//trading: supplies, rare goods, the mystery crate, selling
	public static void showMarketMenu(){
		WndFacilities.Builder b = new WndFacilities.Builder();
		b.add( Messages.get(WndHideout.class, "buy"), true, new Runnable() { public void run() {
			GameScene.show( new WndHideoutShop() );
		}});
		b.add( Messages.get(TownServices.class, "market_title"), true, new Runnable() { public void run() {
			TownServices.showMarket();
		}});
		b.add( Messages.get(TownServices.class, "crate_title"), true, new Runnable() { public void run() {
			TownServices.showCrate();
		}});
		b.add( Messages.get(WndHideout.class, "sell"), true, new Runnable() { public void run() {
			Shopkeeper.sell();
		}});
		b.add( Messages.get(TownServices.class, "back"), true, new Runnable() { public void run() {} } );
		b.show( Messages.get(WndBase.class, "market"),
				Messages.get(WndBase.class, "market_desc", Dungeon.gold, Facilities.haveGold() - Dungeon.gold) );
	}

	private void addButton( String label, final Runnable action ){
		RedButton btn = new RedButton( label ){
			@Override
			protected void onClick() {
				action.run();
			}
		};
		btn.setRect( 0, pos, width, BTN_HEIGHT );
		add( btn );
		pos = btn.bottom() + GAP;
	}

	public static void selectDeposit(){
		GameScene.selectItem( new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(WndBase.class, "deposit_prompt");
			}

			@Override
			public boolean itemSelectable( Item item ) {
				return Stash.canDeposit( item ) && !item.isEquipped( Dungeon.hero );
			}

			@Override
			public void onSelect( Item item ) {
				if (item == null) return;
				if (Stash.depositOne( Dungeon.hero, item )){
					GLog.i( Messages.get(WndBase.class, "stored", Messages.titleCase(item.name())) );
				}
				//keep depositing until the player cancels
				selectDeposit();
			}
		});
	}

}
