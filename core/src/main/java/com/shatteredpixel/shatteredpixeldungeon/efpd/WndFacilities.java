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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.efpd.Facilities.Facility;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Canteen;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.SupplyPack;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.ChargrilledMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MeatPie;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * Menus for the open world base facilities, opened from the base keeper.
 * Every menu is a list of labelled actions, which keeps it usable on small phone screens.
 */
public class WndFacilities {

	/** A WndOptions whose buttons each run an action. */
	static class Menu extends WndOptions {
		//WndOptions asks enabled() from its own constructor, before this class's fields are set,
		//so the button states are handed over through this static field first
		private static ArrayList<Boolean> pendingEnabled;

		private final ArrayList<Runnable> actions;
		private final ArrayList<Boolean> enabled;

		Menu( String title, String message, ArrayList<String> labels, ArrayList<Runnable> actions, ArrayList<Boolean> enabled ){
			super( title, message, prepare( labels, enabled ) );
			this.actions = actions;
			this.enabled = enabled;
		}

		private static String[] prepare( ArrayList<String> labels, ArrayList<Boolean> enabled ){
			pendingEnabled = enabled;
			return labels.toArray( new String[0] );
		}

		@Override
		protected boolean enabled( int index ) {
			ArrayList<Boolean> e = enabled != null ? enabled : pendingEnabled;
			return e == null || e.get( index );
		}

		@Override
		protected void onSelect( int index ) {
			if (enabled( index )) actions.get( index ).run();
		}
	}

	static class Builder {
		final ArrayList<String> labels = new ArrayList<>();
		final ArrayList<Runnable> actions = new ArrayList<>();
		final ArrayList<Boolean> enabled = new ArrayList<>();

		Builder add( String label, boolean on, Runnable action ){
			labels.add( label ); actions.add( action ); enabled.add( on );
			return this;
		}

		void show( String title, String message ){
			GameScene.show( new Menu( title, message, labels, actions, enabled ) );
		}
	}

	private static String msg( String key, Object... args ){
		return Messages.get(WndFacilities.class, key, args);
	}

	//--- facility list ---------------------------------------------------------------------------

	public static void show(){
		Builder b = new Builder();
		for (final Facility f : Facility.values()){
			int lvl = Facilities.level( f );
			String label = lvl > 0
					? msg( "entry", Facilities.name(f), lvl, Facilities.MAX_LEVEL )
					: msg( "entry_unbuilt", Facilities.name(f) );
			b.add( label, true, new Runnable() { public void run() { showFacility( f ); } } );
		}
		final SupplyPack pack = Dungeon.hero.belongings.getItem( SupplyPack.class );
		b.add( pack != null ? msg( "pack_entry", pack.packLevel(), SupplyPack.MAX_LEVEL ) : msg( "pack_none" ),
				pack != null, new Runnable() { public void run() { showPack( pack ); } } );
		b.add( msg( "back" ), true, new Runnable() { public void run() { GameScene.show( new WndBase() ); } } );
		b.show( msg( "title" ), msg( "intro" ) + "\n\n" + Facilities.haveText() );
	}

	public static void showFacility( final Facility f ){
		final Hero hero = Dungeon.hero;
		int lvl = Facilities.level( f );
		Builder b = new Builder();

		StringBuilder text = new StringBuilder( Messages.get(Facilities.class, "desc_" + f.name().toLowerCase()) );
		text.append( "\n\n" );
		text.append( lvl > 0 ? msg( "level", lvl, Facilities.MAX_LEVEL ) : msg( "unbuilt" ) );

		if (lvl > 0){
			switch (f){
				case BED:
					b.add( msg( "sleep", Facilities.turnsUntilDawn() ), true, new Runnable() { public void run() {
						int turns = Facilities.sleep( hero );
						Sample.INSTANCE.play( Assets.Sounds.LULLABY );
						GLog.p( msg( "slept", turns, DayNight.day() ) );
					}});
					break;
				case KITCHEN:
					addKitchen( b, hero, lvl );
					break;
				case INFIRMARY:
					final int cost = Facilities.treatmentCost( hero );
					b.add( cost > 0 ? msg( "treat", cost ) : msg( "treat_none" ),
							cost > 0 && Facilities.haveGold() >= cost, new Runnable() { public void run() {
						if (Facilities.treat( hero )) GLog.p( msg( "treated" ) );
					}});
					break;
				case FARM:
					b.add( msg( "farm_open", Facilities.plots() ), true, new Runnable() { public void run() { showFarm(); } } );
					break;
				case FORGE:
					addForge( b, lvl );
					break;
			}
		}

		final int[] cost = Facilities.nextCost( f );
		if (cost != null){
			text.append( "\n\n" ).append( msg( lvl > 0 ? "upgrade_cost" : "build_cost", Facilities.costText( cost ) ) );
			text.append( "\n" ).append( msg( "have", Facilities.haveText() ) );
			b.add( msg( lvl > 0 ? "upgrade" : "build" ), Facilities.canAfford( cost ), new Runnable() { public void run() {
				if (Facilities.build( f )){
					Sample.INSTANCE.play( Assets.Sounds.EVOKE );
					GLog.p( msg( "built", Facilities.name(f), Facilities.level(f) ) );
				}
				showFacility( f );
			}});
		} else {
			text.append( "\n\n" ).append( msg( "maxed" ) );
		}

		b.add( msg( "back" ), true, new Runnable() { public void run() { show(); } } );
		b.show( Facilities.name( f ), text.toString() );
	}

