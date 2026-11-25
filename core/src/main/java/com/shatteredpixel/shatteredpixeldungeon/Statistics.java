/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon;

import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;

import java.util.Arrays;
import java.util.HashSet;

public class Statistics {

	public static int goldCollected;
	public static int deepestFloor;
	public static int highestAscent;
	public static int enemiesSlain;
	public static int foodEaten;
	public static int itemsCrafted;
	public static int piranhasKilled;
	public static int hazardAssistedKills;
	public static int ankhsUsed;
	//tracks every item type 'seen' this run (i.e. would be added to catalogs)
	public static HashSet<Class> itemTypesDiscovered = new HashSet<>();

	//These are used for score calculation
	// some are built incrementally, most are assigned when full score is calculated
	public static int progressScore;
	public static int heldItemValue;
	public static int treasureScore;
	public static SparseArray<Float> floorsExplored = new SparseArray<>();
	public static int exploreScore;
	public static int[] bossScores = new int[5];
	//EFPD: bitmask of boss floors (bit = depth) whose boss has been slain this run
	public static int efpdBossesCleared;
	//EFPD: true after extracting, while the hero manages loot in the hideout
	public static boolean efpdInHideout;
	//EFPD open world mode
	public static boolean efpdOpenWorld;
	public static int efpdCorpseId;          //id of the current corpse bag, older bags turn to dust
	public static int efpdCorpseDepth = -1;  //where the current corpse bag lies, -1 if none
	public static int efpdCorpseBranch;
	public static int efpdDeaths;
	public static boolean efpdSurvived;      //the amulet was brought back
	public static int efpdSurvivedTurn;
	public static boolean efpdUrban;         //urban warfare mode
	public static int efpdBossKills;         //every boss kill, respawned bosses included
	public static int totalBossScore;
	public static int[] questScores = new int[5];
	public static int totalQuestScore;
	public static float winMultiplier;
	public static float chalMultiplier;
	public static int totalScore;

	//used for hero unlock badges
	public static int upgradesUsed;
	public static int sneakAttacks;
	public static int thrownAttacks;

	public static int spawnersAlive;
	
	public static float duration;
	
	public static boolean qualifiedForNoKilling = false;
	public static boolean completedWithNoKilling = false;
	public static boolean qualifiedForBossRemainsBadge = false;
	public static boolean qualifiedForBossChallengeBadge = false;
	public static boolean qualifiedForRandomVictoryBadge = false;

	public static boolean vaultInjureWarned = false;
	
	public static boolean amuletObtained = false;
	public static boolean gameWon = false;
	public static boolean ascended = false;
	
	public static void reset() {
		
		goldCollected	= 0;
		deepestFloor	= 0;
		highestAscent	= 0;
		enemiesSlain	= 0;
		foodEaten		= 0;
		itemsCrafted    = 0;
		piranhasKilled	= 0;
		hazardAssistedKills = 0;
		ankhsUsed		= 0;
		itemTypesDiscovered.clear();

		progressScore   = 0;
		heldItemValue   = 0;
		treasureScore   = 0;
		floorsExplored  = new SparseArray<>();
		exploreScore    = 0;
		bossScores      = new int[5];
		efpdBossesCleared = 0;
		efpdInHideout = false;
		efpdOpenWorld = false;
		efpdCorpseId = 0;
		efpdCorpseDepth = -1;
		efpdCorpseBranch = 0;
		efpdDeaths = 0;
		efpdSurvived = false;
		efpdSurvivedTurn = 0;
		efpdBossKills = 0;
		efpdUrban = false;
		com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.reset();
		com.shatteredpixel.shatteredpixeldungeon.efpd.hvt.Hvt.reset();
		com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Lab.reset();
		com.shatteredpixel.shatteredpixeldungeon.efpd.RingSage.reset();
		com.shatteredpixel.shatteredpixeldungeon.efpd.TownServices.reset();
		com.shatteredpixel.shatteredpixeldungeon.efpd.PermStats.reset();
		com.shatteredpixel.shatteredpixeldungeon.efpd.Facilities.reset();
		com.shatteredpixel.shatteredpixeldungeon.efpd.Regen.reset();
		com.shatteredpixel.shatteredpixeldungeon.efpd.Allies.reset();
		totalBossScore  = 0;
		questScores     = new int[5];
		totalQuestScore = 0;
		winMultiplier   = 1;
		chalMultiplier  = 1;
		totalScore      = 0;
		
		upgradesUsed    = 0;
		sneakAttacks    = 0;
		thrownAttacks   = 0;

		spawnersAlive   = 0;
		
		duration	    = 0;
		
		qualifiedForNoKilling = false;
		qualifiedForBossRemainsBadge = false;
		qualifiedForBossChallengeBadge = false;
		qualifiedForRandomVictoryBadge = GamesInProgress.randomizedClass;

		vaultInjureWarned = false;
		
		amuletObtained = false;
		gameWon = false;
		ascended = false;
		
	}
	
