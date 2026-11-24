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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.efpd.Injuries.Part;
import com.watabou.utils.Random;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.shatteredpixel.shatteredpixeldungeon.efpd.Injuries.Part.*;

/**
 * Which body parts each enemy injures. When an enemy hits, one part is picked at random
 * from its list (repeat a part to make it more likely).
 *
 * To support a new enemy, add one line to the table below. Entries are checked in order
 * and subclasses match their parent's entry (e.g. Albino matches Rat), so put a subclass
 * above its parent if it should behave differently. Enemies not in the table fall back
 * to a guess based on their size.
 */
public class InjuryTable {

	private static final Map<Class<? extends Char>, Part[]> TABLE = new LinkedHashMap<>();

	private static void add( Class<? extends Char> enemy, Part... parts ){
		TABLE.put( enemy, parts );
	}

	static {
		//sewers: small vermin go for the lower body
		add( Rat.class,             LEGS );
		add( Snake.class,           FEET );
		add( Crab.class,            LEGS, FEET );
		add( Slime.class,           LEGS, FEET );
		add( Swarm.class,           HEAD, ARMS );
		add( GnollTrickster.class,  ARMS, TORSO );
		add( Gnoll.class,           TORSO );
		add( Goo.class,             TORSO, LEGS );

		//prison
		add( Skeleton.class,        TORSO, ARMS );
		add( Thief.class,           ARMS );
		add( DM100.class,           TORSO );
		add( Guard.class,           TORSO, LEGS );
		add( Necromancer.class,     TORSO );
		add( Tengu.class,           TORSO, ARMS, HEAD );

		//caves
		add( Bat.class,             HEAD, ARMS );
		add( Brute.class,           TORSO, HEAD );
		add( Shaman.class,          TORSO );
		add( Spinner.class,         LEGS, FEET );
		add( DM200.class,           TORSO, LEGS );
		add( GnollGuard.class,      TORSO, LEGS );
		add( GnollSapper.class,     TORSO, ARMS );
		add( GnollGeomancer.class,  TORSO, HEAD );
		add( DM300.class,           TORSO, LEGS );

		//city
		add( Ghoul.class,           ARMS, TORSO );
		add( Elemental.class,       ARMS, TORSO );
		add( Warlock.class,         TORSO );
		add( Monk.class,            HEAD, TORSO );
		add( Golem.class,           TORSO, HEAD );
		add( Succubus.class,        TORSO );
		add( DwarfKing.class,       TORSO, HEAD );

		//halls
		add( Eye.class,             HEAD );
		add( Scorpio.class,         TORSO, LEGS );
		add( RipperDemon.class,     TORSO, ARMS );
		add( YogFist.class,         TORSO );

		//special
		add( Piranha.class,         LEGS, FEET );
		add( Mimic.class,           ARMS );
		add( Statue.class,          TORSO, ARMS );
		add( Wraith.class,          TORSO );
		add( Bee.class,             ARMS, HEAD );
	}

	public static Part partFor( Char enemy ){
		for (Map.Entry<Class<? extends Char>, Part[]> e : TABLE.entrySet()){
			if (e.getKey().isInstance(enemy)){
				return Random.element( e.getValue() );
			}
		}
		//unknown enemies: big ones hit high, everything else hits anywhere
		if (enemy.properties().contains(Char.Property.LARGE)){
			return Random.element( new Part[]{ TORSO, HEAD } );
		}
		return Random.element( new Part[]{ TORSO, ARMS, LEGS } );
	}

}
