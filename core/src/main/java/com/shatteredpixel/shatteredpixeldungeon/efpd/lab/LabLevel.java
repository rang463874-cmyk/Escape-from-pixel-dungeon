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

package com.shatteredpixel.shatteredpixeldungeon.efpd.lab;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.AmmoSupplies;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.MedicalSupplies;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Lab complex: a 4x4 grid of rooms joined by corridors. The hero starts in one corner, the
 * escape elevator and its emergency lever sit in the opposite corner, and the generators that
 * power them are spread over the other corners and the middle. Gas vents dot the floor.
 *
 * Generators, the lever and the elevator are transition cells, so the hero uses them by
 * walking onto them and the level handles the rest (activateTransition).
 */
public class LabLevel extends Level {

	public static final int SIZE = 64;
	private static final int GRID = 4, CELL = SIZE / GRID;

	static final int FLOOR    = Terrain.EMPTY_SP;   //room floors
	static final int HALL     = Terrain.EMPTY;      //corridors
	static final int GEN_OFF  = Terrain.PEDESTAL;   //generator, not running
	static final int GEN_ON   = Terrain.EMPTY_DECO; //generator, running
	static final int LEVER    = Terrain.EMPTY_WELL; //emergency lever
	static final int VENT     = Terrain.EMBERS;     //gas vent
	static final int ELEVATOR = Terrain.EXIT;       //escape elevator

	public int startCell = -1;
	public int exitCell = -1;
	public int leverCell = -1;
	private int[] genCells = new int[0];
	private int[] ventCells = new int[0];
	private boolean leverPulled = false;

	{
		viewDistance = 6;
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_CITY;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_SEWERS;
	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.play( leverPulled ? Assets.Music.HALLS_BOSS : Assets.Music.CITY_TENSE, true );
	}

	public int generatorsOn(){
		int n = 0;
		for (int c : genCells) if (map[c] == GEN_ON) n++;
		return n;
	}

	public boolean leverPulled(){
		return leverPulled;
	}

	public int[] vents(){
		return ventCells;
	}

	//--- generation ------------------------------------------------------------------------------

	private int[][] rooms;
	private final ArrayList<Integer> lootCells = new ArrayList<>();
	private final ArrayList<Integer> chestCells = new ArrayList<>();

	@Override
	protected boolean build() {
		setSize( SIZE, SIZE );
		lootCells.clear();
		chestCells.clear();
		leverPulled = false;
		for (int i = 0; i < length(); i++) map[i] = Terrain.WALL;

		//rooms, one per grid cell
		rooms = new int[GRID*GRID][];
		for (int gy = 0; gy < GRID; gy++){
			for (int gx = 0; gx < GRID; gx++){
				int w = Random.IntRange( 8, 13 ), h = Random.IntRange( 8, 13 );
				int x0 = gx*CELL + 1 + Random.Int( CELL - 1 - w );
				int y0 = gy*CELL + 1 + Random.Int( CELL - 1 - h );
				int[] r = { x0, y0, x0+w-1, y0+h-1 };
				rooms[gx + gy*GRID] = r;
				fill( r[0]+1, r[1]+1, r[2]-1, r[3]-1, FLOOR );
			}
		}

		//corridors: a random spanning tree over the grid, plus a few loops
		boolean[] seen = new boolean[GRID*GRID];
		ArrayList<Integer> stack = new ArrayList<>();
		stack.add( 0 );
		seen[0] = true;
		while (!stack.isEmpty()){
			int cur = stack.get( stack.size()-1 );
			ArrayList<Integer> next = new ArrayList<>();
			for (int n : gridNeighbours( cur )) if (!seen[n]) next.add( n );
			if (next.isEmpty()){
				stack.remove( stack.size()-1 );
				continue;
			}
			int n = Random.element( next );
			seen[n] = true;
			corridor( cur, n );
			stack.add( n );
		}
		for (int i = 0; i < 4; i++){
			int a = Random.Int( GRID*GRID );
			ArrayList<Integer> ns = gridNeighbours( a );
			corridor( a, Random.element( ns ) );
		}

		//lab equipment in room corners
		for (int[] r : rooms) furnish( r );

		//start, elevator and lever
		startCell = center( rooms[0] );
		map[startCell] = FLOOR;
		int[] er = rooms[GRID*GRID-1];
		exitCell = center( er );
		map[exitCell] = ELEVATOR;
		transitions.add( new LevelTransition( this, exitCell, LevelTransition.Type.SURFACE ) );
		leverCell = freeCell( er );
		if (leverCell == -1) leverCell = exitCell - 1;
		map[leverCell] = LEVER;
		transitions.add( new LevelTransition( this, leverCell, LevelTransition.Type.SURFACE ) );

		//generators: the two other corners and somewhere in the middle
		int[] genRooms = { GRID-1, GRID*(GRID-1), Random.element( new Integer[]{5, 6, 9, 10} ) };
		genCells = new int[Lab.GENERATORS];
		for (int i = 0; i < Lab.GENERATORS; i++){
			int[] r = rooms[genRooms[i % genRooms.length]];
			int c = freeCell( r );
			if (c == -1) c = center( r );
			map[c] = GEN_OFF;
			genCells[i] = c;
			transitions.add( new LevelTransition( this, c, LevelTransition.Type.SURFACE ) );
		}

		//gas vents, not in the starting room
		ArrayList<Integer> vents = new ArrayList<>();
		for (int tries = 0; tries < 400 && vents.size() < 10; tries++){
			int c = Random.Int( length() );
			if ((map[c] == FLOOR || map[c] == HALL) && distance( c, startCell ) > 7) {
				map[c] = VENT;
				vents.add( c );
			}
		}
		ventCells = new int[vents.size()];
		for (int i = 0; i < ventCells.length; i++) ventCells[i] = vents.get( i );

		//loot: something in most rooms, and a few locked-up caches
		for (int i = 1; i < rooms.length; i++){
			if (Random.Float() < 0.6f){
				int c = freeCell( rooms[i] );
				if (c != -1) lootCells.add( c );
			}
		}
		for (int i = 0; i < 3; i++){
			int c = freeCell( rooms[Random.IntRange( 1, rooms.length-1 )] );
			if (c != -1 && !lootCells.contains( c )) chestCells.add( c );
		}
		return true;
	}