	private static final String GOLD		= "score";
	private static final String DEEPEST		= "maxDepth";
	private static final String HIGHEST		= "maxAscent";
	private static final String SLAIN		= "enemiesSlain";
	private static final String FOOD		= "foodEaten";
	private static final String ALCHEMY		= "potionsCooked";
	private static final String PIRANHAS	= "priranhas";
	private static final String HAZARD_ASSISTS	= "hazard_assists";
	private static final String ANKHS		= "ankhsUsed";

	private static final String PROG_SCORE	    = "prog_score";
	private static final String ITEM_VAL	    = "item_val";
	private static final String TRES_SCORE      = "tres_score";
	private static final String FLR_EXPL        = "flr_expl_";
	private static final String EXPL_SCORE      = "expl_score";
	private static final String BOSS_SCORES		= "boss_scores";
	private static final String EFPD_BOSSES		= "efpd_bosses_cleared";
	private static final String EFPD_HIDEOUT	= "efpd_in_hideout";
	private static final String EFPD_OPENWORLD	= "efpd_open_world";
	private static final String EFPD_CORPSE_ID	= "efpd_corpse_id";
	private static final String EFPD_CORPSE_D	= "efpd_corpse_depth";
	private static final String EFPD_CORPSE_B	= "efpd_corpse_branch";
	private static final String EFPD_DEATHS		= "efpd_deaths";
	private static final String EFPD_SURVIVED	= "efpd_survived";
	private static final String EFPD_SURV_TURN	= "efpd_survived_turn";
	private static final String EFPD_BOSS_KILLS	= "efpd_boss_kills";
	private static final String EFPD_URBAN		= "efpd_urban";
	private static final String TOT_BOSS		= "tot_boss";
	private static final String QUEST_SCORES	= "quest_scores";
	private static final String TOT_QUEST		= "tot_quest";
	private static final String WIN_MULT		= "win_mult";
	private static final String CHAL_MULT		= "chal_mult";
	private static final String TOTAL_SCORE		= "total_score";
	
	private static final String UPGRADES	= "upgradesUsed";
	private static final String SNEAKS		= "sneakAttacks";
	private static final String THROWN		= "thrownAssists";

	private static final String ITEM_TYPES_DISCOVERED    = "item_types_discovered";

	private static final String SPAWNERS	= "spawnersAlive";
	
	private static final String DURATION	= "duration";

	private static final String NO_KILLING_QUALIFIED	= "qualifiedForNoKilling";
	private static final String BOSS_REMAINS_QUALIFIED	= "qualifiedForBossRemainsBadge";
	private static final String BOSS_CHALLENGE_QUALIFIED= "qualifiedForBossChallengeBadge";
	private static final String RANDOM_VICTORY_QUALIFIED= "qualifiedForRandomVictory";
	
	private static final String AMULET          = "amuletObtained";
	private static final String WON		        = "won";
	private static final String ASCENDED		= "ascended";
	
