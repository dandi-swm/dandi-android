# Nyummy Android Design System v1.0.0-rc.3

이 문서는 Android 구현과 검증에 사용하는 고정 명세다. 원본은 Figma `02 · LIVE Android Design System · v1.0.0-rc.3` 페이지(`984:37241`)이며, 이 문서에 적힌 node/style/collection ID로 다시 추출할 수 있어야 한다.

## Source boundary

| 분류 | 사용 규칙 |
|---|---|
| Published P0 canonical | 아래에 열거한 `984:37241`의 component root와 asset node. 공통 Compose API 구현 대상이다. |
| Screen/flow reference | Figma `10–50` 페이지. P0를 조합한 화면의 위치와 흐름을 검증할 때만 사용한다. |
| Noncanonical | canonical 페이지에 없는 draft/example frame. P0 API나 새 variant의 근거로 사용하지 않는다. |
| Legacy | Figma `99` 페이지와 `Legacy/Archive/Exploration` 표기 frame. 구현·비교·asset export에서 제외한다. |

## Tokens

### Color

- Color Primitives: `VariableCollectionId:8:2` — 515 variables.
- Semantic Color: `VariableCollectionId:8:3` — 141 roles.
- Android semantic slot은 Figma path를 lower camel case로 1:1 변환한다.
- variable alias는 해석한 최종 ARGB를 palette에 두고 semantic slot은 그 palette entry를 참조한다.
- `bg/scrim/default`는 `palette/overlay/cocoa-40` (`#170D0966`)에 연결한다. `bg/scrim/modal`과 합치지 않는다.
- 템플릿이 원래 제공하던 `Black`, `White`, `Gray300…Gray900`, `Blue400`, `Red` palette entry는 사용 여부와 관계없이 호환성을 위해 유지한다.
- Material3 `colorScheme`은 fallback projection일 뿐 canonical token을 대체하지 않는다. product component는 `DesignSystemThemeImpl.designSystemColor`를 직접 쓴다.

### Dimensions

Figma collection `VariableCollectionId:8:4`의 29개 값이다.

| 그룹 | 값 |
|---|---|
| radius | `0`, `8`, `12`, `16`, `20`, `22`, `24`, `32`, `full=999` dp |
| spacing | `4`, `8`, `12`, `16`, `20`, `24`, `32` dp |
| character size | `128`, `152`, `224` dp |
| bottom navigation | `base=76`, `compact=66`, `floating=76`, `full=80` dp |
| layout | `mobile gutter=20`, `min touch=48`, `meal row=68`, `meal leading=44` dp |
| calendar | `food icon=24`, `marker=6` dp |

Compose의 density 변환 뒤에도 Figma의 1px stroke가 사라지지 않게 border를 명시한다. 고정 reference 폭은 preview/golden fixture의 viewport이며 실제 screen에서는 constraint 안에서 fill/hug 정책을 유지한다.

## Typography

모든 style은 letter spacing `0`, platform font padding 비사용을 기본으로 한다. 숫자는 `font size / line height`(sp), key는 Figma local text style key다.