	//--- supply pack -------------------------------------------------------------------------------

	public static void showPack( final SupplyPack pack ){
		Builder b = new Builder();
		StringBuilder text = new StringBuilder( msg( "pack_desc" ) );
		text.append( "\n\n" ).append( msg( "pack_level", pack.packLevel(), SupplyPack.MAX_LEVEL,
				pack.capacity(), Math.round( pack.weightBonus() ) ) );
		final int[] cost = pack.nextCost();
		if (cost != null){
			int next = pack.packLevel();
			text.append( "\n\n" ).append( msg( "pack_next", SupplyPack.SLOTS[next], Math.round( SupplyPack.WEIGHT_BONUS[next] ) ) );
			text.append( "\n" ).append( msg( "upgrade_cost", Facilities.costText( cost ) ) );
			text.append( "\n" ).append( msg( "have", Facilities.haveText() ) );
			b.add( msg( "upgrade" ), Facilities.canAfford( cost ), new Runnable() { public void run() {
				if (Facilities.upgradePack( pack )){
					Sample.INSTANCE.play( Assets.Sounds.EVOKE );
					GLog.p( msg( "pack_upgraded", pack.packLevel() ) );
				}
				showPack( pack );
			}});
		} else {
			text.append( "\n\n" ).append( msg( "pack_maxed" ) );
		}
		b.add( msg( "back" ), true, new Runnable() { public void run() { show(); } } );
		b.show( Messages.titleCase( pack.name() ), text.toString() );
	}

	//--- kitchen ---------------------------------------------------------------------------------

	private static int count( Hero hero, Class<?> cls ){
		int n = 0;
		for (Item i : hero.belongings.backpack.items) if (i.getClass() == cls) n += i.quantity();
		return n;
	}

	private static void take( Hero hero, Class<?> cls, int amount ){
		for (Item i : hero.belongings.backpack.items.toArray(new Item[0])){
			if (amount <= 0) return;
			if (i.getClass() == cls){
				int t = Math.min( amount, i.quantity() );
				if (t >= i.quantity()) i.detachAll( hero.belongings.backpack );
				else i.quantity( i.quantity() - t );
				amount -= t;
			}
		}
	}

	private static void give( Item item ){
		if (!item.collect()) Dungeon.level.drop( item, Dungeon.hero.pos ).sprite.drop();
		Item.updateQuickslot();
		Sample.INSTANCE.play( Assets.Sounds.BURNING );
	}

	private static void addKitchen( Builder b, final Hero hero, int lvl ){
		final Canteen canteen = hero.belongings.getItem( Canteen.class );
		b.add( msg( "boil" ), canteen != null && canteen.raw() && canteen.sips() > 0, new Runnable() { public void run() {
			if (canteen.boil()){
				Sample.INSTANCE.play( Assets.Sounds.BURNING );
				GLog.p( Messages.get(Canteen.class, "boiled") );
			}
		}});
		final int raw = count( hero, MysteryMeat.class );
		b.add( msg( "grill", raw ), raw > 0, new Runnable() { public void run() {
			take( hero, MysteryMeat.class, raw );
			give( new ChargrilledMeat().quantity( raw ) );
			GLog.p( msg( "cooked" ) );
		}});
		if (lvl >= 2){
			b.add( msg( "ration" ), count( hero, SmallRation.class ) >= 2, new Runnable() { public void run() {
				take( hero, SmallRation.class, 2 );
				give( new Food() );
				GLog.p( msg( "cooked" ) );
			}});
		}
		if (lvl >= 3){
			final int meat = count( hero, MysteryMeat.class ) + count( hero, ChargrilledMeat.class );
			b.add( msg( "pie" ), meat >= 3 && count( hero, Food.class ) >= 1, new Runnable() { public void run() {
				int fromRaw = Math.min( 3, count( hero, MysteryMeat.class ) );
				take( hero, MysteryMeat.class, fromRaw );
				take( hero, ChargrilledMeat.class, 3 - fromRaw );
				take( hero, Food.class, 1 );
				give( new MeatPie() );
				GLog.p( msg( "cooked" ) );
			}});
		}
	}

	//--- farm ------------------------------------------------------------------------------------

