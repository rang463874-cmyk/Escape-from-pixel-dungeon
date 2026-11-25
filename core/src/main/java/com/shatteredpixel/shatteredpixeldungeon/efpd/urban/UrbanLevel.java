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

package com.shatteredpixel.shatteredpixeldungeon.efpd.urban;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.AmmoSupplies;
import com.shatteredpixel.shatteredpixeldungeon.efpd.hvt.Hvt;
import com.shatteredpixel.shatteredpixeldungeon.efpd.hvt.HvtBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.MedicalSupplies;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * An urban warfare district: a wide flat city of streets, parks and buildings, procedurally laid
 * out. Buildings are entered through doors; their stairs lead to upper floors and basements
 * (BuildingLevel). The city wall has a few gates: walking through one escapes the city.
 */
public class UrbanLevel extends Level {

	public static final int SIZE = 72;

	//terrain of the different parts of the city
	static final int GROUND = Terrain.EMPTY;
	static final int ROAD   = Terrain.EMPTY_DECO;
	static final int INDOOR = Terrain.EMPTY_SP;

	public int startCell;

	//EFPD bounty hunt (HVT) mode: the target's hideout and the district vault
	public int hvtKind = 0;
	public int hvtSite = 0;
	public int hvtBossCell = -1;
	public int hvtVaultDoor = -1;
	//only needed between build() and createMobs()/createItems()
	private int[] hvtBossRect = null;
	private final ArrayList<Integer> vaultCells = new ArrayList<>();

	{
		viewDistance = 8;
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
		Music.INSTANCE.play( Assets.Music.CITY_1, true );
	}

	private int zone(){
		return Urban.zoneIndex( Dungeon.depth );
	}

	//--- generation ------------------------------------------------------------------------------

	private final ArrayList<Integer> lootCells = new ArrayList<>();

	//start coordinates of 2-wide roads along one axis, ring roads included
	private static ArrayList<Integer> roadLines( int len ){
		ArrayList<Integer> lines = new ArrayList<>();
		int pos = 2;
		lines.add( pos );
		while (true){
			int next = pos + 2 + Random.IntRange( 9, 12 );
			if ((len - 4) - (next + 2) < 9) break;
			lines.add( next );
			pos = next;
		}
		lines.add( len - 4 );
		return lines;
	}