| Figma style → Kotlin slot | Font / weight | Size | Style key |
|---|---|---:|---|
| `display/regular/XXL` → `displayRegularXXL` | Jua Regular | 32/40 | `a2dd11360d264292320c764469309450bb8d2451` |
| `display/regular/XL` → `displayRegularXL` | Jua Regular | 24/32 | `635f5d9bdd09bc28400cd69ab5384f3d86c1082a` |
| `display/regular/L` → `displayRegularL` | Jua Regular | 20/28 | `2ee731c39837578df823437e2b8db0180856019e` |
| `display/regular/M` → `displayRegularM` | Jua Regular | 18/26 | `b6085429cb464ea7be2a51625cbbe3dc3e6c9d9d` |
| `voice/regular/M` → `voiceRegularM` | Gowun Dodum Regular | 16/24 | `09344b376852bf9281910157d64830ec3b9aeaa5` |
| `title/strong/L` → `titleStrongL` | Jua Regular | 24/32 | `2b9ad14d07941a9bfb3b2f6e1246e91894b45ab5` |
| `display/strong/XL` → `displayStrongXL` | Noto Sans KR Bold | 26/34 | `1e86a5797c75d613857739556a432505f2c67ec1` |
| `display/strong/L` → `displayStrongL` | Noto Sans KR Bold | 22/30 | `c128b5d78bc8cfecca8cee2e90e7451744a35cd1` |
| `display/strong/M` → `displayStrongM` | Noto Sans KR Bold | 20/28 | `6e1c602cadf253d72cb1fc0399fdc7323657caf6` |
| `text/strong/XL` → `textStrongXL` | Noto Sans KR Bold | 18/26 | `2ff2f20936051bfa0ad398587decb958aa5338f8` |
| `text/strong/L` → `textStrongL` | Noto Sans KR Medium | 16/24 | `9eeb28d539e3292a33126d414900f83dc9a704cb` |
| `text/strong/M` → `textStrongM` | Noto Sans KR Medium | 14/22 | `f211949f327476d7fdfab1ee0150cf390d48342d` |
| `text/regular/L` → `textRegularL` | Noto Sans KR Regular | 16/24 | `890bacb273dc7d72d111cd70b946ac01b1177c56` |
| `text/regular/M` → `textRegularM` | Noto Sans KR Regular | 14/22 | `07f854a7c08a99d024b4763270309f838e874436` |
| `text/regular/S` → `textRegularS` | Noto Sans KR Regular | 12/18 | `518115f478657973eb3c3348680b06612f03a7e5` |
| `text/regular/XS` → `textRegularXS` | Noto Sans KR Regular | 11/16 | `81f1788385f4f20a755ec4f93e865282b315fa80` |
| `label/strong/S` → `labelStrongS` | Noto Sans KR Medium | 12/18 | `5de4301ae9ea422977202e54816376ac8dd013e4` |
| `label/strong/XS` → `labelStrongXS` | Noto Sans KR Bold | 11/16 | `3971a26416c1d8bdf1692532063d583f8eb0959a` |
| `label/regular/XS` → `labelRegularXS` | Noto Sans KR Regular | 11/16 | `e4bcedce299162cc9a50d1b0c045c74affb82509` |
| `number/strong/L` → `numberStrongL` | Fredoka Bold | 24/30 | `93ff718f7376b05ae322dcea48baa721d24f9729` |
| `number/strong/M` → `numberStrongM` | Fredoka Bold | 18/24 | `371bf374222792804d89456c69ef8e03d75b4f30` |

Font assets are self-hosted Noto Sans KR variable, Jua Regular, Gowun Dodum Regular, and Fredoka variable under SIL Open Font License 1.1. Preview와 screenshot test에서 실제 bundled font가 로드된 상태로 비교한다.

## Effects

Figma effect의 `radius`, `spread`, `offset`, alpha를 그대로 `dropShadow`/blur로 옮긴다.

| Figma style | Style key | Contract |
|---|---|---|
| `Effect / Modal Background Blur` | `3af47b81e574386e443f2c45a9869d22ccd326c4` | background blur radius `2`; host가 modal 아래의 content layer에 적용 |
| `Elevation / Navigation / Floating` | `50664fb8344acab85aa03882e9175ed30c11a048` | drop shadow offset `(0,-2)`, radius `16`, spread `0`, ink tone 36 at 18% |
| `Elevation / Surface / Low` | `a7fa280e112c1def13e4e4fdfe980d02ea6c1bad` | drop shadow offset `(0,4)`, radius `12`, spread `0`, ink tone 20 at 6% |
| `Elevation / Dialog / Standard` | `b9b84085b25bd1aaae747d9519c17dff6d916130` | drop shadow offset `(0,12)`, radius `28`, spread `0`, ink tone 15 at 14% |
| `Elevation / Sheet / Standard` | `d189de5e8412ecf00369f6bf6da89e018ce01168` | drop shadow offset `(0,-8)`, radius `24`, spread `0`, ink tone 20 at 13% |
| `Elevation / Floating Action` | `6aff0a1cf1adf60ff361729042508f50c5a924f4` | drop shadow offset `(0,5)`, radius `14`, spread `0`, evergreen tone 45 at 18% |

