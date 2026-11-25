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
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingGridPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.BitmapText;

/**
 * Shows the persistent stash. Items can be taken out only before the first descent.
 */
public class WndStash extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 160;
	private static final int HEIGHT  = 150;

	private static final int BTN_HEIGHT = 16;
	private static final int GAP = 2;

	private RenderedTextBlock title;
	private RenderedTextBlock subtitle;
	private ScrollingGridPane grid;
	private RedButton btnGold;

	public WndStash(){
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		resize(width, HEIGHT);

		title = PixelScene.renderTextBlock( Messages.get(this, "title"), 9 );
		title.hardlight( TITLE_COLOR );
		title.maxWidth( width );
		add( title );

		subtitle = PixelScene.renderTextBlock( 6 );
		subtitle.maxWidth( width );
		add( subtitle );

		grid = new ScrollingGridPane();
		add( grid );

		btnGold = new RedButton( "" ){
			@Override
			protected void onClick() {
				if (Stash.gold <= 1){
					Stash.withdrawGold();
					refresh();
					return;
				}
				GameScene.show( new WndAmount( Messages.get(WndStash.class, "gold_amount"),
						Messages.get(WndStash.class, "unit_gold"), Stash.gold, Stash.gold, new WndAmount.Listener() {
					@Override
					public void onAmount( int amount ) {
						Stash.withdrawGold( amount );
						refresh();
					}
				} ) );
			}
		};
		add( btnGold );

		refresh();
	}

	private void refresh(){
		Stash.load();
		boolean canWithdraw = Stash.canWithdraw();

		subtitle.text( Messages.get(this, OpenWorld.onBase() ? "base" : Statistics.efpdInHideout ? "hideout" : canWithdraw ? "can_withdraw" : "locked") );

		grid.clear();
		if (Stash.items.isEmpty()){
			grid.addHeader( Messages.get(this, "empty"), 6, true );
		}
		for (final Item item : Stash.items){
			ScrollingGridPane.GridItem slot = new ScrollingGridPane.GridItem( new ItemSprite(item) ){
				@Override
				public boolean onClick(float x, float y) {
					if (!inside(x, y)) return false;
					showItem( item );
					return true;
				}
			};
			if (item.quantity() > 1){
				BitmapText qty = new BitmapText( Integer.toString(item.quantity()), PixelScene.pixelFont );
				qty.measure();
				slot.addSecondIcon( qty );
			}
			grid.addItem( slot );
		}

		btnGold.text( Messages.get(this, "gold", Stash.gold) );
		btnGold.enable( canWithdraw && Stash.gold > 0 );

		layoutAll();
	}

	private void layoutAll(){
		title.setPos( (width - title.width())/2f, GAP );
		PixelScene.align( title );
		subtitle.setPos( 0, title.bottom() + GAP );
		PixelScene.align( subtitle );

		btnGold.setRect( 0, height - BTN_HEIGHT, width, BTN_HEIGHT );

		float gridTop = subtitle.bottom() + 2*GAP;
		grid.setRect( 0, gridTop, width, btnGold.top() - GAP - gridTop );
	}

	private void showItem( final Item item ){
		final boolean canWithdraw = Stash.canWithdraw();
		final boolean stack = item.quantity() > 1;
		String[] options = stack
				? new String[]{ Messages.get(WndStash.class, "take_all", item.quantity()), Messages.get(WndStash.class, "take_some") }
				: new String[]{ Messages.get(WndStash.class, "take") };
		GameScene.show( new WndOptions( new ItemSprite(item),
				Messages.titleCase( item.title() ),
				item.info() + "\n\n" + Weight.infoLine(item),
				options ){
			@Override
			protected boolean enabled(int index) {
				return canWithdraw;
			}

			@Override
			protected void onSelect(int index) {
				if (!Stash.canWithdraw()) return;
				if (index == 0){
					Stash.withdraw( Dungeon.hero, item );
					refresh();
				} else if (index == 1){
					GameScene.show( new WndAmount( Messages.titleCase( item.name() ),
							Messages.get(WndStash.class, "unit_items"), item.quantity(), 1, new WndAmount.Listener() {
						@Override
						public void onAmount( int amount ) {
							Stash.withdraw( Dungeon.hero, item, amount );
							refresh();
						}
					} ) );
				}
			}
		});
	}

}