	public static void showFarm(){
		Builder b = new Builder();
		for (int p = 0; p < Facilities.plots(); p++){
			final int plot = p;
			Class<?> seed = Facilities.plotSeed( p );
			if (seed == null){
				b.add( msg( "plot_empty", p+1 ), true, new Runnable() { public void run() { selectSeed( plot ); } } );
			} else {
				String name = Messages.titleCase( ((Item) Reflection.newInstance( seed )).name() );
				int left = Facilities.turnsUntilRipe( p );
				if (left > 0){
					b.add( msg( "plot_growing", p+1, name, left ), false, new Runnable() { public void run() {} } );
				} else {
					b.add( msg( "plot_ripe", p+1, name ), true, new Runnable() { public void run() {
						Item crop = Facilities.harvest( plot );
						if (crop != null) GLog.p( msg( "harvested", Messages.titleCase(crop.name()), Facilities.lastHarvest ) );
						showFarm();
					}});
				}
			}
		}
		b.add( msg( "back" ), true, new Runnable() { public void run() { showFacility( Facility.FARM ); } } );
		b.show( Facilities.name( Facility.FARM ), msg( "farm_desc", Facilities.GROW_TIME ) );
	}

	private static void selectSeed( final int plot ){
		GameScene.selectItem( new WndBag.ItemSelector() {
			@Override public String textPrompt() { return msg( "plant_prompt" ); }
			@Override public Class<? extends com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag> preferredBag() {
				return com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch.class;
			}
			@Override public boolean itemSelectable( Item item ) { return item instanceof Plant.Seed; }
			@Override public void onSelect( Item item ) {
				if (item instanceof Plant.Seed){
					Facilities.plant( plot, (Plant.Seed) item );
					GLog.i( msg( "planted", Messages.titleCase(item.name()) ) );
				}
				showFarm();
			}
		});
	}

	//--- forge -----------------------------------------------------------------------------------

	private static void addForge( Builder b, int lvl ){
		b.add( msg( "uncurse", Allies.facilityGold( Facilities.UNCURSE_COST ) ), Facilities.haveGold() >= Allies.facilityGold( Facilities.UNCURSE_COST ), new Runnable() { public void run() {
			forgeSelect( "uncurse_prompt", 0 );
		}});
		if (lvl >= 2){
			b.add( msg( "identify", Allies.facilityGold( Facilities.IDENTIFY_COST ) ), Facilities.haveGold() >= Allies.facilityGold( Facilities.IDENTIFY_COST ), new Runnable() { public void run() {
				forgeSelect( "identify_prompt", 1 );
			}});
		}
		if (lvl >= 3){
			b.add( msg( "reinforce", Facilities.REINFORCE_MAX ), true, new Runnable() { public void run() {
				forgeSelect( "reinforce_prompt", 2 );
			}});
		}
	}

	private static int reinforceGold( Item item ){
		return Allies.facilityGold( Facilities.REINFORCE_GOLD * (item.level() + 1) );
	}

	private static void forgeSelect( final String prompt, final int mode ){
		GameScene.selectItem( new WndBag.ItemSelector() {
			@Override public String textPrompt() { return msg( prompt ); }
			@Override public boolean itemSelectable( Item item ) {
				switch (mode){
					case 0:  return ScrollOfRemoveCurse.uncursable( item );
					case 1:  return !item.isIdentified();
					default: return item.isUpgradable() && item.levelKnown && item.level() < Facilities.REINFORCE_MAX;
				}
			}
			@Override public void onSelect( final Item item ) {
				if (item == null) return;
				final Hero hero = Dungeon.hero;
				if (mode == 0){
					if (Facilities.spendGold( Allies.facilityGold( Facilities.UNCURSE_COST ) )){
						ScrollOfRemoveCurse.uncurse( hero, item );
						GLog.p( msg( "uncursed", Messages.titleCase(item.name()) ) );
					}
				} else if (mode == 1){
					if (Facilities.spendGold( Allies.facilityGold( Facilities.IDENTIFY_COST ) )){
						item.identify();
						GLog.p( msg( "identified", Messages.titleCase(item.name()) ) );
					}
				} else {
					final int gold = reinforceGold( item );
					final boolean affordable = Facilities.haveGold() >= gold
							&& Facilities.haveMaterial( 3 ) >= Facilities.REINFORCE_METAL;
					GameScene.show( new WndOptions( new ItemSprite( item ), Messages.titleCase( item.name() ),
							msg( "reinforce_confirm", gold, Facilities.REINFORCE_METAL ),
							msg( "confirm" ), msg( "back" ) ){
						@Override protected boolean enabled( int index ) { return index != 0 || affordable; }
						@Override protected void onSelect( int index ) {
							if (index == 0 && Facilities.reinforce( item, gold )){
								GLog.p( msg( "reinforced", Messages.titleCase(item.name()) ) );
							}
						}
					});
				}
				Item.updateQuickslot();
			}
		});
	}

}