## Common P0 component contract

| Compose target | Canonical node | Variant/state contract | Reference geometry |
|---|---:|---|---|
| `NyummyButton` | `990:1052` | Style `Primary/Secondary/Danger/Reward` × State `Default/Pressed/Focused/Disabled/Loading` × Size `M/L` × Width `Hug/Full` = 80 | M `48`, L `56`, min width `96`, Full fixture `320`, radius `12` |
| `NyummyIconButton` | `990:1079` | Style `Ghost/Filled` × State `Default/Pressed/Focused/Disabled` = 8 | `48×48`, icon `24`, radius full |
| `NyummyTextField` | `992:765` | Type `SingleLine/Multiline` × State `Empty/Focused/Filled/Error/Disabled/ReadOnly` = 12 | Single `360×106`, Multi `360×170`; field `56/120`, radius `12`; label-helper gap `6`, horizontal padding `16`, content gap `8`, icon `20` |
| `NyummyTopAppBar` | `993:681` | canonical child visibility and alignment only; caller supplies title/actions | `360×64`, action touch target `48` |
| `NyummySnackbar` | `993:690` | message + optional action; one active snackbar owns live-region semantics | fixture `400×72`, min height `64` |
| `NyummyCard` | `993:695` | canonical container; no product-specific click behavior | `360×132`, radius `16` |
| `NyummyListRow` | `993:699` | leading/content/trailing slots | fixture `400×72`, min height `64` |
| `NyummyChip` | `993:728` | `Default/Selected/Focused/Disabled` | `72×48`, min width `72` |
| `NyummyBadge` | `993:739` | Tone `Neutral/Positive/Warning/Error` | sample `37×24` |
| `NyummyLinearProgress` | `993:753` | Value `0/25/50/75/100` | `320×8`; runtime value는 0…1로 clamp |
| `NyummyLoading` | `993:762` | Size `S/M/L` | `16/24/32` |
| `NyummyModalScrim` | `993:764` | modal scrim semantic color + modal blur | parent bounds 전체 (`360×800` fixture) |

세부 layout/type 규칙:

- Button M은 horizontal padding `16`, icon `18`, `text/strong/M`; L은 padding `20`, icon `20`, `text/strong/L`다. 둘 다 gap `8`이다. Loading도 label과 ring을 함께 유지하며 M/L Hug 기준 폭은 `110/127`이다. M/Hug variant node는 Primary `990:492/520/548/576/604`, Secondary `990:632/660/688/716/744`, Danger `990:772/800/828/856/884`, Reward `990:912/940/968/996/1024` 순으로 Default/Pressed/Focused/Disabled/Loading이다.
- IconButton variant node는 Ghost `990:1055/1058/1061/1064`, Filled `990:1067/1070/1073/1076` 순으로 Default/Pressed/Focused/Disabled다.
- Input variant node는 SingleLine `992:657/666/675/684/693/702`, Multiline `992:711/720/729/738/747/756` 순으로 Empty/Focused/Filled/Error/Disabled/ReadOnly다. label은 `label/strong/S`, value는 `text/regular/M`, supporting text는 `text/regular/S`; SingleLine은 max 1, Multiline은 max 4, supporting text는 max 1 line이다.
- TopAppBar는 horizontal padding `4`, gap `4`, 양쪽 `48` touch target과 title `248`로 구성하고 title은 `text/strong/XL`, icon은 `24`다.
- Snackbar는 padding top/right/bottom `12`, left `16`, gap `8`; message width fixture `268`, `text/regular/M`, max 2 lines; action target `96×48`, `text/strong/M`이다. text가 커지면 높이를 늘린다.
- Card는 vertical padding `16`, gap `8`; title `text/strong/L` max 1, body `text/regular/M` max 3이다. ListRow는 horizontal padding `8`, gap `8`, leading/trailing target `48`, title/subtitle gap `2`다.
- Chip variant node는 `993:712/716/720/724`, Badge는 `993:731/733/735/737`이다. Chip은 horizontal padding `16`, gap `8`, optional icon `18`, `text/strong/M`; Badge는 horizontal padding `8`, `label/strong/XS`다.
- LinearProgress variant node는 `993:743/745/747/749/751`, indicator node는 `993:744/746/748/750/752`; indicator width는 `1/80/160/240/320`이다. API progress 0도 range semantics는 0이지만 Figma의 round cap을 보이기 위한 1dp 시각값을 유지한다.
- Loading node는 `993:756/758/760`, ring node는 `993:757/759/761`; stroke `2/3/3`, 270° arc다. 기본 motion은 progress `600ms ease-out`, loading `900ms linear`이다. Compose Animation 1.11의 `MotionDurationScale`을 그대로 따르므로 시스템 애니메이터 배율이 `0`이면 progress는 끝 값으로 snap하고 무한 loading은 static frame에서 정지한다.
- ModalScrim은 semantic scrim `12%`를 이전 화면 전체 위, modal 아래에 그리고 pointer를 차단하며 dismiss 가능 여부를 caller 정책으로 semantics에 반영한다. Compose의 foreground `Modifier.blur`는 아래 화면이 아니라 scrim 자체를 흐리므로 사용하지 않는다. Figma의 background blur `2`가 필요한 host는 modal 아래의 content layer에 `designSystemEffects.modalBackgroundBlur`를 적용한다.