	@Override
	protected boolean build() {
		setSize( SIZE, SIZE );
		lootCells.clear();
		vaultCells.clear();
		hvtBossCell = hvtVaultDoor = -1;
		hvtBossRect = null;

		//open ground inside the city wall (rings 0 and 1 stay wall)
		for (int y = 2; y < SIZE-2; y++){
			for (int x = 2; x < SIZE-2; x++){
				map[x + y*SIZE] = Random.Int( 9 ) == 0 ? Terrain.GRASS : GROUND;
			}
		}

		ArrayList<Integer> rx = roadLines( SIZE );
		ArrayList<Integer> ry = roadLines( SIZE );
		for (int r : rx) fill( r, 2, r+1, SIZE-3, ROAD );
		for (int r : ry) fill( 2, r, SIZE-3, r+1, ROAD );

		//the starting plaza sits on the road crossing nearest the middle
		int cx = nearest( rx, SIZE/2 ) + 1;
		int cy = nearest( ry, SIZE/2 ) + 1;
		//the plaza covers the crossing and its sidewalks, the buildings around it stay
		int px0 = cx - 2, py0 = cy - 2, px1 = cx + 1, py1 = cy + 1;

		//blocks between the roads
		ArrayList<int[]> buildings = new ArrayList<>();
		for (int i = 0; i < rx.size()-1; i++){
			for (int j = 0; j < ry.size()-1; j++){
				int x0 = rx.get(i) + 2, x1 = rx.get(i+1) - 1;
				int y0 = ry.get(j) + 2, y1 = ry.get(j+1) - 1;
				if (Random.Float() < 0.2f){
					park( x0, y0, x1, y1 );
				} else {
					splitBlock( x0+1, y0+1, x1-1, y1-1, buildings );
				}
			}
		}

		//decide floors, basements and the subway before drawing the buildings
		int[] flat = new int[buildings.size() * Urban.SPEC_SIZE];
		int subway = -1;
		if (!Urban.lastZone( Dungeon.depth ) && !buildings.isEmpty()){
			//the subway entrance is far from the plaza, so finding it takes some exploring
			int best = -1;
			for (int b = 0; b < buildings.size(); b++){
				int[] r = buildings.get(b);
				int d = Math.abs( (r[0]+r[2])/2 - cx ) + Math.abs( (r[1]+r[3])/2 - cy );
				if (d > best && Random.Int(3) != 0){
					best = d;
					subway = b;
				}
			}
			if (subway == -1) subway = Random.Int( buildings.size() );
		}
		//bounty hunt: the target hides in one of the buildings farthest from the plaza,
		//and the district vault takes over another building
		int bossB = -1, vaultB = -1;
		if (Hvt.targetsHere() && buildings.size() >= 3){
			hvtKind = Random.Int( Hvt.KINDS );
			hvtSite = Random.Int( Hvt.SITES );
			ArrayList<Integer> order = new ArrayList<>();
			for (int b = 0; b < buildings.size(); b++) if (b != subway) order.add( b );
			final int fcx = cx, fcy = cy;
			java.util.Collections.sort( order, (a, b2) -> Integer.compare(
					blockDist( buildings.get(b2), fcx, fcy ), blockDist( buildings.get(a), fcx, fcy ) ) );
			bossB = order.get( Random.Int( Math.min( 3, order.size() ) ) );
			ArrayList<Integer> rest = new ArrayList<>( order );
			rest.remove( Integer.valueOf( bossB ) );
			//not right next to the plaza
			for (int i = rest.size()-1; i >= 0 && rest.size() > 1; i--){
				if (blockDist( buildings.get( rest.get(i) ), cx, cy ) < 12) rest.remove( i );
			}
			if (!rest.isEmpty()) vaultB = Random.element( rest );
		}

		for (int b = 0; b < buildings.size(); b++){
			int[] r = buildings.get(b);
			int w = r[2]-r[0]+1, h = r[3]-r[1]+1;
			if (b == vaultB){
				flat[b*Urban.SPEC_SIZE + Urban.SPEC_W] = w;
				flat[b*Urban.SPEC_SIZE + Urban.SPEC_H] = h;
				drawVault( r );
				continue;
			}
			int floors = w*h < 70 ? Random.chances( new float[]{4, 5, 2} )
			                      : Random.chances( new float[]{2, 4, 3, 1} );
			boolean basement = b == subway || Random.Float() < 0.3f;
			flat[b*Urban.SPEC_SIZE + Urban.SPEC_W] = w;
			flat[b*Urban.SPEC_SIZE + Urban.SPEC_H] = h;
			flat[b*Urban.SPEC_SIZE + Urban.SPEC_FLOORS] = floors;
			flat[b*Urban.SPEC_SIZE + Urban.SPEC_BASEMENT] = basement ? 1 : 0;
			flat[b*Urban.SPEC_SIZE + Urban.SPEC_SUBWAY] = b == subway ? 1 : 0;
			drawBuilding( b, r, floors, basement );
			if (b == bossB){
				hvtBossRect = r;
				hvtBossCell = Rooms.freeCell( map, SIZE, r[0], r[1], r[2], r[3], INDOOR );
				if (hvtBossCell == -1) hvtBossCell = ((r[0]+r[2])/2) + ((r[1]+r[3])/2)*SIZE;
				map[hvtBossCell] = INDOOR;
			}
		}
		Urban.recordBuildings( Dungeon.depth, flat );

		scatterDebris();

		//plaza
		fill( px0, py0, px1, py1, INDOOR );
		startCell = cx + cy*SIZE;
		if (zone() > 0){
			//the subway exit the hero came up from, leading back to the previous district
			map[startCell] = Terrain.EXIT;
			//only when the way back is known (reached by subway, or the previous district exists)
			int prev = Urban.prevZoneDepth( Dungeon.depth );
			int sub = findSubway( prev );
			int back = Urban.arrivalFrom >= 100 ? Urban.arrivalFrom : (sub >= 0 ? Urban.floorBranch( sub, -1 ) : -1);
			if (back >= 0){
				transitions.add( new LevelTransition( this, startCell, LevelTransition.Type.BRANCH_ENTRANCE,
						prev, back, LevelTransition.Type.BRANCH_EXIT ) );
			}
		}

		//gates in the city wall: 3 of the 4 sides, each at the end of a road
		ArrayList<Integer> sides = new ArrayList<>();
		for (int s = 0; s < 4; s++) sides.add( s );
		java.util.Collections.shuffle( sides, new java.util.Random( Random.Long() ) );
		for (int k = 0; k < 3; k++){
			int side = sides.get(k);
			ArrayList<Integer> lines = side < 2 ? rx : ry;
			int r = lines.get( Random.IntRange( 1, lines.size()-2 ) );
			for (int o = 0; o < 2; o++){
				int cell;
				switch (side){
					case 0:  cell = (r+o) + SIZE; break;               //top
					case 1:  cell = (r+o) + (SIZE-2)*SIZE; break;      //bottom
					case 2:  cell = 1 + (r+o)*SIZE; break;             //left
					default: cell = (SIZE-2) + (r+o)*SIZE; break;      //right
				}
				map[cell] = Terrain.DOOR;
				transitions.add( new LevelTransition( this, cell, LevelTransition.Type.SURFACE ) );
			}
		}
		return true;
	}

