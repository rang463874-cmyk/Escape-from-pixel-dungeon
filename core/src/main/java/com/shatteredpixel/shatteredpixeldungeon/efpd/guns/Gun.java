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

package com.shatteredpixel.shatteredpixeldungeon.efpd.guns;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Firearms. A gun is a melee weapon (a weak bash) that can also SHOOT bullets from its magazine
 * and RELOAD from ammo in the backpack. Guns are loud: firing alerts nearby enemies.
 *
 * Each gun only sets the stats in its initializer block, see the subclasses in this package.
 * Guns drop from the regular weapon tiers (see Generator), so they follow the same floor tiers
 * as melee weapons.
 */
public abstract class Gun extends MeleeWeapon {

	public static final String AC_SHOOT  = "SHOOT";
	public static final String AC_RELOAD = "RELOAD";

	//--- stats, set by each gun ----------------------------------------------------------------
	protected Ammo.Type ammoType = Ammo.Type.LIGHT;
	protected int   magSize      = 6;
	protected float damageMulti  = 1f;  //multiplies the base bullet damage of the tier
	protected float shotDelay    = 1f;  //turns per shot
	protected float reloadTime   = 2f;  //turns to reload
	protected float closeAcc     = 1f;  //accuracy multiplier when adjacent
	protected float farAcc       = 1f;  //accuracy multiplier at FAR_RANGE tiles or more
	protected boolean falloff    = false; //shotguns: damage drops with distance
	protected int   noise        = 8;   //radius in tiles in which enemies hear the shot
	protected float weight       = 2f;
	protected int   extraStr     = 0;   //strength needed above the tier's usual requirement

	public static final int FAR_RANGE = 6;
	//-------------------------------------------------------------------------------------------

	protected int loaded = 0;

	{
		defaultAction = AC_SHOOT;
		usesTargeting = true;
		bones = true;
	}

	public float weight(){
		return weight;
	}

	public Ammo.Type ammoType(){
		return ammoType;
	}

	public int magSize(){
		return magSize;
	}

	public int loaded(){
		return loaded;
	}

	//new guns come with a full magazine
	@Override
	public Item random() {
		super.random();
		loaded = magSize;
		return this;
	}

	public Gun fill(){
		loaded = magSize;
		return this;
	}

	//--- melee: guns are poor clubs --------------------------------------------------------------

	@Override
	public int min(int lvl) {
		return tier + lvl;
	}

	@Override
	public int max(int lvl) {
		return 2*tier + 2 + lvl;
	}

	@Override
	public int STRReq(int lvl) {
		return super.STRReq(lvl) + extraStr;
	}

	//--- shooting ------------------------------------------------------------------------------

	public int bulletMin(int lvl){
		return Math.round( damageMulti * (tier + 1 + lvl) );
	}

	public int bulletMax(int lvl){
		return Math.round( damageMulti * (4*(tier + 1) + lvl*(tier + 1)) );
	}

	protected float accuracyAt( int distance ){
		if (distance <= 1) return closeAcc;
		if (distance >= FAR_RANGE) return farAcc;
		float t = (distance - 1) / (float)(FAR_RANGE - 1);
		return closeAcc + (farAcc - closeAcc) * t;
	}