State는 parameter로 결정하고 enabled/loading/focus/press의 의미를 섞지 않는다. Loading button은 label width를 유지하며 중복 click을 받지 않는다. ReadOnly input은 focus 가능한 disabled가 아니라 읽기 전용 semantics를 가진다. Figma root와 descendant에는 prototype reaction이 없으므로 animation, dismiss, drag 정책은 Android가 임의로 canonical visual contract라고 주장하지 않는다.

## Overlay and state contract

| Compose target | Canonical node | Contract |
|---|---:|---|
| `NyummyEditDialog` / `NyummyDestructiveDialog` / `NyummyNoticeDialog` | `956:563` | Type `Edit` (`956:522`, `334×308`), `Destructive` (`956:533`, `334×286`), `Notice` (`956:562`, `342×610`); radius `20`; Edit/Destructive만 dialog effect, Notice는 1px border/no shadow |
| `NyummyConfirmBottomSheet` / `NyummyMealSummaryBottomSheet` / `NyummyCollectionDetailBottomSheet` | `956:645` | Type `Confirm` (`956:582`, `390×290`), `MealSummary` (`956:626`, `370×546`), `CollectionDetail` (`956:644`, `390×430`); radius `24`; sheet effect 적용 |
| `NyummyStateSurface` | `475:62` | 14개 type, 각 `190×220`, radius `16`; 아래 이름을 그대로 public state로 제공 |

State Surface types:

`Empty` (`475:6`), `Loading` (`475:14`), `Offline` (`475:22`), `Analysis Failed` (`475:30`), `Permission Denied` (`475:38`), `Ended` (`475:46`), `Destructive` (`475:54`), `Partial` (`519:6032`), `Retrying` (`519:6041`), `Reward Pending` (`519:6087`), `Reward Completed` (`519:6096`), `Already Claimed` (`519:6105`), `Reconcile Failed` (`519:6114`), `Rate Limited` (`519:6123`).

Dialog/Sheet는 scrim click, back dismiss, confirm/cancel callback을 caller가 소유한다. Figma에는 dismiss/drag reaction이 없으므로 임의 gesture를 디자인 계약으로 추가하지 않는다. Bottom Sheet isolated reference는 네 모서리 radius `24`이며, 실제 edge-attached sheet는 상단 모서리에만 적용한다. 홈에서 사용할 때는 Bottom Navigation 위 영역에 배치한다.

## Navigation contract

### Bottom Navigation

