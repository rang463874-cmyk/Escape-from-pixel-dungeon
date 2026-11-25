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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.efpd.Extraction;
import com.shatteredpixel.shatteredpixeldungeon.efpd.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.efpd.Regen;
import com.shatteredpixel.shatteredpixeldungeon.efpd.Stash;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Lab: the final destination of Escape from Pixel Dungeon.
 *
 * A sealed high risk research complex. Entering costs a lab keycard taken from the stash; keycards
 * are rare finds in the other extraction modes (late dungeon bosses, HVT targets, city vaults and
 * chests). Inside there are only elite mercenaries and security golems, the air vents leak toxic
 * gas and the hero gets hungry twice as fast.
 *
 * The escape elevator has no power: the hero has to start the generators spread across the
 * complex, then pull the emergency lever next to the elevator. The lever sets off the alarm and
 * elite reinforcements pour in; the hero then has to fight their way into the elevator.
 * Escaping is the final victory, counted in the stash file.
 */
public class Lab {

	public static final int LAB_DEPTH  = 18;  //sets enemy and loot strength
	public static final int LAB_BRANCH = 4;

	public static final int   START_LEVEL   = 12;   //lab operatives are veterans
	public static final int   GENERATORS    = 3;
	public static final float HUNGER_MULTI  = 2f;
	public static final float GAS_INTERVAL  = 12f;  //turns between vent leaks
	public static final int   GAS_AMOUNT    = 250;
	public static final int   GEN_REINFORCE = 2;    //elites showing up when a generator starts
	public static final int   LEVER_REINFORCE = 7;  //elites showing up when the lever is pulled

	//set by the mode picker before a new game starts
	public static boolean selectedMode = false;

	private static boolean active = false;

	public static boolean isActive(){
		return active;
	}

	public static void start(){
		active = true;
	}

	public static void reset(){
		active = false;
	}

	public static boolean inLab(){
		return (active || OpenWorld.isActive()) && Dungeon.branch == LAB_BRANCH && Dungeon.level instanceof LabLevel;
	}

	//--- keycards --------------------------------------------------------------------------------

	public static int keycardsInStash(){
		int n = 0;
		for (Item i : Stash.items) if (i instanceof Keycard) n += i.quantity();
		return n;
	}

	/** Takes one keycard out of the stash for a lab run. Returns false if there is none. */
	public static boolean spendKeycard(){
		Stash.load();
		for (Item i : new ArrayList<>( Stash.items )){
			if (i instanceof Keycard){
				if (i.quantity() > 1) i.quantity( i.quantity() - 1 );
				else Stash.items.remove( i );
				Stash.save();
				return true;
			}
		}
		return false;
	}

	public static String statusLine(){
		Stash.load();
		return Messages.get( Lab.class, "status", keycardsInStash(), Stash.labClears );
	}

	//--- open world entry -------------------------------------------------------------------------

	/** Keycards the open world hero could spend: carried ones and the stash's. */
	public static int keycardsAvailable(){
		int n = 0;
		if (Dungeon.hero != null) for (Keycard k : Dungeon.hero.belongings.getAllItems( Keycard.class )) n += k.quantity();
		Stash.load();
		return n + keycardsInStash();
	}

	public static boolean corpseInLab(){
		return com.shatteredpixel.shatteredpixeldungeon.Statistics.efpdCorpseDepth == LAB_DEPTH
				&& com.shatteredpixel.shatteredpixeldungeon.Statistics.efpdCorpseBranch == LAB_BRANCH;
	}

	/** Open world: spends a keycard (carried first) and goes to the Lab, built anew unless the hero's remains are there. */
	public static boolean enterFromOpenWorld(){
		boolean paid = false;
		for (Keycard k : Dungeon.hero.belongings.getAllItems( Keycard.class )){
			if (k.quantity() > 1) k.quantity( k.quantity() - 1 ); else k.detachAll( Dungeon.hero.belongings.backpack );
			paid = true;
			break;
		}
		if (!paid) paid = spendKeycard();
		if (!paid) return false;
		if (!corpseInLab()) Regen.forceLabRegen();
		OpenWorld.travelTo( LAB_DEPTH, LAB_BRANCH );
		return true;
	}

	//--- environment -----------------------------------------------------------------------------

	public static float hungerMultiplier(){
		return inLab() ? HUNGER_MULTI : 1f;
	}

	public static boolean canWithdraw(){
		if (!inLab() || Dungeon.hero == null) return false;
		LabLevel l = (LabLevel) Dungeon.level;
		return l.generatorsOn() == 0 && !l.leverPulled()
				&& l.distance( Dungeon.hero.pos, l.startCell ) <= 4;
	}