	private static int blockDist( int[] r, int cx, int cy ){
		return Math.abs( (r[0]+r[2])/2 - cx ) + Math.abs( (r[1]+r[3])/2 - cy );
	}

	//the district vault: one sealed hall behind a locked door, opened by the target's vault key
	private void drawVault( int[] r ){
		int x0 = r[0], y0 = r[1], x1 = r[2], y1 = r[3];
		fill( x0, y0, x1, y1, Terrain.WALL );
		fill( x0+1, y0+1, x1-1, y1-1, INDOOR );
		int door = Rooms.addOuterDoor( map, SIZE, x0, y0, x1, y1, INDOOR );
		if (door != -1){
			map[door] = Terrain.LOCKED_DOOR;
			hvtVaultDoor = door;
		}
		Rooms.furnish( map, SIZE, x0, y0, x1, y1, INDOOR );
		for (int i = 0; i < 3; i++){
			int cell = Rooms.freeCell( map, SIZE, x0, y0, x1, y1, INDOOR );
			if (cell != -1 && !vaultCells.contains( cell )) vaultCells.add( cell );
		}
	}

	public boolean vaultLocked(){
		return hvtVaultDoor >= 0 && map[hvtVaultDoor] == Terrain.LOCKED_DOOR;
	}

	//war damage on the streets: wrecks, craters and barricades, never closing a road
	private void scatterDebris(){
		for (int y = 2; y < SIZE-2; y++){
			for (int x = 2; x < SIZE-2; x++){
				int c = x + y*SIZE;
				if (map[c] == ROAD){
					float f = Random.Float();
					if (f < 0.012f && roadStaysOpen( x, y )) map[c] = Terrain.STATUE_SP;   //burnt-out wreck
					else if (f < 0.04f) map[c] = Terrain.EMBERS;                              //scorch marks
				} else if (map[c] == GROUND && Random.Float() < 0.05f){
					map[c] = Terrain.EMPTY_DECO;                                              //rubble
				}
			}
		}
		//a few craters
		for (int i = 0; i < 6; i++){
			int x = Random.IntRange( 4, SIZE-5 ), y = Random.IntRange( 4, SIZE-5 );
			for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++){
				int c = (x+dx) + (y+dy)*SIZE;
				if (map[c] == ROAD || map[c] == GROUND || map[c] == Terrain.GRASS) map[c] = Terrain.EMBERS;
			}
		}
	}

	//a wreck may only take one lane of a 2-wide road
	private boolean roadStaysOpen( int x, int y ){
		int[] n = { -1, 1, -SIZE, SIZE };
		for (int o : n){
			int c = x + y*SIZE + o;
			if (map[c] == Terrain.STATUE_SP) return false;
		}
		return true;
	}

	private static int nearest( ArrayList<Integer> lines, int target ){
		int best = lines.get(0);
		for (int l : lines) if (Math.abs( l - target ) < Math.abs( best - target )) best = l;
		return best;
	}

	private int findSubway( int depth ){
		for (int b = 0; ; b++){
			int[] s = Urban.spec( depth, b );
			if (s == null) return -1;
			if (s[Urban.SPEC_SUBWAY] == 1) return b;
		}
	}

	private void fill( int x0, int y0, int x1, int y1, int terrain ){
		for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) map[x + y*SIZE] = terrain;
	}

	private void park( int x0, int y0, int x1, int y1 ){
		for (int y = y0; y <= y1; y++){
			for (int x = x0; x <= x1; x++){
				float f = Random.Float();
				map[x + y*SIZE] = f < 0.55f ? Terrain.GRASS : f < 0.8f ? Terrain.HIGH_GRASS : GROUND;
			}
		}
		int w = x1-x0+1, h = y1-y0+1;
		if (w >= 7 && h >= 7 && Random.Int(2) == 0){
			int ox = x0 + Random.IntRange( 1, w-5 ), oy = y0 + Random.IntRange( 1, h-5 );
			fill( ox, oy, ox+2, oy+2, Terrain.WATER );
		} else if (w >= 5 && h >= 5){
			map[(x0+w/2) + (y0+h/2)*SIZE] = Terrain.STATUE;
		}
	}

	//one big building per block, or two when the block is long
	private void splitBlock( int x0, int y0, int x1, int y1, ArrayList<int[]> out ){
		int w = x1-x0+1, h = y1-y0+1;
		if (w < 6 || h < 6) return;
		if (w >= 15){
			int cut = x0 + w/2 + Random.IntRange( -1, 1 );
			splitBlock( x0, y0, cut-1, y1, out );
			splitBlock( cut+1, y0, x1, y1, out );
		} else if (h >= 15){
			int cut = y0 + h/2 + Random.IntRange( -1, 1 );
			splitBlock( x0, y0, x1, cut-1, out );
			splitBlock( x0, cut+1, x1, y1, out );
		} else {
			out.add( new int[]{ x0, y0, x1, y1 } );
		}
	}

	private void drawBuilding( int b, int[] r, int floors, boolean basement ){
		int x0 = r[0], y0 = r[1], x1 = r[2], y1 = r[3];
		fill( x0, y0, x1, y1, Terrain.WALL );
		fill( x0+1, y0+1, x1-1, y1-1, INDOOR );
		Rooms.partition( map, SIZE, x0, y0, x1, y1, INDOOR );

		//1 or 2 front doors
		int doors = Random.Int(3) == 0 ? 2 : 1;
		for (int d = 0; d < doors; d++){
			Rooms.addOuterDoor( map, SIZE, x0, y0, x1, y1, INDOOR );
		}

		//stairs up and down, away from the doors
		if (floors > 0){
			int cell = Rooms.freeCell( map, SIZE, x0, y0, x1, y1, INDOOR );
			if (cell != -1){
				map[cell] = Terrain.ENTRANCE;
				transitions.add( new LevelTransition( this, cell, LevelTransition.Type.REGULAR_ENTRANCE,
						Dungeon.depth, Urban.floorBranch( b, 1 ), LevelTransition.Type.REGULAR_EXIT ) );
			}
		}
		if (basement){
			int cell = Rooms.freeCell( map, SIZE, x0, y0, x1, y1, INDOOR );
			if (cell != -1){
				map[cell] = Terrain.EXIT;
				transitions.add( new LevelTransition( this, cell, LevelTransition.Type.REGULAR_EXIT,
						Dungeon.depth, Urban.floorBranch( b, -1 ), LevelTransition.Type.REGULAR_ENTRANCE ) );
			}
		}

		Rooms.furnish( map, SIZE, x0, y0, x1, y1, INDOOR );

		//shelled buildings: holes in the walls and burnt floors
		if (Random.Float() < 0.3f){
			int holes = Random.IntRange( 2, 4 );
			for (int i = 0; i < holes; i++){
				int x, y;
				if (Random.Int(2) == 0){ x = Random.IntRange( x0+1, x1-1 ); y = Random.Int(2) == 0 ? y0 : y1; }
				else                   { y = Random.IntRange( y0+1, y1-1 ); x = Random.Int(2) == 0 ? x0 : x1; }
				map[x + y*SIZE] = Terrain.EMPTY_DECO;
			}
			for (int y = y0+1; y < y1; y++) for (int x = x0+1; x < x1; x++){
				if (map[x + y*SIZE] == INDOOR && Random.Int(6) == 0) map[x + y*SIZE] = Terrain.EMBERS;
			}
		}

		//something left behind on the ground floor
		if (Random.Int(2) == 0){
			int cell = Rooms.freeCell( map, SIZE, x0, y0, x1, y1, INDOOR );
			if (cell != -1) lootCells.add( cell );
		}
	}

	//--- enemies and loot ------------------------------------------------------------------------

	@Override
	public int mobLimit() {
		return 16 + 3 * zone();
	}

	@Override
	public Mob createMob() {
		return UrbanMobs.create( this );
	}

	@Override
	protected void createMobs() {
		for (int i = 0; i < mobLimit(); i++){
			Mob mob = createMob();
			int cell = farCell( 12 );
			if (cell == -1) continue;
			mob.pos = cell;
			mobs.add( mob );
		}
		if (Hvt.targetsHere() && hvtBossCell >= 0 && Hvt.targetAlive( zone() )){
			placeTarget();
		}
	}

	//the target and its escorts, resting in the hideout
	private void placeTarget(){
		Mob old = findMob( hvtBossCell );
		if (old != null) mobs.remove( old );
		HvtBoss boss = new HvtBoss().setKind( hvtKind );
		boss.pos = hvtBossCell;
		mobs.add( boss );

		int escorts = 3 + zone()/2;
		for (int i = 0; i < escorts; i++){
			int cell = -1;
			for (int tries = 0; tries < 40 && cell == -1; tries++){
				int c;
				if (hvtBossRect != null && tries < 20){
					c = Rooms.freeCell( map, SIZE, hvtBossRect[0], hvtBossRect[1], hvtBossRect[2], hvtBossRect[3], INDOOR );
				} else {
					c = hvtBossCell + (Random.IntRange( -4, 4 ) + Random.IntRange( -4, 4 )*SIZE);
				}
				if (c < 0 || c >= length() || !passable[c] || findMob( c ) != null || isTransitionCell( c )) continue;
				cell = c;
			}
			if (cell == -1) continue;
			Mob m = Random.Int( 3 ) == 0 ? new Looter() : new Gunman();
			m.pos = cell;
			mobs.add( m );
		}
	}

	/** After an HVT kill: fresh enemies show up at the gates, and they know where the hero is. */
	public void spawnReinforcements( int count ){
		ArrayList<Integer> gates = new ArrayList<>();
		for (LevelTransition t : transitions){
			if (t.type == LevelTransition.Type.SURFACE) gates.add( t.centerCell );
		}
		if (gates.isEmpty()) return;
		int heroPos = Dungeon.hero != null ? Dungeon.hero.pos : startCell;
		for (int i = 0; i < count; i++){
			int gate = gates.get( i % gates.size() );
			int cell = -1;
			for (int tries = 0; tries < 60 && cell == -1; tries++){
				int c = gate + Random.IntRange( -5, 5 ) + Random.IntRange( -5, 5 )*SIZE;
				if (c < 0 || c >= length() || !passable[c] || solid[c]) continue;
				if (Actor.findChar( c ) != null || isTransitionCell( c )) continue;
				if (distance( c, heroPos ) < 6) continue;
				cell = c;
			}
			if (cell == -1) continue;
			Mob m = UrbanMobs.create( this );
			m.pos = cell;
			m.state = m.WANDERING;
			GameScene.add( m );
			//half of them hunt the hero, the rest hold the gate
			if (i % 2 == 0) m.beckon( heroPos );
			else m.beckon( gate );
		}
	}

	//a random walkable cell at least minDist steps (as the crow flies) from the start
	private int farCell( int minDist ){
		for (int tries = 0; tries < 200; tries++){
			int cell = Random.Int( length() );
			if (!passable[cell] || findMob( cell ) != null) continue;
			if (distance( cell, startCell ) < minDist) continue;
			if (isTransitionCell( cell )) continue;
			return cell;
		}
		return -1;
	}

	@Override
	protected void createItems() {
		//the vault's riches
		for (int i = 0; i < vaultCells.size(); i++){
			Item item = i == 0 ? new Gold( Random.IntRange( 80, 150 ) * (zone() + 1) ) : CityLoot.rare();
			if (item != null) drop( item, vaultCells.get(i) ).type = Heap.Type.CHEST;
		}
		if (!vaultCells.isEmpty() && com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard.canDrop() && Random.Float() < com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard.VAULT_CHANCE){
			drop( new com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard(), vaultCells.get( 0 ) );
		}
		for (int cell : lootCells){
			Item item = Generator.random();
			if (item != null) drop( item, cell );
		}
		ArrayList<Item> extra = new ArrayList<>();
		extra.addAll( MedicalSupplies.levelLoot() );
		extra.addAll( AmmoSupplies.levelLoot() );
		extra.addAll( AmmoSupplies.levelLoot() );
		extra.add( Generator.random( Generator.Category.FOOD ) );
		for (int i = 0; i < 3; i++) extra.add( Generator.random() );
		for (Item item : extra){
			if (item == null) continue;
			int cell = farCell( 6 );
			if (cell != -1) drop( item, cell );
		}
	}

	//--- travel ----------------------------------------------------------------------------------

	@Override
	public LevelTransition getTransition( LevelTransition.Type type ) {
		//arriving from a building floor: land on that building's stairs
		if (Urban.arrivalFrom >= 100 && (type == LevelTransition.Type.REGULAR_ENTRANCE
				|| type == LevelTransition.Type.REGULAR_EXIT)){
			for (LevelTransition t : transitions){
				if (t.destBranch == Urban.arrivalFrom && t.type == type) return t;
			}
		}
		if (type == LevelTransition.Type.BRANCH_ENTRANCE){
			Urban.markZoneArrival();
		}
		if (type == null){
			//never hand out a wall gate as the place to stand
			for (LevelTransition t : transitions){
				if (t.type == LevelTransition.Type.BRANCH_ENTRANCE) return t;
			}
			return new LevelTransition( this, startCell, LevelTransition.Type.REGULAR_ENTRANCE );
		}
		LevelTransition t = super.getTransition( type );
		//stairs that no longer match (a district regrew): stand on the plaza instead
		return t != null ? t : new LevelTransition( this, startCell, LevelTransition.Type.REGULAR_ENTRANCE );
	}

	@Override
	public boolean activateTransition( Hero hero, LevelTransition transition ) {
		if (locked) return false;
		if (transition.type == LevelTransition.Type.SURFACE){
			if (com.shatteredpixel.shatteredpixeldungeon.efpd.OpenWorld.isActive()){
				UrbanEscape.showTownWindow();
			} else {
				UrbanEscape.showGateWindow( hero );
			}
			return false;
		}
		Urban.arrivalFrom = Dungeon.branch;
		Urban.leftStart = true;
		return performTransition( hero, transition );
	}

	@Override
	public int randomRespawnCell( com.shatteredpixel.shatteredpixeldungeon.actors.Char ch ) {
		int cell = super.randomRespawnCell( ch );
		//enemies don't appear right on the plaza
		if (cell != -1 && distance( cell, startCell ) < 6) return -1;
		return cell;
	}

	private static final String START = "efpd_start";
	private static final String HVT_KIND  = "efpd_hvt_kind";
	private static final String HVT_SITE  = "efpd_hvt_site";
	private static final String HVT_BOSS  = "efpd_hvt_boss";
	private static final String HVT_VAULT = "efpd_hvt_vault";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( START, startCell );
		bundle.put( HVT_KIND, hvtKind );
		bundle.put( HVT_SITE, hvtSite );
		bundle.put( HVT_BOSS, hvtBossCell );
		bundle.put( HVT_VAULT, hvtVaultDoor );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		startCell = bundle.getInt( START );
		hvtKind = bundle.getInt( HVT_KIND );
		hvtSite = bundle.getInt( HVT_SITE );
		hvtBossCell = bundle.contains( HVT_BOSS ) ? bundle.getInt( HVT_BOSS ) : -1;
		hvtVaultDoor = bundle.contains( HVT_VAULT ) ? bundle.getInt( HVT_VAULT ) : -1;
	}


	//safe during generation (Level.getTransition(int) reads the current level)
	private boolean isTransitionCell( int cell ){
		for (LevelTransition t : transitions) if (t.centerCell == cell) return true;
		return false;
	}

}