- Root `634:11768`: Style `Compact/Floating/FullWidth` × Selected `Home/History/Quest/Collection/Shop`.
- Item `619:44573`: Destination 5종 × Selected `False/True`, `70×58`, vertical gap `8`, bottom padding `7`.
- 순서와 라벨은 `홈 / 히스토리 / 퀘스트 / 컬렉션 / 상점`으로 고정한다.
- 선택 indicator는 `44×30`, x=`13`, radius full, `bg/brand/soft`; 선택 label은 `label/strong/XS` + `content/accent`; 비선택은 `label/regular/XS` + `content/default/level1`.
- 아이콘은 모두 `24×24`, stroke `2`, round cap/join, semantic tint `content/icon/level0`: Home `619:44557`, History `619:44559`, Quest `619:44562`, Collection `619:44565`, Shop `619:44570`.
- Android interaction policy (2026-07-22 user-approved): 각 destination의 터치·semantics 영역은 `70×58`을 유지하되 press indication은 아이콘의 `44×30` full-radius indicator 영역 안에서만 렌더링한다. 선택 indicator는 destination별로 새로 생성하지 않고 하나의 `44×30` layer가 `280ms` `FastOutSlowIn` motion으로 이동한다. 시스템 animator duration scale을 따르며, resting geometry는 위 Figma contract을 그대로 유지한다. 이 motion은 Figma prototype reaction이 아니라 Android interaction contract다.

| Style | Bounds and item positions | Surface |
|---|---|---|
| Compact | `370×66`; items x=`10/80/150/220/290`, y=`4` | radius `22`, floating navigation bg, 1px border |
| Floating | `370×76`; items x=`10/80/150/220/290`, y=`8` | radius `22`, floating navigation bg, border 없음 + Navigation Floating effect |
| FullWidth | `390×80`; items x=`20/90/160/230/300`, y=`10` | full-width navigation bg + top 1px divider |

각 destination은 하나의 selectable semantics node로 노출하고 icon과 label을 별도 click target으로 만들지 않는다.

## Meal and history contract

| Compose target | Canonical node | State/geometry contract |
|---|---:|---|
| `NyummyFloatingTodayMeals` | `884:554` | `122×56`, radius `20`, 1px border + Floating Action effect; Salad asset `38×38` at `(9,9)`, label `오늘 현황` `label/strong/S`, status dot `8` |
| `NyummyMealRow` | `800:13506` | `Completed/Analyzing/Failed/Retrying`; `350×68`, padding `12`, gap `8`, radius `16`, icon `44`. Failed만 retry action을 제공하고 processing/retrying은 중복 요청을 막는다. |
| `NyummyCalendarHeaderAction` | `800:13515` | `Previous/Next`, `48×48`; inner radius `14`, 1px calendar outline; chevron `22×22` (`22:54`, `22:58`) |
| `NyummyCalendarDay` | `844:12774` | Selection `Default/Selected` × IconCount `0/1/2` × NutritionStatus `Positive/OutOfRange/NoRecord/None` = 24; `50×56`, icon `24`, marker `6`; selected는 1px outline |
| `NyummyAnalysisBanner` | `1042:11` | `Failed/Retrying`; `350×80`, padding vertical `8`/horizontal `10`, gap `8`, radius `16`; Failed에만 `84×48` retry action |
| `NyummyDailyNutritionSummary` | `1043:64` | `Default/Loading/Partial/Failed/Retrying`; `350×112`, radius `20`, 1px nutrition border + Surface Low effect; 3 macro columns, 각 track `96×6` |
| `NyummyMealDetailCard` | `481:87` | `Completed/Analyzing/Failed/Retrying`; `656×306`, radius `16`, 1px card border; photo `176×176`, icon `44`, status pill `138×30`, actions 각 `192×52` |
| `NyummyMealNutritionIndicator` | `1044:66` | `Start/DailyGoal/Final`; `302×326`, nutrient track `302×8`; Start=track, DailyGoal=누적 fill, Final=누적+이번 식사 기여 fill; legend와 `302×82` insight 포함 |
| `NyummyPhotoPicker` | `1046:588` | `Selected/Empty/Uploading/Error/Disabled`; `350×166`, radius `20`; photo `136×136` at `(12,15)`, text x=`166`, button `166×48` at `(166,86)` |

