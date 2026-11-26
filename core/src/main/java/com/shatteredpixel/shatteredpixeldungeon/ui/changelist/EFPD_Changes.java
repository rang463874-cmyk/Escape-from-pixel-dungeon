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

package com.shatteredpixel.shatteredpixeldungeon.ui.changelist;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;

import java.util.ArrayList;

/**
 * Escape from Pixel Dungeon's own update history, shown first in the changes screen.
 * One section per version, one icon per change; tapping an icon shows its description.
 *
 * Every new release adds a section at the top of addAllChanges.
 */
public class EFPD_Changes {

	public static final int BUGFIX = 0xFFAA44;

	public static void addAllChanges( ArrayList<ChangeInfo> c ){
		ChangeInfo v;

		v = version( c, "v0.18.0" );
		add( v, Icons.TALENT.get(), "유산 퍽",
				"엔딩(옌더의 부적을 들고 탈출, 연구소 탈출)을 볼 때마다 유산 포인트를 얻습니다. 포인트로 모든 게임에 적용되는 퍽을 삽니다.\n\n- 강인한 육체: 최대 체력 +5 (3단계)\n- 타고난 힘: 힘 +1 (2단계)\n- 짐꾼: 무게 한도 +4 (3단계)\n- 경량 장비: 무기·방어구 무게 -15% (2단계)\n- 탄띠: 탄약 무게 절반\n- 야전 의무병: 붕대 + 부목 = 응급처치 키트\n- 키카드 위조범: 인식표 3개 = 연구소 키카드\n\n모드 선택 창, 은신처, 거점 관리인 메뉴에서 열 수 있습니다." );
		add( v, Icons.WARNING.get(), "버그 수정",
				"익스트랙션 모드에서 부적을 얻은 자리에서 바로 엔딩을 보면 아이템이 창고에 회수되지 않던 문제를 고쳤습니다. 오픈월드에서 바로 엔딩을 고르면 클리어 연출 뒤 마을에서 게임이 이어집니다." );

		v = version( c, "v0.17.0" );
		add( v, new ItemSprite( ItemSpriteSheet.RETURN_BEACON ), "은신처 길잡이",
				"익스트랙션 모드의 상점(6·11·16층, 20층 임프 상점)에서 은신처 길잡이를 팝니다. 값은 앙크의 두 배입니다.\n\n들고 다닐 수 없는 서비스로, 사는 즉시 가진 것을 모두 들고 은신처로 탈출합니다." );

		v = version( c, "v0.16.5" );
		add( v, new ItemSprite( ItemSpriteSheet.SPLINT ), "부상 기준 조정",
				"한 번에 받은 피해로 부상이 생기는 기준을 낮췄습니다.\n\n- 최대 체력의 30% 이상: 바로 중상 (기존 40%)\n- 15% 이상: 확정 경상 (기존 25%)\n- 15% 미만: 6% 확률로 경상 (그대로)" );

		v = version( c, "v0.16.4" );
		add( v, Icons.CHANGES.get(), "업데이트 내역 개편",
				"업데이트 내역을 원작처럼 버전별 아이콘으로 바꿨습니다. 아이콘을 누르면 설명이 나옵니다. 원작의 변경사항은 아래 \"Shattered->\" 탭에 있습니다.\n\n업데이트 알림 창에는 최신 버전의 내용만 나오고, 업데이트 버튼은 한 번 확인하면 더 이상 깜빡이지 않습니다." );

		v = version( c, "v0.16.3" );
		add( v, Icons.WARNING.get(), "버그 수정",
				"연구소를 클리어하고 돌아와도 \"연구소 공기\"와 \"봉쇄\" 디버프가 남아 있던 문제를 고쳤습니다. 연구소를 나오는 순간(탈출, 사망) 바로 사라집니다." );

		v = version( c, "v0.16.2" );
		add( v, Icons.WARNING.get(), "버그 수정",
				"오픈월드에서 부적을 들고 나와 마을로 돌아온 뒤에도 부적의 저주(승천 도전)가 남아 있던 문제를 고쳤습니다. 부적을 손에서 놓으면(창고 보관, 사망) 마을에 도착할 때 저주가 풀립니다." );

		v = version( c, "v0.16.1" );
		add( v, new ItemSprite( ItemSpriteSheet.AMULET ), "부적을 들고 탈출",
				"부적을 들고 1층 계단으로 올라가면 원작의 게임 클리어 연출이 나오고, 부적은 기념품으로 창고에 자동 보관됩니다(꺼낼 수 없음).\n\n오픈월드는 연출 뒤 마을에서 게임이 계속됩니다." );

		v = version( c, "v0.16.0" );
		add( v, Icons.PREFS.get(), "개발자 도구 개편",
				"현재 위치 표시, 한 층 위로/아래로 이동, 층 번호로 이동(도시에서는 구역 이동)을 넣었습니다.\n\n아이템 소환은 카테고리별 페이지로 나눴습니다." );

		v = version( c, "v0.15.2" );
		add( v, Icons.WARNING.get(), "버그 수정",
				"오픈월드에서 재생성된 층(예: 10층)으로 아래층에서 올라오면 어두운 출구 구역에 갇히던 문제를 고쳤습니다. 재생성된 층에는 입구 쪽에서 도착합니다." );

		v = version( c, "v0.15.1" );
		add( v, new ItemSprite( ItemSpriteSheet.AMULET ), "부적과 지름길",
				"오픈월드에서 요그의 부적을 들고 올라가는 동안에는 마을로 가는 지름길(층 계단의 \"마을로 돌아가기\", 도시 성문, 귀환 부적)이 막힙니다." );

		v = version( c, "v0.15.0" );
		add( v, new ItemSprite( ItemSpriteSheet.DOG_TAG ), "현상금 게시판",
				"오픈월드 거점의 현상금 게시판에서 도시 구역의 표적 의뢰를 받습니다. 의뢰한 구역이 표적과 함께 새로 만들어지고, 처치하면 200×구역 번호 골드를 받습니다." );
		add( v, new ItemSprite( ItemSpriteSheet.KEYCARD ), "오픈월드 연구소",
				"지역 지도의 \"연구소\"에서 키카드로 입장합니다. 연구소에서는 귀환 부적이 막히고, 엘리베이터로 탈출하면 마을로 돌아오며 엔딩이 뜹니다.\n\n오픈월드에서도 키카드가 나옵니다." );

		v = version( c, "v0.14.1" );
		add( v, new ItemSprite( ItemSpriteSheet.CHEST ), "창고 묶음 합치기",
				"창고가 정렬될 때 같은 아이템 묶음이 자동으로 하나로 합쳐집니다." );

		v = version( c, "v0.14.0" );
		add( v, new ItemSprite( ItemSpriteSheet.GOLD ), "마을 상점가와 훈련장",
				"상점가: 보급품, 희귀 상품(강화 주문서·힘의 물약은 살 때마다 값이 오름), 수상한 상자(하루 3개).\n\n훈련장: 골드로 영구 능력치 숙련 경험치를 삽니다(하루 3회)." );
		add( v, new ItemSprite( ItemSpriteSheet.CHEST ), "창고 연동",
				"마을의 구매·건설·훈련에 창고 골드도 함께 씁니다.\n\n창고에서 아이템과 골드를 원하는 개수만큼 꺼낼 수 있고, 마을 연금술 작업대에서는 창고 재료를 바로 넣을 수 있습니다." );

		v = version( c, "v0.13.0" );
		add( v, new ItemSprite( ItemSpriteSheet.RING_GARNET ), "반지 현자",
				"오픈월드에서 요그 드제바를 쓰러뜨리면 거점 마을에 반지 현자가 나타납니다. 반지를 바치면 그 효과를 영구히 얻습니다(종류마다 하나, 더 강한 반지로 교체 가능)." );

		v = version( c, "v0.12.0" );
		add( v, new ItemSprite( ItemSpriteSheet.KEYCARD ), "연구소 (최종 목적지)",
				"창고의 연구소 키카드를 써서 들어가는 엔드 콘텐츠입니다. 정예 용병과 경비 골렘, 허기 2배와 독가스, 발전기 3대와 비상 레버. 레버를 당기면 경보와 정예 증원군이 몰려옵니다. 탈출하면 엔딩입니다." );

		v = version( c, "v0.11.0" );
		add( v, new ItemSprite( ItemSpriteSheet.DOG_TAG ), "현상금 토벌 (HVT)",
				"시가전 도시의 구역마다 숨은 표적과 호위병을 사냥하는 모드입니다. 위치 힌트만 주어지고, 표적은 엄폐·광역 공격·연막 도주·회복 같은 전술로 싸웁니다.\n\n처치하면 인식표와 전용 장비 또는 비밀 금고 열쇠를 얻고, 구역 전체에 경보가 울립니다." );

		v = version( c, "v0.10.0" );
		add( v, Icons.STAIRS.get(), "오픈월드와 도시 연결",
				"오픈월드 캐릭터가 지역 지도의 \"폐허 도시\"로 시가전 도시를 오갈 수 있습니다. 도시 건물 맨 위층 보물상자에서 희귀 전리품이 나옵니다." );

		v = version( c, "v0.9.0" );
		add( v, new ItemSprite( ItemSpriteSheet.ASSAULT_RIFLE ), "시가전 모드",
				"넓은 절차적 도시, 건물 위층·지하층, 지하철로 다음 구역, 성문으로 탈출합니다. 새 도시형 적: 약탈자, 총잡이." );

		v = version( c, "v0.8.0" );
		add( v, new ItemSprite( ItemSpriteSheet.GLAIVE ), "고유 무기",
				"보스 전리품으로 고유 무기를 만듭니다: 점액 칼날(구), 텐구의 발톱(텐구), 피스톤 망치(DM-300), 왕의 글레이브(드워프 왕). 창고 자동 정렬도 추가했습니다." );

		v = version( c, "v0.7.0" );
		add( v, Icons.TALENT.get(), "거점에 합류하는 NPC",
				"오픈월드에서 퀘스트를 완료한 유령, 지팡이 장인, 대장장이, 임프가 거점에 합류해 각자 이점을 줍니다." );

		v = version( c, "v0.6.0 ~ v0.6.3" );
		add( v, new ItemSprite( ItemSpriteSheet.SUPPLY_PACK ), "오픈월드 확장",
				"층 재생성과 보스 재등장, 귀환 부적, 생존 기록, 보급 가방, 거점 시설(침대, 조리대, 의무실, 작물밭, 대장간)." );
		add( v, new ItemSprite( ItemSpriteSheet.REVOLVER ), "편의 기능",
				"총을 조준한 상태에서 자기 자신을 누르면 재장전합니다. 적이 없는 곳에서 휴식하면 낮은 확률로 부상이 낫습니다." );

		v = version( c, "v0.5.0 이전" );
		add( v, new ItemSprite( ItemSpriteSheet.CHEST ), "익스트랙션",
				"창고, 1층·보스 층 탈출, 은신처 상점과 연금술." );
		add( v, new ItemSprite( ItemSpriteSheet.BANDAGE ), "생존 시스템",
				"무게 인벤토리, 부위별 부상·출혈과 의료품, 총기 15종과 탄약." );
		add( v, Icons.ENTER.get(), "오픈월드 생존",
				"거점 마을, 지역 지도, 사망 시 유품, 영구 스탯(단련 + 강장제), 갈증·수통·우물, 낮·밤 주기." );
	}

	private static ChangeInfo version( ArrayList<ChangeInfo> c, String title ){
		ChangeInfo v = new ChangeInfo( title, true, "" );
		v.hardlight( Window.TITLE_COLOR );
		c.add( v );
		return v;
	}

	private static void add( ChangeInfo v, Image icon, String title, String text ){
		v.addButton( new ChangeButton( icon, title, text ) );
	}

}
