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

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AlchemyScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingGridPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.BitmapText;

/**
 * Open world town alchemy: picks ingredients straight out of the stash. Each tap puts one unit
 * into a free alchemy slot; anything left unused goes back to the stash.
 */
public class WndStashPick extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 160;
	private static final int HEIGHT  = 140;
	private static final int BTN_HEIGHT = 16;
	private static final int GAP = 2;

	private final AlchemyScene scene;
	private final RenderedTextBlock title;
	private final ScrollingGridPane grid;
	private final RedButton close;

	public WndStashPick( AlchemyScene scene ){
		super();
		this.scene = scene;
		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		resize( width, HEIGHT );

		title = PixelScene.renderTextBlock( Messages.get( this, "title" ), 8 );
		title.hardlight( TITLE_COLOR );
		title.maxWidth( width );
		add( title );

		grid = new ScrollingGridPane();
		add( grid );

		close = new RedButton( Messages.get( this, "close" ) ){
			@Override
			protected void onClick() {
				hide();
			}
		};
		add( close );

		refresh();
	}

	private void refresh(){
		Stash.load();
		grid.clear();
		boolean any = false;
		for (final Item item : Stash.items){
			if (!Recipe.usableInRecipe( item )) continue;
			any = true;
			ScrollingGridPane.GridItem slot = new ScrollingGridPane.GridItem( new ItemSprite( item ) ){
				@Override
				public boolean onClick( float x, float y ) {
					if (!inside( x, y )) return false;
					pick( item );
					return true;
				}
			};
			if (item.quantity() > 1){
				BitmapText qty = new BitmapText( Integer.toString( item.quantity() ), PixelScene.pixelFont );
				qty.measure();
				slot.addSecondIcon( qty );
			}
			grid.addItem( slot );
		}
		if (!any) grid.addHeader( Messages.get( this, "empty" ), 6, true );

		title.setPos( (width - title.width())/2f, GAP );
		PixelScene.align( title );
		close.setRect( 0, height - BTN_HEIGHT, width, BTN_HEIGHT );
		float top = title.bottom() + 2*GAP;
		grid.setRect( 0, top, width, close.top() - GAP - top );
	}

	private void pick( Item item ){
		if (!scene.slotFree()){
			hide();
			return;
		}
		boolean whole = item instanceof LiquidMetal || item instanceof MissileWeapon;
		Item taken = Stash.takeForUse( item, whole );
		if (taken == null) return;
		if (!scene.addFromStash( taken )){
			Stash.putBack( taken );
		}
		if (scene.slotFree()) refresh();
		else hide();
	}

}
