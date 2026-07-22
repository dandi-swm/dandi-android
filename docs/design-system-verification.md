# Nyummy Android Design System 검증 기록

검증일: 2026-07-22

## 기준과 환경

- Figma file: `olJJrCuFYJypKJCR3RNSn9`
- canonical page: `02 · LIVE Android Design System · v1.0.0-rc.3` (`984:37241`)
- 비교 제외: `99` Legacy/Archive/Exploration 및 canonical page 밖의 draft variant
- Android fixture: debug-only `DesignSystemCatalogActivity`
- Emulator: `Pixel_8(AVD) - 17`, physical `1080×2400`, density `420dpi`
- App locale/theme: `ko-KR`, light
- 고정 component viewport: Figma의 `360/370/390/400dp` reference bounds, 통합 fixture `390×800dp`

## Contract inventory

| 영역 | Figma 계약 | Android 확인 결과 |
|---|---:|---|
| Semantic color | 141 roles | getter 141 + string-key mapping 141, 누락 0 |
| Typography | 21 local text styles | getter 21 + style-key mapping 21, bundled font 적용 |
| Dimensions | 29 variables | radius/spacing/size/layout 29개 매핑 |
| Effects | blur 1 + shadow 5 | background blur token 1 + exact drop shadow 5 |
| Common | P0 component 12개 | 전용 Preview 또는 debug catalog에 모두 포함 |
| Button | 4 style × 5 state × 2 size × 2 width | 80/80 렌더 및 계약 테스트 통과 |
| Input | 2 type × 6 state | 12/12 렌더 및 계약 테스트 통과 |
| Dialog / Sheet | 3 + 3 | 6/6 catalog fixture 포함 |
| State Surface | 14 types | 14/14 렌더 및 계약 테스트 통과 |
| Bottom Navigation | 3 style × 5 selected | 15/15 catalog fixture, 5 destination semantics + indicator-bounded press/moving indicator 통과 |
| Calendar Day | 2 × 3 × 4 | 24/24 렌더 및 계약 테스트 통과 |
| Meal / History | MealRow 4, Banner 2, Daily 5, Detail 4, Nutrition 3, Photo 5 | 모든 canonical state catalog fixture 포함 |

## Figma 병렬 시각 비교

Figma node export와 같은 reference bounds의 Android catalog/emulator 캡처를 나란히 놓고 color, bundled font baseline, spacing, radius, 1px stroke, icon geometry, shadow 방향을 확인했다.

| 비교 대상 | Canonical node | 결과 |
|---|---:|---|
| Button / IconButton / Input / common surfaces | `990:1052`, `990:1079`, `992:765`, `993:681–764` | blocking diff 없음 |
| Edit/Destructive/Notice Dialog | `956:563` | bounds, radius, action hierarchy, scrim과 shadow 일치; blocking diff 없음 |
| Confirm/MealSummary/CollectionDetail Sheet | `956:645` | bounds, handle, radius, shadow와 content hierarchy 일치; blocking diff 없음 |
| Bottom Navigation | `634:11768`, item `619:44573` | `홈/히스토리/퀘스트/컬렉션/상점` 순서, canonical path icon, indicator, Compact/Floating/FullWidth 일치 |
| Today Meals / Calendar / meal states | `884:554`, `800:13506`, `800:13515`, `844:12774`, `1042:11`, `1043:64` | geometry, status mapping, food icon asset와 semantic color 일치 |
| Meal Detail / Nutrition / Photo Picker | `481:87`, `1044:66`, `1046:588` | 상태별 hierarchy, track, action bounds 일치; product photo는 caller slot으로 확인 |
| Home sheet + navigation integration | Figma screen pages `10–50` | `390×800dp` fixture에서 MealSummary가 Floating navigation 위에 배치되어 가리지 않음 |

재검증 과정에서 잘못된 navigation 구성과 임의 Material icon을 canonical 5개 destination/vector path로 교체했고, 임시 음식 원형을 Figma의 Salad/Pasta/Rice pixel asset으로 교체했다. 통합 fixture의 navigation style도 canonical Floating으로 맞췄으며, canonical page에 없는 `cardFeatured` variant는 제거했다.

다음 차이는 component 결함으로 처리하지 않는다.

- 실제 음식 사진, 고양이, 이벤트/제품 artwork는 screen/content asset이므로 component가 caller slot으로 받는다. Catalog에서는 `runtime slot`으로 명시한다.
- Figma sheet 샘플과 catalog fixture의 날짜·식사 문구는 다르지만 같은 geometry와 type/color token을 사용한다.
- status/navigation system bar는 Android platform 영역이라 component screenshot 비교에서 제외한다.
- modal background blur는 foreground scrim에 적용하면 잘못된 layer가 흐려진다. Scrim은 정확한 12% token과 input blocking을 소유하고, host가 아래 content layer에 `modalBackgroundBlur`를 적용한다.

최종 육안 비교에서 출시를 막는 visual diff는 발견하지 못했다.

## 자동 검증

### Compose instrumentation contract

`DesignSystemContractTest`를 `Pixel_8(AVD) - 17`에서 실행했다.

- tests `7`, failures `0`, errors `0`, skipped `0`
- Chip: canonical `72×48dp` bounds와 icon이 없는 label의 수평·수직 중심점 일치
- Button 80조합: canonical bounds/role/enabled/loading, 실제 touch pressed, 실제 focus + Enter, 렌더 변화
- Input 12조합: outer `360dp`, inner `328×56/92dp`, total min height, enabled/read-only/error/focus와 실제 text replacement
- Navigation: destination 5개와 Floating `370×76dp`, press 렌더링이 `44×30dp` indicator 바운드 밖을 변경하지 않음, `280ms` indicator 중간/종료 위치
- State Surface: 14 types와 action semantics
- Calendar Day: 24조합의 selection/icon/nutrition state
- report: `common/presentation/build/reports/androidTests/connected/debug/index.html`

### Static, architecture, build and lint

| Gate | 결과 |
|---|---|
| raw hex outside token layer | 0 |
| raw `sp` outside type layer | 0 |
| `git diff --check` | pass |
| architecture-guardian | HIGH 0 / MEDIUM 0 / LOW 0 |
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew :common:presentation:assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| `./gradlew test` | BUILD SUCCESSFUL |
| `./gradlew :common:presentation:connectedDebugAndroidTest` | BUILD SUCCESSFUL, 7/7 |
| `./gradlew :common:presentation:lintDebug` | BUILD SUCCESSFUL, issue 0 |
| `./gradlew :app:lintDebug` | BUILD SUCCESSFUL |

App lint에는 프로젝트에 이미 있던 dependency/plugin update와 `MissingApplicationIcon` warning이 남지만, 이번 design-system 변경 모듈의 새 lint issue는 0이다.

## Release follow-up

새로 번들한 Noto Sans KR, Jua, Gowun Dodum, Fredoka font는 압축 전 약 19MB다. 배포 전에 APK/AAB size budget을 확인하고, 각 font의 SIL OFL source/license에 맞는 `OFL` 또는 `NOTICE` 파일을 저장소와 배포 산출물에 포함할지 확인한다. 이는 현재 component/architecture 검증 실패는 아니다.
