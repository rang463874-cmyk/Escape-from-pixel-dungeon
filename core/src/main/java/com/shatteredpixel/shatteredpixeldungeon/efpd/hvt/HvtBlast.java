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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A thrown area attack of a high value target that lands after a short fuse. The cells it will
 * hit are marked when it lands, so the hero has a couple of turns to get clear.
 *  - grenade (warlord): a bomb blast
 *  - mana burst (warlock): dark magic damage and weakness
 *  - firebomb (butcher): sets the area on fire
 */
public class HvtBlast extends Actor {

	public static final int FUSE = 2; //turns before it goes off

	{
		actPriority = BUFF_PRIO;
	}

	private int center;
	private int kind;
	private int fuse;
	private Char source;

	/**
	 * Throws the attack at a cell. Returns true if an animation plays, in which case the
	 * callback runs when it has landed.
	 */
	public static boolean launch( final Char src, final int cell, final int kind, final Callback after ){
		boolean visible = src.sprite != null && (src.sprite.visible || Dungeon.level.heroFOV[cell]);
		if (!visible){
			arm( src, cell, kind );
			return false;
		}
		Callback land = new Callback() {
			@Override
			public void call() {
				arm( src, cell, kind );
				after.call();
			}
		};
		src.sprite.turnTo( src.pos, cell );
		if (kind == Hvt.WARLORD){
			Sample.INSTANCE.play( Assets.Sounds.MISS, 1f, 0.8f );
			((MissileSprite) src.sprite.parent.recycle( MissileSprite.class )).
					reset( src.sprite, cell, new Bomb(), land );
		} else {
			Sample.INSTANCE.play( Assets.Sounds.ZAP );
			MagicMissile.boltFromChar( src.sprite.parent,
					kind == Hvt.WARLOCK ? MagicMissile.SHADOW : MagicMissile.FIRE,
					src.sprite, cell, land );
		}
		return true;
	}

	private static void arm( Char src, int cell, int kind ){
		HvtBlast b = new HvtBlast();
		b.center = cell;
		b.kind = kind;
		b.fuse = FUSE;
		b.source = src;
		boolean seen = false;
		for (int c : b.cells()){
			if (Dungeon.level.heroFOV[c]){
				GameScene.targetedCell( c, FUSE );
				seen = true;
			}
		}
		if (seen){
			GLog.w( Messages.get( HvtBlast.class, "warn_" + kind ) );
			Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 0.6f, 1.3f );
		}
		Actor.addDelayed( b, 1f );
	}

	private ArrayList<Integer> cells(){
		ArrayList<Integer> out = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS9){
			int c = center + n;
			if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]) out.add( c );
		}
		return out;
	}

	@Override
	protected boolean act() {
		fuse--;
		if (fuse > 0){
			spend( TICK );
			return true;
		}
		explode();
		Actor.remove( this );
		return true;
	}

	private void explode(){
		int z = Urban.zoneIndex( Dungeon.depth );
		switch (kind){
			case Hvt.WARLORD:
				new Bomb().explode( center );
				break;

			case Hvt.WARLOCK:
				Sample.INSTANCE.play( Assets.Sounds.BLAST, 1f, 0.7f );
				for (int c : cells()){
					if (Dungeon.level.heroFOV[c]){
						CellEmitter.get( c ).burst( ShadowParticle.UP, 8 );
					}
					Char ch = Actor.findChar( c );
					if (ch == null || ch == source || !ch.isAlive()) continue;
					int dmg = Random.NormalIntRange( 4 + 3*z, 9 + 5*z );
					ch.damage( dmg, source != null ? source : this );
					if (ch.isAlive()){
						Buff.prolong( ch, Weakness.class, 4f );
					} else if (ch == Dungeon.hero){
						Dungeon.fail( HvtBoss.class );
						GLog.n( Messages.get( HvtBlast.class, "killed" ) );
					}
				}
				break;

			default:
				Sample.INSTANCE.play( Assets.Sounds.BURNING );
				for (int c : cells()){
					if (!Dungeon.level.water[c]){
						GameScene.add( Blob.seed( c, 4, Fire.class ) );
					}
				}
				break;
		}
	}

}
