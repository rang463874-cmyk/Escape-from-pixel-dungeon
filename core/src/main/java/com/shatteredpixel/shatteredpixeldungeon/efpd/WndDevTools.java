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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.Gun;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.LightAmmo;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.RifleAmmo;
import com.shatteredpixel.shatteredpixeldungeon.efpd.guns.ShotgunShell;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Bandage;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Canteen;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Material;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.RecallCharm;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Essence;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Tonic;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.FirstAidKit;
import com.shatteredpixel.shatteredpixeldungeon.efpd.items.Splint;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Honeypot;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Stylus;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Image;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Developer tool: pick an upgrade level and a quantity at the top, then tap any item
 * in the list below to receive it. Opened from the in-game menu in development builds.
 */
public class WndDevTools extends Window {

	private static final int WIDTH_P = 126;
	private static final int WIDTH_L = 200;
	private static final int HEIGHT  = 180;
	private static final int ROW     = 16;
	private static final int GAP     = 2;

	public static final int MAX_LEVEL    = 30;
	public static final int MAX_QUANTITY = 999;

	//remembered between openings
	private static int level    = 0;
	private static int quantity = 1;

	private RedButton btnLevel;
	private RedButton btnQuantity;

	public WndDevTools(){
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		int height = (int)Math.min( HEIGHT, PixelScene.uiCamera.height - 20 );
		resize( width, height );

		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get(this, "title"), 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (width - title.width())/2f, 0 );
		add( title );
		float pos = title.bottom() + GAP;

		//upgrade level row
		pos = addStepper( Messages.get(this, "level"), width, pos, true );
		//quantity row
		pos = addStepper( Messages.get(this, "quantity"), width, pos, false );