	public static void storeInBundle( Bundle bundle ) {
		bundle.put( GOLD,		goldCollected );
		bundle.put( DEEPEST,	deepestFloor );
		bundle.put( HIGHEST,	highestAscent );
		bundle.put( SLAIN,		enemiesSlain );
		bundle.put( FOOD,		foodEaten );
		bundle.put( ALCHEMY,    itemsCrafted );
		bundle.put( PIRANHAS,	piranhasKilled );
		bundle.put(HAZARD_ASSISTS, hazardAssistedKills);
		bundle.put( ANKHS,		ankhsUsed );
		bundle.put( ITEM_TYPES_DISCOVERED, itemTypesDiscovered.toArray(new Class<?>[0]) );

		bundle.put( PROG_SCORE,  progressScore );
		bundle.put( ITEM_VAL,    heldItemValue );
		bundle.put( TRES_SCORE,  treasureScore );
		for (int i = 1; i < 26; i++){
			if (floorsExplored.containsKey(i)){
				bundle.put( FLR_EXPL+i, floorsExplored.get(i) );
			}
		}
		bundle.put( EXPL_SCORE,  exploreScore );
		bundle.put( BOSS_SCORES, bossScores );
		bundle.put( EFPD_BOSSES, efpdBossesCleared );
		bundle.put( EFPD_HIDEOUT, efpdInHideout );
		bundle.put( EFPD_OPENWORLD, efpdOpenWorld );
		bundle.put( EFPD_CORPSE_ID, efpdCorpseId );
		bundle.put( EFPD_CORPSE_D, efpdCorpseDepth );
		bundle.put( EFPD_CORPSE_B, efpdCorpseBranch );
		bundle.put( EFPD_DEATHS, efpdDeaths );
		bundle.put( EFPD_SURVIVED, efpdSurvived );
		bundle.put( EFPD_SURV_TURN, efpdSurvivedTurn );
		bundle.put( EFPD_BOSS_KILLS, efpdBossKills );
		bundle.put( EFPD_URBAN, efpdUrban );
		com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.storeInBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.hvt.Hvt.storeInBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Lab.storeInBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.RingSage.storeInBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.TownServices.storeInBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.PermStats.storeInBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.Facilities.storeInBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.Regen.storeInBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.Allies.storeInBundle( bundle );
		bundle.put( TOT_BOSS,    totalBossScore );
		bundle.put( QUEST_SCORES,questScores );
		bundle.put( TOT_QUEST,   totalQuestScore );
		bundle.put( WIN_MULT,    winMultiplier );
		bundle.put( CHAL_MULT,   chalMultiplier );
		bundle.put( TOTAL_SCORE, totalScore );
		
		bundle.put( UPGRADES,   upgradesUsed );
		bundle.put( SNEAKS,		sneakAttacks );
		bundle.put( THROWN,     thrownAttacks);

		bundle.put( SPAWNERS,	spawnersAlive );
		
		bundle.put( DURATION,	duration );

		bundle.put(NO_KILLING_QUALIFIED, qualifiedForNoKilling);
		bundle.put(BOSS_REMAINS_QUALIFIED, qualifiedForBossRemainsBadge);
		bundle.put(BOSS_CHALLENGE_QUALIFIED, qualifiedForBossChallengeBadge);
		bundle.put(RANDOM_VICTORY_QUALIFIED, qualifiedForRandomVictoryBadge);
		
		bundle.put( AMULET,		amuletObtained );
		bundle.put( WON,        gameWon );
		bundle.put( ASCENDED,   ascended );
	}
	