	private ArrayList<Integer> gridNeighbours( int i ){
		ArrayList<Integer> out = new ArrayList<>();
		int gx = i % GRID, gy = i / GRID;
		if (gx > 0) out.add( i-1 );
		if (gx < GRID-1) out.add( i+1 );
		if (gy > 0) out.add( i-GRID );
		if (gy < GRID-1) out.add( i+GRID );
		return out;
	}

	private int center( int[] r ){
		return (r[0]+r[2])/2 + ((r[1]+r[3])/2)*SIZE;
	}

	private void fill( int x0, int y0, int x1, int y1, int t ){
		for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) map[x + y*SIZE] = t;
	}

	private boolean onRoomWall( int x, int y ){
		for (int[] r : rooms){
			if (x < r[0] || x > r[2] || y < r[1] || y > r[3]) continue;
			if (x == r[0] || x == r[2] || y == r[1] || y == r[3]) return true;
		}
		return false;
	}

	private void carve( int x, int y ){
		int c = x + y*SIZE;
		if (map[c] != Terrain.WALL) return;
		map[c] = onRoomWall( x, y ) ? Terrain.DOOR : HALL;
	}

	//an L-shaped corridor between two room centres
	private void corridor( int a, int b ){
		int ca = center( rooms[a] ), cb = center( rooms[b] );
		int x = ca % SIZE, y = ca / SIZE, tx = cb % SIZE, ty = cb / SIZE;
		boolean horizFirst = Random.Int( 2 ) == 0;
		if (horizFirst){
			while (x != tx){ carve( x, y ); x += Integer.signum( tx - x ); }
			while (y != ty){ carve( x, y ); y += Integer.signum( ty - y ); }
		} else {
			while (y != ty){ carve( x, y ); y += Integer.signum( ty - y ); }
			while (x != tx){ carve( x, y ); x += Integer.signum( tx - x ); }
		}
		carve( x, y );
	}

	private boolean nearDoor( int c ){
		for (int n : PathFinder.NEIGHBOURS8){
			int t = map[c+n];
			if (t == Terrain.DOOR || t == HALL) return true;
		}
		return false;
	}

	//a room floor cell away from doors and special cells, -1 if none
	private int freeCell( int[] r ){
		for (int tries = 0; tries < 60; tries++){
			int x = Random.IntRange( r[0]+1, r[2]-1 ), y = Random.IntRange( r[1]+1, r[3]-1 );
			int c = x + y*SIZE;
			if (map[c] == FLOOR && !nearDoor( c ) && c != startCell) return c;
		}
		return -1;
	}

	private void furnish( int[] r ){
		for (int y = r[1]+1; y < r[3]; y++){
			for (int x = r[0]+1; x < r[2]; x++){
				int c = x + y*SIZE;
				if (map[c] != FLOOR || nearDoor( c )) continue;
				boolean wallV = map[c-SIZE] == Terrain.WALL || map[c+SIZE] == Terrain.WALL;
				boolean wallH = map[c-1] == Terrain.WALL || map[c+1] == Terrain.WALL;
				if (wallV && wallH && Random.Float() < 0.4f) map[c] = Terrain.STATUE_SP;
			}
		}
	}

	//--- enemies and loot ------------------------------------------------------------------------

	@Override
	public int mobLimit() {
		return 12;
	}

	@Override
	public Mob createMob() {
		return LabMobs.create();
	}

	@Override
	protected void createMobs() {
		for (int i = 0; i < mobLimit(); i++){
			Mob m = createMob();
			int c = randomCell( startCell, 14, SIZE*2 );
			if (c == -1) continue;
			m.pos = c;
			mobs.add( m );
		}
		//every generator is guarded
		for (int g : genCells){
			Mob m = createMob();
			int c = randomCell( g, 1, 3 );
			if (c == -1) continue;
			m.pos = c;
			mobs.add( m );
		}
	}

	//a free walkable cell between minD and maxD steps (as the crow flies) from a cell
	private int randomCell( int from, int minD, int maxD ){
		for (int tries = 0; tries < 300; tries++){
			int c = Random.Int( length() );
			if (!passable[c] || solid[c] || findMob( c ) != null || Actor.findChar( c ) != null) continue;
			if (isSpecial( c )) continue;
			int d = distance( c, from );
			if (d < minD || d > maxD) continue;
			if (distance( c, startCell ) < 8) continue;
			return c;
		}
		return -1;
	}

	private boolean isSpecial( int c ){
		for (LevelTransition t : transitions) if (t.centerCell == c) return true;
		return c == startCell;
	}

	@Override
	protected void createItems() {
		for (int c : lootCells){
			Item item = Random.Int( 3 ) == 0 ? LabLoot.rare() : LabLoot.normal();
			if (item != null) drop( item, c );
		}
		for (int c : chestCells){
			Item item = LabLoot.rare();
			if (item != null) drop( item, c ).type = Heap.Type.CHEST;
		}
		ArrayList<Item> extra = new ArrayList<>();
		extra.addAll( MedicalSupplies.levelLoot() );
		extra.addAll( MedicalSupplies.levelLoot() );
		for (int i = 0; i < 3; i++) extra.addAll( AmmoSupplies.levelLoot() );
		extra.add( Generator.random( Generator.Category.FOOD ) );
		extra.add( Generator.random( Generator.Category.FOOD ) );
		for (Item item : extra){
			if (item == null) continue;
			int c = randomCell( startCell, 6, SIZE*2 );
			if (c != -1) drop( item, c );
		}
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		int cell = super.randomRespawnCell( ch );
		if (cell != -1 && distance( cell, startCell ) < 8) return -1;
		return cell;
	}

	/** Elite reinforcements: around a cell, at a distance from the hero, all hunting the hero. */
	private void reinforce( int count, int around, int minD, int maxD ){
		Hero hero = Dungeon.hero;
		for (int i = 0; i < count; i++){
			int c = -1;
			for (int tries = 0; tries < 200 && c == -1; tries++){
				int cand = around + Random.IntRange( -maxD, maxD ) + Random.IntRange( -maxD, maxD )*SIZE;
				if (cand < 0 || cand >= length() || !passable[cand] || solid[cand]) continue;
				if (Actor.findChar( cand ) != null || isSpecial( cand )) continue;
				int d = distance( cand, around );
				if (d < minD || d > maxD) continue;
				if (hero != null && distance( cand, hero.pos ) < 4) continue;
				c = cand;
			}
			if (c == -1) continue;
			Mob m = LabMobs.create();
			m.pos = c;
			m.state = m.WANDERING;
			GameScene.add( m );
			if (hero != null) m.beckon( hero.pos );
		}
	}

	//--- generators, lever, elevator -------------------------------------------------------------

	private int generatorIndex( int cell ){
		for (int i = 0; i < genCells.length; i++) if (genCells[i] == cell) return i;
		return -1;
	}

	@Override
	public boolean activateTransition( Hero hero, LevelTransition transition ) {
		int c = transition.centerCell;

		if (c == exitCell){
			if (leverPulled){
				showEscapeWindow( hero );
			} else {
				GLog.w( Messages.get( Lab.class, "no_power", generatorsOn(), Lab.GENERATORS ) );
			}
			return false;
		}

		if (generatorIndex( c ) >= 0){
			if (map[c] == GEN_ON){
				GLog.i( Messages.get( Lab.class, "gen_running" ) );
				return false;
			}
			set( c, GEN_ON );
			GameScene.updateMap( c );
			Sample.INSTANCE.play( Assets.Sounds.CHARGEUP );
			int on = generatorsOn();
			GLog.p( Messages.get( Lab.class, "gen_on", on, Lab.GENERATORS ) );
			if (on >= Lab.GENERATORS) GLog.p( Messages.get( Lab.class, "all_power" ) );
			//the noise draws attention
			for (Mob m : mobs.toArray( new Mob[0] )){
				if (m.alignment == Char.Alignment.ENEMY && distance( m.pos, c ) <= 14) m.beckon( c );
			}
			reinforce( Lab.GEN_REINFORCE, hero.pos, 7, 12 );
			return false;
		}

		if (c == leverCell){
			if (leverPulled){
				GLog.i( Messages.get( Lab.class, "lever_done" ) );
			} else if (generatorsOn() < Lab.GENERATORS){
				GLog.w( Messages.get( Lab.class, "lever_no_power", generatorsOn(), Lab.GENERATORS ) );
			} else {
				pullLever( hero );
			}
			return false;
		}
		return false;
	}

	private void pullLever( Hero hero ){
		leverPulled = true;
		Sample.INSTANCE.play( Assets.Sounds.ALERT );
		Sample.INSTANCE.play( Assets.Sounds.CHALLENGE );
		PixelScene.shake( 3, 0.8f );
		GLog.n( Messages.get( Lab.class, "lever_pulled" ) );
		//half of them block the way to the elevator, the rest come from all over
		reinforce( Lab.LEVER_REINFORCE / 2, exitCell, 3, 7 );
		reinforce( Lab.LEVER_REINFORCE - Lab.LEVER_REINFORCE / 2, hero.pos, 8, 16 );
		Lab.callEveryone( hero.pos );
		Buff.affect( hero, Lab.Lockdown.class );
		playLevelMusic();
	}

	private void showEscapeWindow( final Hero hero ){
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndOptions( Icons.get( Icons.STAIRS ),
						Messages.get( Lab.class, "escape_title" ),
						Messages.get( Lab.class, "escape_desc" ),
						Messages.get( Lab.class, "escape" ),
						Messages.get( Lab.class, "stay" ) ){
					@Override
					protected void onSelect( int index ) {
						if (index == 0) Lab.escape( hero );
					}
				} );
			}
		} );
	}

	//--- arrival ---------------------------------------------------------------------------------

	@Override
	public LevelTransition getTransition( LevelTransition.Type type ) {
		//the only way in is the start of the run
		return new LevelTransition( this, startCell, LevelTransition.Type.REGULAR_ENTRANCE );
	}

	//--- tiles -----------------------------------------------------------------------------------

	@Override
	public String tileName( int tile ) {
		switch (tile){
			case GEN_OFF:  return Messages.get( Lab.class, "tile_gen_off" );
			case GEN_ON:   return Messages.get( Lab.class, "tile_gen_on" );
			case LEVER:    return Messages.get( Lab.class, "tile_lever" );
			case VENT:     return Messages.get( Lab.class, "tile_vent" );
			case ELEVATOR: return Messages.get( Lab.class, "tile_elevator" );
			case Terrain.STATUE_SP: return Messages.get( Lab.class, "tile_machine" );
			default:       return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile){
			case GEN_OFF:  return Messages.get( Lab.class, "tile_gen_off_desc" );
			case GEN_ON:   return Messages.get( Lab.class, "tile_gen_on_desc" );
			case LEVER:    return Messages.get( Lab.class, leverPulled ? "tile_lever_done_desc" : "tile_lever_desc" );
			case VENT:     return Messages.get( Lab.class, "tile_vent_desc" );
			case ELEVATOR: return Messages.get( Lab.class, leverPulled ? "tile_elevator_on_desc" : "tile_elevator_desc" );
			case Terrain.STATUE_SP: return Messages.get( Lab.class, "tile_machine_desc" );
			default:       return super.tileDesc( tile );
		}
	}

	//--- saving ----------------------------------------------------------------------------------

	private static final String START = "start";
	private static final String EXIT  = "exit_cell";
	private static final String LEVER_CELL = "lever_cell";
	private static final String GENS  = "generators";
	private static final String VENTS = "vents";
	private static final String PULLED = "lever_pulled";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( START, startCell );
		bundle.put( EXIT, exitCell );
		bundle.put( LEVER_CELL, leverCell );
		bundle.put( GENS, genCells );
		bundle.put( VENTS, ventCells );
		bundle.put( PULLED, leverPulled );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		startCell = bundle.getInt( START );
		exitCell = bundle.getInt( EXIT );
		leverCell = bundle.getInt( LEVER_CELL );
		genCells = bundle.getIntArray( GENS );
		ventCells = bundle.getIntArray( VENTS );
		if (genCells == null) genCells = new int[0];
		if (ventCells == null) ventCells = new int[0];
		leverPulled = bundle.getBoolean( PULLED );
	}

}
