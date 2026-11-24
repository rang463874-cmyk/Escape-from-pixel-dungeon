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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AlchemyScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;

/**
 * The hideout menu shown after extracting: sell and buy items, use the alchemy station,
 * manage the stash, and finally finish the run.
 */
public class WndHideout extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 150;
	private static final int BTN_HEIGHT = 18;
	private static final int GAP = 2;

	public WndHideout(){
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		float pos = GAP;

		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get(this, "title"), 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (width - title.width())/2f, pos );
		add( title );
		pos = title.bottom() + 2*GAP;

		RenderedTextBlock info = PixelScene.renderTextBlock(
				Messages.get(this, "info", Dungeon.gold, Dungeon.energy, Weight.label(Dungeon.hero)), 6 );
		info.maxWidth( width );
		info.setPos( 0, pos );
		add( info );
		pos = info.bottom() + 2*GAP;

		pos = addButton( Messages.get(this, "sell"), width, pos, new Runnable() {
			public void run() {
				hide();
				Shopkeeper.sell();
			}
		});
		pos = addButton( Messages.get(this, "buy"), width, pos, new Runnable() {
			public void run() {
				hide();
				GameScene.show( new WndHideoutShop() );
			}
		});
		pos = addButton( Messages.get(this, "alchemy"), width, pos, new Runnable() {
			public void run() {
				hide();
				AlchemyScene.clearToolkit();
				ShatteredPixelDungeon.switchScene( AlchemyScene.class );
			}
		});
		pos = addButton( Messages.get(this, "stash"), width, pos, new Runnable() {
			public void run() {
				hide();
				GameScene.show( new WndStash() );
			}
		});
		pos = addButton( Messages.get(this, "finish"), width, pos + GAP, new Runnable() {
			public void run() {
				hide();
				GameScene.show( new WndOptions( Icons.get(Icons.STAIRS),
						Messages.get(WndHideout.class, "finish_title"),
						Messages.get(WndHideout.class, "finish_desc"),
						Messages.get(WndHideout.class, "finish_yes"),
						Messages.get(WndHideout.class, "finish_no") ){
					@Override
					protected void onSelect(int index) {
						if (index == 0) Extraction.finish( Dungeon.hero );
					}
				});
			}
		});

		resize( width, (int)pos );
	}

	private float addButton( String label, int width, float pos, final Runnable action ){
		RedButton btn = new RedButton( label ){
			@Override
			protected void onClick() {
				action.run();
			}
		};
		btn.setRect( 0, pos, width, BTN_HEIGHT );
		add( btn );
		return btn.bottom() + GAP;
	}

	@Override
	public void onBackPressed() {
		//can't leave the hideout menu, use "finish" instead
	}

}
