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
import com.shatteredpixel.shatteredpixeldungeon.efpd.Facilities.Facility;
import com.shatteredpixel.shatteredpixeldungeon.efpd.PermStats.Stat;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

/** Open world survival record: days, deaths, bosses, facilities, stats and the floors' regrowth. */
public class WndSurvivalRecord extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 160;
	private static final int MAX_HEIGHT = 180;

	public WndSurvivalRecord(){
		super();
		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		RenderedTextBlock title = PixelScene.renderTextBlock( msg("title"), 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (width - title.width())/2f, 2 );
		add( title );

		RenderedTextBlock body = PixelScene.renderTextBlock( text(), 6 );
		body.maxWidth( width );

		Component content = new Component();
		content.add( body );
		body.setPos( 0, 0 );
		content.setSize( width, body.height() );

		ScrollPane pane = new ScrollPane( content );
		add( pane );

		float top = title.bottom() + 4;
		int height = (int)Math.min( MAX_HEIGHT, top + body.height() + 2 );
		resize( width, height );
		pane.setRect( 0, top, width, height - top );
	}

	private static String msg( String key, Object... args ){
		return Messages.get(WndSurvivalRecord.class, key, args);
	}

	private static String text(){
		StringBuilder sb = new StringBuilder();
		int day = DayNight.day();
		sb.append( msg("day", day, Messages.get(DayNight.class, DayNight.phase().name().toLowerCase())) ).append('\n');
		sb.append( msg("level", Dungeon.hero.lvl) ).append('\n');
		sb.append( msg("deaths", Statistics.efpdDeaths) ).append('\n');
		sb.append( msg("kills", Statistics.enemiesSlain) ).append('\n');
		sb.append( msg("bosses", Statistics.efpdBossKills) ).append('\n');

		int regions = 1;
		for (int r = 1; r < OpenWorld.REGION_START.length; r++) if (OpenWorld.regionUnlocked(r)) regions++;
		sb.append( msg("regions", regions, OpenWorld.REGION_START.length) ).append('\n');

		if (Statistics.efpdSurvived){
			sb.append( "_" ).append( msg("survived", Statistics.efpdSurvivedTurn / DayNight.DAY_LENGTH + 1) ).append( "_\n" );
		} else {
			sb.append( msg("not_survived") ).append('\n');
		}

		sb.append('\n').append( "_" ).append( msg("facilities") ).append( "_\n" );
		for (Facility f : Facility.values()){
			sb.append( msg("facility", Facilities.name(f), Facilities.level(f), Facilities.MAX_LEVEL) ).append('\n');
		}

		sb.append('\n').append( "_" ).append( msg("stats") ).append( "_\n" );
		int total = 0;
		for (Stat s : Stat.values()){
			total += PermStats.total(s);
			sb.append( msg("stat", PermStats.name(s), PermStats.total(s)) ).append('\n');
		}
		sb.append( msg("stat_total", total) ).append('\n');

		sb.append('\n').append( "_" ).append( msg("regrowth") ).append( "_\n" );
		boolean any = false;
		for (int d = 1; d < 26; d++){
			if (!Dungeon.levelVisitedOW( d )) continue;
			any = true;
			int left = Regen.turnsUntilRegen( d );
			String when = Regen.heldByCorpse( d ) ? msg("regrow_held")
					: left <= 0 ? msg("regrow_ready") : msg("regrow_in", left);
			sb.append( msg( Regen.isBossDepth(d) ? "floor_boss" : "floor", d, when ) ).append('\n');
		}
		if (!any) sb.append( msg("regrow_none") ).append('\n');
		return sb.toString();
	}

}
