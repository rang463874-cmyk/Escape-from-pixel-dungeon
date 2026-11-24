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
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

/** Gunman: the hooded thief sprite in dark grey (sprites/efpd_gunman.png), firing bullets. */
public class GunmanSprite extends MobSprite {

	public static final String TEXTURE = "sprites/efpd_gunman.png";

	private Animation shoot;

	public GunmanSprite() {
		super();
		texture( TEXTURE );
		TextureFilm film = new TextureFilm( texture, 12, 13 );

		idle = new Animation( 1, true );
		idle.frames( film, 0, 0, 0, 1, 0, 0, 0, 0, 1 );

		run = new Animation( 15, true );
		run.frames( film, 0, 0, 2, 3, 3, 4 );

		die = new Animation( 10, false );
		die.frames( film, 5, 6, 7, 8, 9 );

		attack = new Animation( 12, false );
		attack.frames( film, 10, 11, 12, 0 );

		shoot = attack.clone();

		idle();
	}

	private static class BulletVisual extends Item {
		{ image = ItemSpriteSheet.BULLET; }
	}

	@Override
	public void attack( int cell ) {
		if (!Dungeon.level.adjacent( cell, ch.pos )) {
			Sample.INSTANCE.play( Assets.Sounds.BLAST, 0.5f, Random.Float( 1.6f, 1.9f ) );
			((MissileSprite)parent.recycle( MissileSprite.class )).
					reset( this, cell, new BulletVisual(), new Callback() {
						@Override
						public void call() {
							ch.onAttackComplete();
						}
					} );
			play( shoot );
			turnTo( ch.pos, cell );
		} else {
			super.attack( cell );
		}
	}
}