Photo Picker copy/action:

| State | Copy | Action |
|---|---|---|
| Selected | `첨부 완료` | `사진 바꾸기` |
| Empty | `사진을 추가해요` | `사진 선택` |
| Uploading | `사진을 올리고 있어요` | `업로드 중…` |
| Error | `사진을 올리지 못했어요` | `다시 선택` |
| Disabled | `처리 중에는 바꿀 수 없어요` | `사진 바꾸기` |

식사 API의 상태는 `ANALYSIS/COMPLETED/FAILED`다. `Retrying`은 Android 요청/폴링 상태에서 파생한다. 캘린더 `Positive/OutOfRange/NoRecord`는 각각 `POSITIVE/NEGATIVE/UNRECORDED` 표현이며, `None`은 marker 없음이다. `foodIconIds`는 Salad `125:44`, Pasta `125:42`, Rice `125:50` 같은 로컬 pixel asset으로 매핑하지만, 이 세 샘플을 product의 전체 ID 목록으로 고정하지 않는다.

## Preview and catalog contract

- 각 public component는 canonical reference viewport의 전용 `@Preview` 또는 debug catalog fixture 중 적어도 하나에 반드시 포함한다. 각 component 파일에는 Android Studio Layoutlib에서 바로 열 수 있는 대표 `@Preview`를 둔다.
- variant가 8개 이하면 전용 preview 또는 catalog에서 전부 렌더한다. 조합 수가 큰 Button/Input/Calendar Day는 catalog grid와 데이터 기반 Compose 계약 테스트를 함께 사용해 Cartesian product 전체를 렌더·검증한다.
- preview/catalog에는 임의 Material icon, 임의 색, 가짜 navigation destination을 넣지 않는다. canonical asset과 실제 5개 destination을 사용한다.
- preview/catalog는 bundled font와 light-only `DesignSystemTheme`를 사용한다. geometry는 dp reference viewport로 고정하고, emulator screenshot 결과에는 사용한 viewport·density·locale(ko-KR)을 함께 기록한다.
- catalog는 Common, Overlay/State, Navigation, Meal/History 섹션으로 나누고 node ID를 함께 표시한다.

## Verification gates

완료 보고 전 아래를 모두 통과한다.

1. **Contract inventory**: 위 component/state Cartesian product가 빠짐없이 Compose API와 preview/catalog에 존재하는지 검사한다.
2. **Semantics/bounds tests**: Compose UI test로 role, selected/disabled/read-only/loading, content description, clickability와 canonical min/bounds를 확인한다. 최소한 navigation 5 destinations, Button 80 조합의 state mapping, Input 12, State Surface 14, Calendar Day 24를 데이터 기반으로 검사한다.
3. **Screenshot comparison**: canonical Figma node를 같은 viewport로 export하고 Android preview/catalog 또는 emulator screenshot과 나란히 비교한다. color, font baseline, stroke, radius, spacing, icon geometry, shadow offset까지 확인하며 육안 확인 없는 “preview 생성”만으로 통과 처리하지 않는다.
4. **Screen integration**: Figma `10–50`의 대표 화면에서 navigation 순서/선택 indicator, 홈 sheet가 nav를 가리지 않는지, meal/history 상태 매핑을 확인한다.
5. **Architecture review**: raw hex/sp, Material icon 대체, presentation→data 의존, mutable UIState collection, 공통 컴포넌트 내부 product behavior가 없는지 `architecture-guardian`으로 검사하고 HIGH/MEDIUM을 해결한다.
6. **Build/lint**: `./gradlew :app:assembleDebug`와 변경 모듈 lint를 실행한다. Kotlin/Gradle 수정 후에는 빌드 결과 없는 완료 보고를 금지한다.

검증 결과에는 Figma page/root ID, 비교한 Android fixture, 발견한 차이, 수정 후 재검증 결과를 남긴다. Figma 변경으로 값이 달라지면 문서 수치를 수동 보정하지 말고 같은 ID에서 다시 추출해 token/component 계약을 함께 갱신한다.
