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

package com.shatteredpixel.shatteredpixeldungeon.efpd.hvt;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban;
import com.shatteredpixel.shatteredpixeldungeon.efpd.urban.UrbanLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

/**
 * Bounty hunt (HVT) mode: urban warfare where every district hides a high value target and its
 * escorts. The hero only gets a rough hint of where it is. The target fights with tactics
 * (HvtBoss), and killing it raises the alarm across the whole district: every enemy closes in on
 * the hero and reinforcements guard the gates, so getting out alive is the real test.
 *
 * Everything else (start, gates, stash, subway districts) follows urban warfare rules.
 */
public class Hvt {

	//set by the mode picker before a new game starts
	public static boolean selectedMode = false;

	//target types
	public static final int WARLORD = 0; //gunner with grenades
	public static final int WARLOCK = 1; //caster with mana bursts
	public static final int BUTCHER = 2; //brute with firebombs
	public static final int KINDS   = 3;
	//hideout flavour per target type
	public static final int SITES   = 3;

	//enemies freshly guarding the gates after a kill
	public static final int REINFORCE_BASE = 3;
	//turns between the alarm calling every enemy in the district to the hero
	public static final float ALARM_PULSE  = 10f;

	private static boolean active = false;
	private static boolean[] killed = new boolean[Urban.ZONE_DEPTHS.length];
	private static int bounties = 0;

	public static boolean isActive(){
		return active && Urban.isActive();
	}

	public static void start(){
		active = true;
	}

	public static boolean killed( int zone ){
		return zone >= 0 && zone < killed.length && killed[zone];
	}

	public static int bounties(){
		return bounties;
	}

	//--- hints -----------------------------------------------------------------------------------

	//8-way compass direction from one cell to another, as text
	public static String direction( Level level, int from, int to ){
		if (level.distance( from, to ) < 6) return Messages.get( Hvt.class, "dir_near" );
		int dx = (to % level.width()) - (from % level.width());
		int dy = (to / level.width()) - (from / level.width());
		double angle = Math.toDegrees( Math.atan2( -dy, dx ) );
		int idx = (int)Math.round( angle / 45.0 );
		idx = ((idx % 8) + 8) % 8;
		return Messages.get( Hvt.class, "dir_" + idx );
	}

	public static String briefingText( UrbanLevel level ){
		int zone = Urban.zoneIndex( Dungeon.depth );
		if (killed( zone ) || level.hvtBossCell < 0){
			return Messages.get( Hvt.class, "brief_done", zone+1 );
		}
		int k = level.hvtKind;
		return Messages.get( Hvt.class, "brief", zone+1,
				HvtBoss.nameOf( k ),
				direction( level, level.startCell, level.hvtBossCell ),
				Messages.get( Hvt.class, "site_" + k + "_" + level.hvtSite ),
				Messages.get( Hvt.class, "sign_" + k ) );
	}

	//shown when the hero arrives in a district
	public static void brief(){
		if (!isActive() || !(Dungeon.level instanceof UrbanLevel)) return;
		final String text = briefingText( (UrbanLevel) Dungeon.level );
		GLog.w( text );
		GameScene.show( new WndMessage( Messages.get( Hvt.class, "brief_title" ) + "\n\n" + text ) );
	}

	//--- the kill --------------------------------------------------------------------------------

	public static void onBossKilled( HvtBoss boss ){
		int zone = Urban.zoneIndex( Dungeon.depth );
		if (zone >= 0 && zone < killed.length) killed[zone] = true;
		bounties++;

		Hero hero = Dungeon.hero;
		GLog.n( Messages.get( Hvt.class, "alarm" ) );
		Sample.INSTANCE.play( Assets.Sounds.ALERT );
		Sample.INSTANCE.play( Assets.Sounds.CHALLENGE, 0.6f, 0.8f );
		if (hero == null || !hero.isAlive()) return;

		callEveryone( hero.pos );
		if (Dungeon.level instanceof UrbanLevel){
			((UrbanLevel) Dungeon.level).spawnReinforcements( REINFORCE_BASE + zone );
		}
		Buff.affect( hero, Manhunt.class ).set( Dungeon.depth );
	}

	static void callEveryone( int cell ){
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && m.state != m.HUNTING){
				m.beckon( cell );
			}
		}
	}

	/** After an HVT kill, the district keeps hunting the hero until they leave it. */
	public static class Manhunt extends Buff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		private int depth;

		public void set( int depth ){
			this.depth = depth;
		}

		@Override
		public boolean act() {
			if (Dungeon.depth != depth || !Urban.inCity()){
				detach();
				return true;
			}
			if (Dungeon.branch == Urban.URBAN_BRANCH && target.isAlive()){
				callEveryone( target.pos );
			}
			spend( ALARM_PULSE );
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.TARGETED;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 1f, 0.2f, 0.2f );
		}

		@Override
		public String desc() {
			return Messages.get( this, "desc" );
		}

		private static final String DEPTH = "depth";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( DEPTH, depth );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			depth = bundle.getInt( DEPTH );
		}
	}

	//--- saving ----------------------------------------------------------------------------------

	public static void reset(){
		active = false;
		killed = new boolean[Urban.ZONE_DEPTHS.length];
		bounties = 0;
	}

	private static final String NODE     = "efpd_hvt";
	private static final String ACTIVE   = "active";
	private static final String KILLED   = "killed";
	private static final String BOUNTIES = "bounties";

	public static void storeInBundle( Bundle bundle ){
		Bundle node = new Bundle();
		node.put( ACTIVE, active );
		node.put( KILLED, killed );
		node.put( BOUNTIES, bounties );
		bundle.put( NODE, node );
	}

	public static void restoreFromBundle( Bundle bundle ){
		reset();
		if (!bundle.contains( NODE )) return;
		Bundle node = bundle.getBundle( NODE );
		active = node.getBoolean( ACTIVE );
		boolean[] k = node.getBooleanArray( KILLED );
		if (k != null) System.arraycopy( k, 0, killed, 0, Math.min( k.length, killed.length ) );
		bounties = node.getInt( BOUNTIES );
	}

}
