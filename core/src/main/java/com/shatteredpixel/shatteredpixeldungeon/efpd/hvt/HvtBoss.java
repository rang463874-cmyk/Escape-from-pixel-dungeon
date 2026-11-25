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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SmokeScreen;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.efpd.urban.CityLoot;
import com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban;
import com.shatteredpixel.shatteredpixeldungeon.efpd.urban.UrbanLevel;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A high value target. Unlike ordinary enemies it fights with tactics:
 *  - it waits in its hideout with its escorts, and calls them when it spots the hero
 *  - ranged targets duck out of the line of fire after a couple of shots and peek out again
 *  - every few turns it lobs an area attack that lands after a short fuse (grenade, mana burst
 *    or firebomb, by type), marked on the ground so the hero can get out of the way
 *  - when badly hurt it drops a smoke screen, runs, and patches itself up once out of sight
 *    (a limited number of times)
 * Killing it drops its dog tag and either its own gear or the key to the district's vault,
 * and raises the alarm (Hvt.onBossKilled).
 */
public class HvtBoss extends Mob {

	public static final float RETREAT_HP   = 0.35f; //share of max HP that makes it run
	public static final int   HEALS        = 2;     //times it can run and patch up
	public static final float HEAL_SHARE   = 0.35f; //share of max HP healed each time
	public static final int   SHOTS_COVER  = 2;     //ranged attacks before ducking into cover
	public static final int   ABILITY_MIN  = 6, ABILITY_MAX = 9; //turns between area attacks
	public static final int   RANGE        = 7;
	public static final int   CALL_RADIUS  = 9;     //escorts it calls when it spots the hero

	private static final Class<?>[] SPRITES = {
			WarlordSprite.class, HexerSprite.class, ButcherSprite.class };

	private int kind = Hvt.WARLORD;
	private int healsLeft = HEALS;
	private int abilityCD = 3;
	private int shots = 0;
	private int coverWait = 0;
	private int retreatTurns = 0;
	private int hiddenTurns = 0;
	private boolean spotted = false;

	{
		spriteClass = WarlordSprite.class;
		properties.add( Property.BOSS );
		lootChance = 0;

		HUNTING = new Tactical();
		FLEEING = new Retreat();
		PASSIVE = new Guarding();
		state = PASSIVE;
	}

	public HvtBoss(){
		super();
		setKind( Hvt.WARLORD );
	}

	private static int zone(){
		return Urban.zoneIndex( Dungeon.depth );
	}

	@SuppressWarnings("unchecked")
	public HvtBoss setKind( int k ){
		kind = Math.max( 0, Math.min( Hvt.KINDS-1, k ) );
		spriteClass = (Class<? extends CharSprite>) SPRITES[kind];
		int z = zone();
		switch (kind){
			case Hvt.WARLORD: HT = 70 + 45*z; defenseSkill = 10 + 4*z; break;
			case Hvt.WARLOCK: HT = 55 + 40*z; defenseSkill = 12 + 4*z; break;
			default:          HT = 95 + 60*z; defenseSkill =  8 + 4*z; break;
		}
		HP = HT;
		EXP = 25 + 10*z;
		maxLvl = 30;
		return this;
	}

	public int kind(){
		return kind;
	}

	public static String nameOf( int kind ){
		return Messages.get( HvtBoss.class, "name_" + kind );
	}

	@Override
	public String name() {
		return nameOf( kind );
	}

	@Override
	public String description() {
		return Messages.get( this, "desc_" + kind ) + "\n\n" + Messages.get( this, "tactics" );
	}

	//--- combat numbers --------------------------------------------------------------------------

	@Override
	public int damageRoll() {
		int z = zone();
		switch (kind){
			case Hvt.WARLORD: return Random.NormalIntRange( 3 + 3*z, 8 + 5*z );
			case Hvt.WARLOCK: return Random.NormalIntRange( 3 + 3*z, 7 + 5*z );
			default:          return Random.NormalIntRange( 5 + 4*z, 11 + 6*z );
		}
	}