		ScrollingListPane list = new ScrollingListPane();
		add( list );
		list.setRect( 0, pos + GAP, width, height - pos - GAP );
		fillList( list );
	}

	//--- top controls --------------------------------------------------------------------------

	private float addStepper( String label, int width, float pos, final boolean isLevel ){
		RenderedTextBlock txt = PixelScene.renderTextBlock( label, 7 );
		txt.setPos( 0, pos + (ROW - txt.height())/2f );
		add( txt );

		float btnW = ROW + 4;
		float valueW = 34;
		float right = width;

		final int[] steps = isLevel ? new int[]{ -1, +1 } : new int[]{ -10, -1, +1, +10 };
		ArrayList<RedButton> buttons = new ArrayList<>();
		for (final int step : steps){
			RedButton b = new RedButton( (step > 0 ? "+" : "") + step, 6 ){
				@Override
				protected void onClick() {
					change( isLevel, step );
				}
			};
			buttons.add( b );
			add( b );
		}

		RedButton value = new RedButton( "", 7 ){
			@Override
			protected void onClick() {
				typeValue( isLevel );
			}
		};
		add( value );
		if (isLevel) btnLevel = value; else btnQuantity = value;

		//layout from the right: [-..] [value] [+..]
		int half = steps.length / 2;
		float x = right - (steps.length * (btnW + 1)) - valueW - 1;
		for (int i = 0; i < half; i++){
			buttons.get(i).setRect( x, pos, btnW, ROW );
			x += btnW + 1;
		}
		value.setRect( x, pos, valueW, ROW );
		x += valueW + 1;
		for (int i = half; i < steps.length; i++){
			buttons.get(i).setRect( x, pos, btnW, ROW );
			x += btnW + 1;
		}

		refreshValues();
		return pos + ROW + GAP;
	}

	private void change( boolean isLevel, int step ){
		if (isLevel) level = clamp( level + step, 0, MAX_LEVEL );
		else         quantity = clamp( quantity + step, 1, MAX_QUANTITY );
		refreshValues();
	}

	private void typeValue( final boolean isLevel ){
		GameScene.show( new WndTextInput(
				Messages.get(WndDevTools.class, isLevel ? "level" : "quantity"),
				Messages.get(WndDevTools.class, isLevel ? "level_range" : "quantity_range", isLevel ? MAX_LEVEL : MAX_QUANTITY),
				Integer.toString( isLevel ? level : quantity ),
				4, false,
				Messages.get(WndDevTools.class, "ok"),
				Messages.get(WndDevTools.class, "cancel") ){
			@Override
			public void onSelect( boolean positive, String text ) {
				if (!positive) return;
				try {
					int v = Integer.parseInt( text.trim() );
					if (isLevel) level = clamp( v, 0, MAX_LEVEL );
					else         quantity = clamp( v, 1, MAX_QUANTITY );
				} catch (NumberFormatException e){
					GLog.w( Messages.get(WndDevTools.class, "not_number") );
				}
				refreshValues();
			}
		});
	}

	private void refreshValues(){
		if (btnLevel != null)    btnLevel.text( "+" + level );
		if (btnQuantity != null) btnQuantity.text( "x" + quantity );
	}

	private static int clamp( int v, int min, int max ){
		return Math.max( min, Math.min( max, v ) );
	}

	//--- item list -----------------------------------------------------------------------------

	private void fillList( ScrollingListPane list ){
		//currencies
		list.addTitle( Messages.get(this, "cat_currency") );
		list.addItem( new ListEntry( new ItemSprite(ItemSpriteSheet.GOLD), Messages.get(this, "gold") ){
			@Override protected void give() {
				Dungeon.gold += 1000 * quantity;
				Item.updateQuickslot();
				GLog.i( Messages.get(WndDevTools.class, "gave_gold", 1000 * quantity) );
			}
		});
		list.addItem( new ListEntry( new ItemSprite(ItemSpriteSheet.ENERGY), Messages.get(this, "energy") ){
			@Override protected void give() {
				Dungeon.energy += 100 * quantity;
				Item.updateQuickslot();
				GLog.i( Messages.get(WndDevTools.class, "gave_energy", 100 * quantity) );
			}
		});

		//debug actions
		list.addTitle( Messages.get(this, "cat_debug") );
		list.addItem( new ListEntry( new ItemSprite(ItemSpriteSheet.POTION_CRIMSON), Messages.get(this, "heal") ){
			@Override protected void give() {
				Dungeon.hero.HP = Dungeon.hero.HT;
				Injuries injuries = Dungeon.hero.buff( Injuries.class );
				if (injuries != null) injuries.healAll();
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.detach( Dungeon.hero, Hemorrhage.class );
				GLog.p( Messages.get(WndDevTools.class, "healed") );
			}
		});
		list.addItem( new ListEntry( new ItemSprite(ItemSpriteSheet.SPLINT), Messages.get(this, "injure") ){
			@Override protected void give() {
				//one minor and one severe injury on random body parts
				Injuries inj = com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect( Dungeon.hero, Injuries.class );
				Injuries.Part[] parts = Injuries.Part.values();
				inj.injure( parts[com.watabou.utils.Random.Int(parts.length)], Injuries.MINOR );
				inj.injure( parts[com.watabou.utils.Random.Int(parts.length)], Injuries.SEVERE );
				GLog.w( Messages.get(WndDevTools.class, "injured") );
			}
		});
		list.addItem( new ListEntry( new ItemSprite(ItemSpriteSheet.TOMB), Messages.get(this, "die") ){
			@Override protected void give() {
				hide();
				Dungeon.hero.HP = 0;
				Dungeon.hero.die( WndDevTools.class );
			}
		});

		list.addItem( new ListEntry( new ItemSprite(ItemSpriteSheet.ARTIFACT_HOURGLASS), Messages.get(this, "skip_time") ){
			@Override protected void give() {
				//moves the world clock only, buffs and hunger don't advance
				com.shatteredpixel.shatteredpixeldungeon.Statistics.duration += 300 * quantity;
				GLog.i( DayNight.label() );
				Dungeon.observe();
			}
		});
		list.addItem( new ListEntry( new ItemSprite(ItemSpriteSheet.CANTEEN), Messages.get(this, "thirst") ){
			@Override protected void give() {
				Thirst t = Dungeon.hero.buff( Thirst.class );
				if (t == null){
					GLog.w( Messages.get(Tonic.class, "only_open_world") );
					return;
				}
				t.quench( -Thirst.DEHYDRATED );
			}
		});
		list.addItem( new ListEntry( new ItemSprite(ItemSpriteSheet.TONIC_STRENGTH), Messages.get(this, "stat_xp") ){
			@Override protected void give() {
				if (!OpenWorld.isActive()){
					GLog.w( Messages.get(Tonic.class, "only_open_world") );
					return;
				}
				for (PermStats.Stat s : PermStats.Stat.values()) PermStats.gain( s, 100 * quantity );
			}
		});

		//melee weapons and guns, by tier
		ArrayList<Class<?>> guns = new ArrayList<>();
		for (int t = 0; t < Generator.wepTiers.length; t++){
			ArrayList<Class<?>> melee = new ArrayList<>();
			for (Class<?> c : Generator.wepTiers[t].classes){
				if (Gun.class.isAssignableFrom(c)) guns.add(c); else melee.add(c);
			}
			addCategory( list, Messages.get(this, "cat_melee", t+1), melee );
		}
		addCategory( list, Messages.get(this, "cat_guns"), guns );
		addCategory( list, Messages.get(this, "cat_ammo"), Arrays.asList( LightAmmo.class, RifleAmmo.class, ShotgunShell.class ) );

		ArrayList<Class<?>> missiles = new ArrayList<>();
		for (Generator.Category c : Generator.misTiers) missiles.addAll( Arrays.asList(c.classes) );
		addCategory( list, Messages.get(this, "cat_missile"), missiles );

		ArrayList<Class<?>> armor = new ArrayList<>();
		for (Class<?> c : Generator.Category.ARMOR.classes){
			//class armor needs a hero ability and comes from the crown, skip it
			if (!ClassArmor.class.isAssignableFrom(c)) armor.add(c);
		}
		addCategory( list, Messages.get(this, "cat_armor"), armor );

		addCategory( list, Messages.get(this, "cat_wand"),     Arrays.asList( Generator.Category.WAND.classes ) );
		addCategory( list, Messages.get(this, "cat_ring"),     Arrays.asList( Generator.Category.RING.classes ) );
		addCategory( list, Messages.get(this, "cat_artifact"), Arrays.asList( Generator.Category.ARTIFACT.classes ) );
		addCategory( list, Messages.get(this, "cat_trinket"),  Arrays.asList( Generator.Category.TRINKET.classes ) );
		addCategory( list, Messages.get(this, "cat_potion"),   Arrays.asList( Generator.Category.POTION.classes ) );
		addCategory( list, Messages.get(this, "cat_exotic_potion"), new ArrayList<Class<?>>( ExoticPotion.regToExo.values() ) );
		addCategory( list, Messages.get(this, "cat_scroll"),   Arrays.asList( Generator.Category.SCROLL.classes ) );
		addCategory( list, Messages.get(this, "cat_exotic_scroll"), new ArrayList<Class<?>>( ExoticScroll.regToExo.values() ) );
		addCategory( list, Messages.get(this, "cat_stone"),    Arrays.asList( Generator.Category.STONE.classes ) );
		addCategory( list, Messages.get(this, "cat_seed"),     Arrays.asList( Generator.Category.SEED.classes ) );
		addCategory( list, Messages.get(this, "cat_food"),     Arrays.asList( Generator.Category.FOOD.classes ) );
		addCategory( list, Messages.get(this, "cat_bomb"),     Arrays.asList( Bomb.class, Firebomb.class, FrostBomb.class,
				FlashBangBomb.class, HolyBomb.class, Noisemaker.class, RegrowthBomb.class, ShrapnelBomb.class,
				SmokeBomb.class, WoollyBomb.class, ArcaneBomb.class ) );
		addCategory( list, Messages.get(this, "cat_openworld"), Arrays.asList( Essence.Sewers.class, Essence.Prison.class,
				Essence.Caves.class, Essence.City.class, Essence.Halls.class, Tonic.Strength.class, Tonic.Vitality.class,
				Tonic.Agility.class, Tonic.Skill.class, Tonic.Endurance.class, Tonic.Resilience.class, Canteen.class, Material.Cloth.class, Material.Wood.class,
				Material.StoneBlock.class, Material.RefinedMetal.class, RecallCharm.class ) );
		addCategory( list, Messages.get(this, "cat_medical"),  Arrays.asList( Bandage.class, Splint.class, FirstAidKit.class ) );
		addCategory( list, Messages.get(this, "cat_misc"),     Arrays.asList( Ankh.class, Honeypot.class, Torch.class,
				Stylus.class, ArcaneResin.class ) );
	}

	private void addCategory( ScrollingListPane list, String title, java.util.List<? extends Class<?>> classes ){
		list.addTitle( title );
		for (final Class<?> c : classes){
			final Item sample = (Item) Reflection.newInstance( c );
			if (sample == null) continue;
			list.addItem( new ListEntry( new ItemSprite(sample), Messages.titleCase(sample.name()) ){
				@Override protected void give() {
					giveItem( c );
				}
			});
		}
	}

	private static void giveItem( Class<?> cls ){
		Item item = (Item) Reflection.newInstance( cls );
		if (item == null) return;

		String name = Messages.titleCase( item.name() );
		if (item.stackable){
			item.quantity( quantity );
			grant( prepare( item ) );
		} else {
			for (int i = 0; i < quantity; i++){
				Item copy = i == 0 ? item : (Item) Reflection.newInstance( cls );
				grant( prepare( copy ) );
			}
		}
		//the item may have merged into an existing stack, so report the requested amount
		GLog.i( Messages.get(WndDevTools.class, "gave", name, quantity) );
	}

	private static Item prepare( Item item ){
		if (item instanceof Gun) ((Gun) item).fill();
		if (item instanceof Canteen) ((Canteen) item).fill();
		//the upgrade level picks the tonic grade
		if (item instanceof Tonic) ((Tonic) item).grade( Math.max(1, level) );
		//artifacts level up through use, not upgrades
		if (level > 0 && item.isUpgradable() && !(item instanceof Artifact)){
			item.upgrade( level );
		}
		item.identify();
		return item;
	}

	private static void grant( Item item ){
		if (!item.collect()){
			Dungeon.level.drop( item, Dungeon.hero.pos ).sprite.drop();
		}
	}

	private static abstract class ListEntry extends ScrollingListPane.ListItem {

		public ListEntry( Image icon, String text ){
			super( icon, text );
		}

		protected abstract void give();

		@Override
		public boolean onClick( float x, float y ) {
			if (inside( x, y )){
				give();
				return true;
			}
			return false;
		}
	}

}