	//shown when the hero arrives
	public static void onArrive(){
		showPendingVictory();
		//out of the Lab (or died in it): its air and alarm are gone right away
		if (!inLab()) clearLabBuffs( Dungeon.hero );
		if (!inLab() || Dungeon.hero == null) return;
		if (Dungeon.hero.buff( Hazard.class ) == null){
			//the first leak comes a while after arriving
			Buff.affect( Dungeon.hero, Hazard.class ).delayStart();
		}
		LabLevel l = (LabLevel) Dungeon.level;
		if (canWithdraw()){
			GameScene.show( new WndMessage( Messages.get( Lab.class, "brief_title" ) + "\n\n"
					+ Messages.get( Lab.class, "brief", GENERATORS ) ) );
		} else if (OpenWorld.isActive() && l.generatorsOn() == 0 && !l.leverPulled()){
			GameScene.show( new WndMessage( Messages.get( Lab.class, "brief_title" ) + "\n\n"
					+ Messages.get( Lab.class, "brief_ow", GENERATORS ) ) );
		}
		GLog.w( Messages.get( Lab.class, "arrive", l.generatorsOn(), GENERATORS ) );
	}

	/** Leaking vents and a nagging alarm while in the lab. */
	public static class Hazard extends Buff {

		{
			type = buffType.NEGATIVE;
			announced = false;
		}

		@Override
		public boolean act() {
			if (!inLab()){
				detach();
				return true;
			}
			LabLevel l = (LabLevel) Dungeon.level;
			ArrayList<Integer> near = new ArrayList<>();
			for (int v : l.vents()){
				int d = l.distance( v, target.pos );
				if (d >= 2 && d <= 10) near.add( v );
			}
			if (!near.isEmpty()){
				int v = Random.element( near );
				GameScene.add( Blob.seed( v, GAS_AMOUNT, ToxicGas.class ) );
				if (l.heroFOV[v]){
					GLog.w( Messages.get( Lab.class, "gas" ) );
					Sample.INSTANCE.play( Assets.Sounds.GAS );
				}
			}
			spend( GAS_INTERVAL * Random.Float( 0.8f, 1.2f ) );
			return true;
		}

		public void delayStart(){
			spend( GAS_INTERVAL );
		}

		@Override
		public int icon() {
			return BuffIndicator.POISON;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 0.6f, 1f, 0.3f );
		}

		@Override
		public String desc() {
			return Messages.get( this, "desc" );
		}
	}

	//--- generators, lever and alarm -------------------------------------------------------------

	static void callEveryone( int cell ){
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && m.state != m.HUNTING){
				m.beckon( cell );
			}
		}
	}

	/** After the lever: the whole complex hunts the hero until they get out. */
	public static class Lockdown extends Buff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public boolean act() {
			if (!inLab()){
				detach();
				return true;
			}
			callEveryone( target.pos );
			Sample.INSTANCE.play( Assets.Sounds.ALERT, 0.5f, 0.9f );
			spend( 8f );
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.TARGETED;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 1f, 0.6f, 0f );
		}

		@Override
		public String desc() {
			return Messages.get( this, "desc" );
		}
	}

	//--- the end ---------------------------------------------------------------------------------

	//open world: the victory message waits until the hero is back in town
	private static int pendingVictory = 0;

	/** Removes the Lab's air and lockdown from the hero, once they are out of the Lab. */
	public static void clearLabBuffs( Hero hero ){
		if (hero == null) return;
		Buff.detach( hero, Hazard.class );
		Buff.detach( hero, Lockdown.class );
	}

	public static void escape( final Hero hero ){
		clearLabBuffs( hero );
		if (OpenWorld.isActive()){
			//open world: back to town with the loot, the run goes on
			Stash.load();
			Stash.labClears++;
			Stash.save();
			pendingVictory = Stash.labClears;
			OpenWorld.travelToBase();
			return;
		}
		Extraction.extract( hero );
		Stash.labClears++;
		Stash.save();
		final int clears = Stash.labClears;
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				showVictory( clears );
			}
		} );
	}

	private static void showVictory( int clears ){
		GameScene.show( new WndMessage( Messages.get( Lab.class, "victory_title" ) + "\n\n"
				+ Messages.get( Lab.class, OpenWorld.isActive() ? "victory_ow" : "victory", clears ) ) );
	}

	/** Called on arrival anywhere: shows the open world victory once back in town. */
	public static void showPendingVictory(){
		if (pendingVictory > 0 && OpenWorld.onBase()){
			int c = pendingVictory;
			pendingVictory = 0;
			showVictory( c );
		}
	}

	//--- saving ----------------------------------------------------------------------------------

	private static final String NODE   = "efpd_lab";
	private static final String ACTIVE = "active";

	public static void storeInBundle( Bundle bundle ){
		Bundle node = new Bundle();
		node.put( ACTIVE, active );
		bundle.put( NODE, node );
	}

	public static void restoreFromBundle( Bundle bundle ){
		reset();
		if (!bundle.contains( NODE )) return;
		active = bundle.getBundle( NODE ).getBoolean( ACTIVE );
	}


}