	public static void restoreFromBundle( Bundle bundle ) {
		goldCollected	= bundle.getInt( GOLD );
		deepestFloor	= bundle.getInt( DEEPEST );
		highestAscent   = bundle.getInt( HIGHEST );
		enemiesSlain	= bundle.getInt( SLAIN );
		foodEaten		= bundle.getInt( FOOD );
		itemsCrafted    = bundle.getInt( ALCHEMY );
		piranhasKilled	= bundle.getInt( PIRANHAS );
		hazardAssistedKills = bundle.getInt( HAZARD_ASSISTS );
		ankhsUsed		= bundle.getInt( ANKHS );

		if (bundle.contains( ITEM_TYPES_DISCOVERED )) {
			itemTypesDiscovered = new HashSet<>(Arrays.asList(bundle.getClassArray(ITEM_TYPES_DISCOVERED)));
		} else {
			itemTypesDiscovered.clear();
		}

		progressScore   = bundle.getInt( PROG_SCORE );
		heldItemValue   = bundle.getInt( ITEM_VAL );
		treasureScore   = bundle.getInt( TRES_SCORE );
		floorsExplored.clear();
		for (int i = 1; i < 26; i++){
			if (bundle.contains( FLR_EXPL+i )) {
				floorsExplored.put(i, bundle.getFloat(FLR_EXPL + i));
			}
		}
		exploreScore    = bundle.getInt( EXPL_SCORE );
		if (bundle.contains( BOSS_SCORES )) bossScores = bundle.getIntArray( BOSS_SCORES );
		else                                bossScores = new int[5];
		efpdBossesCleared = bundle.getInt( EFPD_BOSSES );
		efpdInHideout = bundle.getBoolean( EFPD_HIDEOUT );
		efpdOpenWorld = bundle.getBoolean( EFPD_OPENWORLD );
		efpdCorpseId = bundle.getInt( EFPD_CORPSE_ID );
		efpdCorpseDepth = bundle.contains( EFPD_CORPSE_D ) ? bundle.getInt( EFPD_CORPSE_D ) : -1;
		efpdCorpseBranch = bundle.getInt( EFPD_CORPSE_B );
		efpdDeaths = bundle.getInt( EFPD_DEATHS );
		efpdSurvived = bundle.getBoolean( EFPD_SURVIVED );
		efpdSurvivedTurn = bundle.getInt( EFPD_SURV_TURN );
		efpdBossKills = bundle.getInt( EFPD_BOSS_KILLS );
		efpdUrban = bundle.getBoolean( EFPD_URBAN );
		com.shatteredpixel.shatteredpixeldungeon.efpd.urban.Urban.restoreFromBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.hvt.Hvt.restoreFromBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.lab.Lab.restoreFromBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.RingSage.restoreFromBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.TownServices.restoreFromBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.PermStats.restoreFromBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.Facilities.restoreFromBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.Regen.restoreFromBundle( bundle );
		com.shatteredpixel.shatteredpixeldungeon.efpd.Allies.restoreFromBundle( bundle );
		totalBossScore  = bundle.getInt( TOT_BOSS );
		if (bundle.contains( QUEST_SCORES ))questScores = bundle.getIntArray( QUEST_SCORES );
		else                                questScores = new int[5];
		totalQuestScore = bundle.getInt( TOT_QUEST );
		winMultiplier   = bundle.getFloat( WIN_MULT );
		chalMultiplier  = bundle.getFloat( CHAL_MULT );
		totalScore      = bundle.getInt( TOTAL_SCORE );
		
		upgradesUsed    = bundle.getInt( UPGRADES );
		sneakAttacks    = bundle.getInt( SNEAKS );
		thrownAttacks   = bundle.getInt( THROWN );

		spawnersAlive   = bundle.getInt( SPAWNERS );
		
		duration		= bundle.getFloat( DURATION );

		qualifiedForNoKilling = bundle.getBoolean( NO_KILLING_QUALIFIED );
		qualifiedForBossRemainsBadge = bundle.getBoolean( BOSS_REMAINS_QUALIFIED );
		qualifiedForBossChallengeBadge = bundle.getBoolean( BOSS_CHALLENGE_QUALIFIED );
		qualifiedForRandomVictoryBadge = bundle.getBoolean( RANDOM_VICTORY_QUALIFIED );
		
		amuletObtained	= bundle.getBoolean( AMULET );
		gameWon         = bundle.getBoolean( WON );
		ascended        = bundle.getBoolean( ASCENDED );
	}
	
	public static void preview( GamesInProgress.Info info, Bundle bundle ){
		info.goldCollected  = bundle.getInt( GOLD );
		info.maxDepth       = bundle.getInt( DEEPEST );
		info.efpdOpenWorld  = bundle.getBoolean( EFPD_OPENWORLD );
	}

}
