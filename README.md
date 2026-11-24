# Escape from Pixel Dungeon

[Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon) (v4.0.2)을 기반으로 한 **하드코어 파밍·익스트랙션 생존 모드**입니다.
원작은 Evan Debenham의 작품이며, 이 모드도 원작과 같이 **GPLv3** 라이선스를 따릅니다 (`LICENSE.txt`). 원본 README는 `README_SPD.md`에 있습니다.

## 핵심 루프 (익스트랙션)

- **은신처 창고(Stash)**: 게임과 게임 사이에 유지되는 영구 창고 (`stash.dat`).
- **탈출**: 1층 입구 계단을 밟으면 탈출 창이 뜹니다. 탈출하면 소지한 모든 아이템(퀘스트 아이템 제외)과 골드가 창고로 옮겨지고 게임이 끝납니다.
- **사망**: 가지고 들어간 모든 것을 잃습니다.
- **준비**: 창고에서 아이템을 꺼내는 건 탐험 시작 전(처음 내려가기 전)에만 할 수 있습니다.
- 앰뮬렛을 들고 탈출해도 창고에 보관됩니다.

모드 코드는 `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/efpd/`에 있고, 원본 파일을 고친 곳에는 `//EFPD:` 주석을 달았습니다.

## 로드맵

- [x] 은신처 창고 + 1층 탈출
- [ ] 깊은 층의 탈출 지점 (보스 층 이후 등)
- [ ] 무게 기반 인벤토리 / 과적 페널티
- [ ] 부상·출혈 상태이상과 치료 아이템
- [ ] 오픈월드 생존 모드 (영구 스탯 성장)

## 빌드 (데스크톱)

```bash
# Android SDK 없이 데스크톱만 빌드/실행
./gradlew desktop:debug -PdesktopOnly     # 실행
./gradlew desktop:release -PdesktopOnly   # 실행 가능한 jar
```

`-PdesktopOnly`를 빼면 원래처럼 android/ios 모듈도 포함됩니다 (Android SDK 필요).
