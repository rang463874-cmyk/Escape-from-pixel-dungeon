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

import com.shatteredpixel.shatteredpixeldungeon.efpd.PermStats.Stat;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

/** Hero window tab listing the open world permanent stats. */
public class PermStatsTab extends Component {

	private RenderedTextBlock title;
	//allocated in createChildren, which runs from the super constructor before field initializers
	private RenderedTextBlock[] names;
	private RenderedTextBlock[] effects;
	private RenderedTextBlock[] details;

	@Override
	protected void createChildren() {
		title = PixelScene.renderTextBlock( Messages.get(PermStats.class, "tab_title"), 9 );
		title.hardlight( Window.TITLE_COLOR );
		add( title );
		int n = Stat.values().length;
		names = new RenderedTextBlock[n];
		effects = new RenderedTextBlock[n];
		details = new RenderedTextBlock[n];
		for (int i = 0; i < n; i++){
			names[i] = PixelScene.renderTextBlock( 7 );
			add( names[i] );
			effects[i] = PixelScene.renderTextBlock( 6 );
			add( effects[i] );
			details[i] = PixelScene.renderTextBlock( 5 );
			add( details[i] );
		}
	}

	public void refresh(){
		for (Stat s : Stat.values()){
			int i = s.ordinal();
			names[i].text( Messages.get(PermStats.class, "row", PermStats.name(s), PermStats.total(s)) );
			names[i].hardlight( PermStats.total(s) > 0 ? 0xFFFF44 : 0xFFFFFF );
			effects[i].text( PermStats.effectDesc(s) );
			String progress = PermStats.useLevel(s) >= PermStats.MAX_USE
					? Messages.get(PermStats.class, "use_maxed")
					: Messages.get(PermStats.class, "progress", (int)PermStats.xp(s), PermStats.xpToNext(s));
			details[i].text( Messages.get(PermStats.class, "detail", PermStats.useLevel(s), PermStats.MAX_USE,
					progress, PermStats.tonicLevel(s), PermStats.MAX_TONIC) );
			details[i].hardlight( 0xCCCCCC );
		}
		layout();
	}

	@Override
	protected void layout() {
		title.setPos( x + (width - title.width())/2f, y + 2 );
		PixelScene.align( title );
		float pos = title.bottom() + 5;
		for (int i = 0; i < names.length; i++){
			names[i].setPos( x, pos );
			PixelScene.align( names[i] );
			effects[i].setPos( x + width - effects[i].width(), pos + (names[i].height() - effects[i].height())/2f );
			PixelScene.align( effects[i] );
			pos = names[i].bottom() + 2;
			details[i].setPos( x + 3, pos );
			PixelScene.align( details[i] );
			pos = details[i].bottom() + 4;
		}
	}

}