	@Override
	public int attackSkill( Char target ) {
		return 16 + 5*zone();
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 2 + 2*zone() );
	}

	@Override
	public float speed() {
		//the butcher speeds up when it gets angry
		float s = super.speed();
		if (kind == Hvt.BUTCHER && HP < HT/2) s *= 1.5f;
		return s;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )) return true;
		if (kind == Hvt.BUTCHER) return false;
		return Dungeon.level.distance( pos, enemy.pos ) <= RANGE
				&& new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE ).collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (!Dungeon.level.adjacent( pos, enemy.pos )) shots++;
		return super.doAttack( enemy );
	}

	@Override
	protected boolean act() {
		if (abilityCD > 0) abilityCD--;
		return super.act();
	}

	//--- tactics ---------------------------------------------------------------------------------

	//first sight of the hero: shout, show the health bar, call the escorts
	private void onSpotted(){
		if (spotted) return;
		spotted = true;
		BossHealthBar.assignBoss( this );
		yell( Messages.get( this, "spot_" + kind ) );
		int cell = enemy != null ? enemy.pos : pos;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m != this && m.alignment == Alignment.ENEMY
					&& Dungeon.level.distance( pos, m.pos ) <= CALL_RADIUS){
				m.beckon( cell );
			}
		}
	}

	@Override
	public void beckon( int cell ) {
		//a noise (like a gunshot) pulls the target out of its hideout too
		if (state == PASSIVE){
			state = WANDERING;
		}
		super.beckon( cell );
	}

	@Override
	public void notice() {
		super.notice();
		if (state == HUNTING) onSpotted();
	}

	//a neighbouring cell out of the enemy's line of fire, -1 if there is none
	private int coverCell(){
		if (enemy == null) return -1;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			if (!Dungeon.level.passable[c] || Dungeon.level.avoid[c] || Actor.findChar( c ) != null) continue;
			if (Dungeon.level.adjacent( c, enemy.pos )) continue;
			if (new Ballistica( enemy.pos, c, Ballistica.PROJECTILE ).collisionPos != c) cells.add( c );
		}
		return cells.isEmpty() ? -1 : Random.element( cells );
	}

	private boolean useAbility( int cell ){
		abilityCD = Random.IntRange( ABILITY_MIN, ABILITY_MAX );
		spend( TICK );
		if (sprite != null && sprite.visible){
			sprite.showStatus( CharSprite.WARNING, Messages.get( this, "throw_" + kind ) );
		}
		boolean animating = HvtBlast.launch( this, cell, kind, new Callback() {
			@Override
			public void call() {
				next();
			}
		} );
		return !animating;
	}

	private void startRetreat(){
		healsLeft--;
		retreatTurns = 0;
		hiddenTurns = 0;
		coverWait = 0;
		if (enemy != null) target = enemy.pos;
		state = FLEEING;
		GameScene.add( Blob.seed( pos, 120, SmokeScreen.class ) );
		if (sprite != null && sprite.visible){
			yell( Messages.get( this, "retreat_" + kind ) );
		}
	}

	private void recover(){
		int heal = Math.min( HT - HP, Math.round( HT * HEAL_SHARE ) );
		if (heal <= 0) return;
		HP += heal;
		if (sprite != null && sprite.visible){
			sprite.emitter().burst( Speck.factory( Speck.HEALING ), 4 );
			sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), FloatingText.HEALING );
			GLog.w( Messages.get( this, "recover", name() ) );
		}
	}

	//stays in its hideout until it sees or hears the hero
	private class Guarding extends Passive {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			if (enemyInFOV && (justAlerted || Random.Int( 3 ) == 0
					|| Dungeon.level.distance( pos, enemy.pos ) <= 3)){
				state = HUNTING;
				target = enemy.pos;
				onSpotted();
			} else if (justAlerted){
				//hit by something it cannot see: go looking
				state = WANDERING;
				target = Dungeon.hero != null ? Dungeon.hero.pos : pos;
			}
			spend( TICK );
			return true;
		}
	}

	private class Tactical extends Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			if (enemyInFOV) onSpotted();

			//badly hurt: smoke and run
			if (healsLeft > 0 && HP < HT * RETREAT_HP){
				startRetreat();
				return FLEEING.act( enemyInFOV, justAlerted );
			}

			//waiting in cover, out of sight
			if (coverWait > 0 && !enemyInFOV){
				coverWait--;
				spend( TICK );
				return true;
			}
			coverWait = 0;

			if (enemyInFOV && enemy != null && !isCharmedBy( enemy )){
				int d = Dungeon.level.distance( pos, enemy.pos );

				//area attack
				if (abilityCD <= 0 && d >= 2 && d <= RANGE
						&& new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE ).collisionPos == enemy.pos){
					return useAbility( enemy.pos );
				}

				//ranged types duck behind cover after a few shots
				if (kind != Hvt.BUTCHER && shots >= SHOTS_COVER && d > 1){
					int c = coverCell();
					if (c != -1){
						shots = 0;
						coverWait = Random.IntRange( 1, 2 );
						int old = pos;
						move( c, false );
						spend( 1 / speed() );
						return moveSprite( old, pos );
					}
				}
			}
			return super.act( enemyInFOV, justAlerted );
		}
	}

	private class Retreat extends Fleeing {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			retreatTurns++;
			hiddenTurns = enemyInFOV ? 0 : hiddenTurns + 1;
			if (hiddenTurns >= 3 || retreatTurns >= 12){
				recover();
				state = HUNTING;
				spend( TICK );
				return true;
			}
			if (enemyInFOV && enemy != null) target = enemy.pos;
			int old = pos;
			if (target != -1 && getFurther( target )){
				spend( 1 / speed() );
				return moveSprite( old, pos );
			}
			spend( TICK );
			nowhereToRun();
			return true;
		}

		@Override
		protected void nowhereToRun() {
			//cornered: patch up a little if it had a moment, then fight
			if (retreatTurns >= 4) recover();
			state = HUNTING;
		}
	}

	//--- death -----------------------------------------------------------------------------------

	@Override
	public void die( Object cause ) {
		int cell = pos;
		super.die( cause );
		yell( Messages.get( this, "die_" + kind ) );

		int z = zone();
		ArrayList<Item> drops = new ArrayList<>();
		drops.add( new DogTag().forZone( z, kind ) );
		boolean vault = Dungeon.level instanceof UrbanLevel && ((UrbanLevel) Dungeon.level).vaultLocked();
		if (vault && Random.Int( 2 ) == 0){
			drops.add( new VaultKey().forDepth( Dungeon.depth ) );
			UrbanLevel lvl = (UrbanLevel) Dungeon.level;
			GLog.p( Messages.get( this, "vault_hint", Hvt.direction( lvl, cell, lvl.hvtVaultDoor ) ) );
		} else {
			drops.add( HvtGear.forKind( kind, z ) );
		}
		drops.add( CityLoot.rare() );
		if (com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard.canDrop() && Random.Float() < com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard.HVT_BASE * (z + 1)){
			drops.add( new com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard() );
			GLog.p( Messages.get( com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Keycard.class, "found" ) );
		}
		for (Item item : drops){
			if (item != null) Dungeon.level.drop( item, cell ).sprite.drop();
		}

		Hvt.onBossKilled( this );
	}

	//--- saving ----------------------------------------------------------------------------------

	private static final String KIND    = "kind";
	private static final String HEALS_L = "heals_left";
	private static final String ABILITY = "ability_cd";
	private static final String SHOTS   = "shots";
	private static final String COVER   = "cover_wait";
	private static final String RETREAT = "retreat_turns";
	private static final String HIDDEN  = "hidden_turns";
	private static final String SPOTTED = "spotted";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( KIND, kind );
		bundle.put( HEALS_L, healsLeft );
		bundle.put( ABILITY, abilityCD );
		bundle.put( SHOTS, shots );
		bundle.put( COVER, coverWait );
		bundle.put( RETREAT, retreatTurns );
		bundle.put( HIDDEN, hiddenTurns );
		bundle.put( SPOTTED, spotted );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		kind = bundle.getInt( KIND );
		spriteClass = (Class<? extends CharSprite>) SPRITES[Math.max( 0, Math.min( Hvt.KINDS-1, kind ) )];
		healsLeft = bundle.getInt( HEALS_L );
		abilityCD = bundle.getInt( ABILITY );
		shots = bundle.getInt( SHOTS );
		coverWait = bundle.getInt( COVER );
		retreatTurns = bundle.getInt( RETREAT );
		hiddenTurns = bundle.getInt( HIDDEN );
		spotted = bundle.getBoolean( SPOTTED );
		if (spotted) BossHealthBar.assignBoss( this );
	}

}