	protected float falloffAt( int distance ){
		if (!falloff || distance <= 2) return 1f;
		return Math.max( 0.5f, 1f - 0.15f*(distance - 2) );
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_SHOOT );
		actions.add( AC_RELOAD );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );

		if (action.equals( AC_SHOOT )) {
			if (!isEquipped( hero )){
				GLog.w( Messages.get(Gun.class, "not_equipped") );
			} else {
				//aiming works with an empty magazine too: tapping yourself reloads
				curUser = hero;
				curItem = this;
				GameScene.selectCell( shooter );
			}

		} else if (action.equals( AC_RELOAD )) {
			reload( hero );
		}
	}

	public void reload( Hero hero ){
		if (loaded >= magSize){
			GLog.i( Messages.get(Gun.class, "full") );
			return;
		}
		Ammo ammo = Ammo.find( hero, ammoType );
		if (ammo == null){
			GLog.w( Messages.get(Gun.class, "no_ammo", Messages.get(Ammo.class, ammoType.name().toLowerCase())) );
			return;
		}

		int needed = magSize - loaded;
		int taken = Math.min( needed, ammo.quantity() );
		if (taken >= ammo.quantity()){
			ammo.detachAll( hero.belongings.backpack );
		} else {
			ammo.quantity( ammo.quantity() - taken );
		}
		loaded += taken;
		updateQuickslot();

		Sample.INSTANCE.play( Assets.Sounds.UNLOCK, 1f, 1.3f );
		GLog.i( Messages.get(Gun.class, "reloaded", loaded, magSize) );
		hero.sprite.operate( hero.pos );
		hero.spendAndNext( reloadTime );
	}

	//every shot is heard by enemies nearby
	protected void makeNoise( Char user ){
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.distance( user.pos, m.pos ) <= noise){
				m.beckon( user.pos );
			}
		}
	}

	@Override
	public int targetingPos( Hero user, int dst ) {
		return new Bullet().targetingPos( user, dst );
	}

	@Override
	public String status() {
		return loaded + "/" + magSize;
	}

	private final CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ) {
			if (target == null) return;
			//tapping yourself while aiming reloads
			if (target == curUser.pos) {
				reload( curUser );
			} else if (loaded <= 0) {
				GLog.w( Messages.get(Gun.class, "empty") );
			} else {
				new Bullet().cast( curUser, target );
			}
		}
		@Override
		public String prompt() {
			return Messages.get(Gun.class, "prompt");
		}
	};

	/** The projectile fired by a gun. It is never dropped or picked up. */
	public class Bullet extends MissileWeapon {

		{
			image = ItemSpriteSheet.BULLET;
			hitSound = Assets.Sounds.HIT_ARROW;
			setID = 0;
		}

		//distance to the target, for shotgun damage falloff
		private int distance = 1;

		@Override
		public ArrayList<String> actions( Hero hero ) {
			return new ArrayList<>();
		}

		@Override
		public String defaultAction() {
			return null;
		}

		@Override
		public int defaultQuantity() {
			return 1;
		}

		@Override
		public int damageRoll( Char owner ) {
			int lvl = Gun.this.buffedLvl();
			int dmg = owner instanceof Hero
					? Hero.heroDamageIntRange( bulletMin(lvl), bulletMax(lvl) )
					: Random.NormalIntRange( bulletMin(lvl), bulletMax(lvl) );
			dmg = Gun.this.augment.damageFactor( dmg );
			if (owner instanceof Hero){
				int exStr = ((Hero) owner).STR() - Gun.this.STRReq();
				if (exStr > 0) dmg += Hero.heroDamageIntRange( 0, exStr );
			}
			return Math.round( dmg * falloffAt( distance ) );
		}

		@Override
		public float accuracyFactor( Char owner, Char target ) {
			//uses the gun's own accuracy rules instead of thrown weapon rules
			return Gun.this.accuracyFactor( owner, target )
					* accuracyAt( Dungeon.level.distance( owner.pos, target.pos ) );
		}

		@Override
		public float delayFactor( Char user ) {
			return shotDelay * Gun.this.delayFactor( user );
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			return Gun.this.proc( attacker, defender, damage );
		}

		@Override
		public int STRReq( int lvl ) {
			return Gun.this.STRReq();
		}

		@Override
		protected void onThrow( int cell ) {
			Char enemy = Actor.findChar( cell );
			distance = Dungeon.level.distance( curUser.pos, cell );
			if (enemy == null || enemy == curUser) {
				parent = null;
				Splash.at( cell, 0xCCAAAAAA, 1 );
			} else if (!curUser.shoot( enemy, this )) {
				Splash.at( cell, 0xCCAAAAAA, 1 );
			}
		}

		@Override
		public void cast( Hero user, int dst ) {
			loaded--;
			Gun.this.updateQuickslot();
			makeNoise( user );
			super.cast( user, dst );
		}

		@Override
		public Item split( int amount ) {
			return null;
		}

		@Override
		public void throwSound() {
			Sample.INSTANCE.play( Assets.Sounds.BLAST, 0.6f, Random.Float(1.6f, 1.9f) );
		}
	}

	//--- info ----------------------------------------------------------------------------------

	@Override
	public String info() {
		String info = super.info();
		int lvl = levelKnown ? buffedLvl() : 0;
		info += "\n\n" + Messages.get(Gun.class, "stats",
				augment.damageFactor(bulletMin(lvl)), augment.damageFactor(bulletMax(lvl)),
				Messages.get(Ammo.class, ammoType.name().toLowerCase()),
				loaded, magSize,
				new java.text.DecimalFormat("#.##").format(shotDelay),
				new java.text.DecimalFormat("#.##").format(reloadTime),
				noise);
		if (closeAcc > farAcc + 0.2f)      info += " " + Messages.get(Gun.class, "close_range");
		else if (farAcc > closeAcc + 0.2f) info += " " + Messages.get(Gun.class, "long_range");
		if (falloff)                       info += " " + Messages.get(Gun.class, "falloff");
		return info;
	}

	@Override
	public String abilityInfo() {
		return Messages.get(Gun.class, "ability_desc");
	}

	private static final String LOADED = "loaded";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LOADED, loaded );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		loaded = bundle.getInt( LOADED );
	}

}
