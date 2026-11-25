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

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

/** Picks an amount between 1 and a maximum, with -/+ buttons in steps of 1, 10 and 100. */
public class WndAmount extends Window {

	public interface Listener {
		void onAmount( int amount );
	}

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 150;
	private static final int BTN_H = 16;
	private static final int GAP = 2;

	private final int max;
	private int amount;
	private final RenderedTextBlock value;
	private final String unit;

	public WndAmount( String title, String unit, int max, int start, final Listener listener ){
		super();
		this.max = Math.max( 1, max );
		this.unit = unit;
		amount = Math.max( 1, Math.min( this.max, start ) );
		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		float pos = GAP;

		RenderedTextBlock t = PixelScene.renderTextBlock( title, 9 );
		t.hardlight( TITLE_COLOR );
		t.maxWidth( width );
		t.setPos( (width - t.width())/2f, pos );
		add( t );
		pos = t.bottom() + 2*GAP;

		value = PixelScene.renderTextBlock( 9 );
		add( value );
		float valueY = pos;
		pos += 12 + GAP;

		int[] steps = this.max >= 100 ? new int[]{ 1, 10, 100 } : this.max >= 10 ? new int[]{ 1, 10 } : new int[]{ 1 };
		float bw = (width - (steps.length-1)*GAP) / (float) steps.length;
		for (int row = 0; row < 2; row++){
			for (int i = 0; i < steps.length; i++){
				final int delta = row == 0 ? -steps[steps.length-1-i] : steps[i];
				RedButton b = new RedButton( (delta > 0 ? "+" : "") + delta ){
					@Override
					protected void onClick() {
						set( amount + delta );
					}
				};
				b.setRect( i*(bw + GAP), pos, bw, BTN_H );
				add( b );
			}
			pos += BTN_H + GAP;
		}

		float hw = (width - GAP) / 2f;
		RedButton min = new RedButton( Messages.get( WndAmount.class, "min" ) ){
			@Override
			protected void onClick() { set( 1 ); }
		};
		min.setRect( 0, pos, hw, BTN_H );
		add( min );
		RedButton all = new RedButton( Messages.get( WndAmount.class, "max" ) ){
			@Override
			protected void onClick() { set( WndAmount.this.max ); }
		};
		all.setRect( hw + GAP, pos, hw, BTN_H );
		add( all );
		pos += BTN_H + 2*GAP;

		RedButton ok = new RedButton( Messages.get( WndAmount.class, "ok" ) ){
			@Override
			protected void onClick() {
				hide();
				listener.onAmount( amount );
			}
		};
		ok.setRect( 0, pos, width, BTN_H );
		add( ok );
		pos += BTN_H + GAP;

		resize( width, (int) pos );
		set( amount );
		value.setPos( 0, valueY );
		layoutValue( width );
	}

	private void set( int a ){
		amount = Math.max( 1, Math.min( max, a ) );
		value.text( Messages.get( WndAmount.class, "value", amount, max, unit ) );
		layoutValue( width );
	}

	private void layoutValue( int w ){
		value.setPos( (w - value.width())/2f, value.top() );
		PixelScene.align( value );
	}

}
