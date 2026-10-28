# Nyummy 디자인 시스템 아키텍처

## 기준과 우선순위

- Figma 파일: [Nyummy 냐미](https://www.figma.com/design/olJJrCuFYJypKJCR3RNSn9/Nyummy-%EB%83%90%EB%AF%B8)
- 디자인 시스템 기준 페이지는 `v2 · 01 Foundations`(색, 글자, 간격, 모서리)와 `v2 · 03 Components`(공통 컴포넌트)다.
- 화면 조합과 플로우는 그 뒤 번호의 `v2 · ` 화면 페이지를 따른다. 화면 페이지는 공통 컴포넌트의 토큰, 크기, variant를 새로 정의하지 않는다.
- `99 · LEGACY Archive` 페이지와 `_legacy /` 변수·텍스트 스타일, `Legacy`, `Archive`, `Exploration` 표시가 있는 프레임은 구현 기준이 아니다.

충돌 시 현재 사용자 지시 → 위 디자인 시스템 페이지 → 화면 페이지 → 이 문서 순으로 적용한다. 제품 상태와 데이터 의미는 현재 SRS/API 계약이 우선하며, Figma의 예시 문구나 샘플 데이터로 도메인 enum을 만들지 않는다.

## 계층과 소비 API

```text
Figma variables (Nyummy v2 / 1 Primitive · 2 Semantic · 3 Dimension · 4 Typography) + v2/ text styles
                    ↓
NyummyPalette (internal) → NyummyColors, NyummyTypography, NyummyDimensions, NyummyElevation
                    ↓ CompositionLocal
NyummyTheme.colors / typography / spacing / radius / borderWidth / size / elevation
                    ↓
Nyummy* 공통 컴포넌트 (designsystem/component)
                    ↓
Feature screen (presentation)
```

코드는 모두 `common/presentation/.../designsystem/` 아래에 있다.

| 경로 | 책임 |
|---|---|
| [`token/NyummyPalette.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/token/NyummyPalette.kt) | 원색 팔레트(회색 사다리, Evergreen, Red, Orange, Gold, 장면 색, 외부 브랜드 색). Figma `Nyummy v2 / 1 Primitive`와 1:1이고, 구글·네이버 색만 소셜 로그인 가이드 때문에 더 둔다. `internal`이라 화면에서 직접 못 쓴다. |
| [`theme/NyummyColors.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/theme/NyummyColors.kt) | 역할 색. `bg`, `content`, `border`, `nav`, `data`, `external` 그룹. Figma `Nyummy v2 / 2 Semantic`과 1:1. |
| [`theme/NyummyTypography.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/theme/NyummyTypography.kt) | 글자 역할 20종(display, title, body, label, voice, number)과 글꼴 2종. Figma `v2/` 텍스트 스타일과 1:1. |
| [`theme/NyummyDimensions.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/theme/NyummyDimensions.kt) | 간격(`s2`~`s48`, `gutter`), 모서리(`xs`, `s`, `m`, `ml`, `l`, `full`), 선 두께, 크기(터치 영역, 아이콘, 캐릭터, 버튼, 하단 내비, 섹션 띠). Figma `Nyummy v2 / 3 Dimension`과 1:1. |
| [`theme/NyummyElevation.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/theme/NyummyElevation.kt) | 그림자 `soft`(일러스트 위 카드), `float`(시트, 떠 있는 요소). 버튼과 카드는 그림자를 쓰지 않는다. |
| [`theme/NyummyTheme.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/theme/NyummyTheme.kt) | 진입점. CompositionLocal 주입과, Material3 기본값이 냐미 토큰을 따르도록 하는 `MaterialTheme` 대응. |
| [`foundation/`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/foundation/) | 컴포넌트 내부 전용 고정 치수(`NyummyComponentDimens`), 누름 효과(`nyummyClickable`), 키보드 처리. |
| [`component/`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/component/) | 공통 컴포넌트(`NyummyButton`, `NyummyText`, `NyummySheet`, `NyummyPoseImage`, 스프라이트 재생 등). |

`NyummyTheme { }`은 `MainActivity`에서 한 번 감싼다. 화면은 별칭 없이 `NyummyTheme.colors.x`처럼 직접 읽는다.

```kotlin
NyummyText(
    text = label,
    style = NyummyTheme.typography.labelM,
    color = NyummyTheme.colors.content.secondary,
)

Modifier
    .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.m))
    .padding(horizontal = NyummyTheme.spacing.gutter)
```

- `Color(0x...)`는 `NyummyPalette` 밖에서 쓰지 않는다. 화면과 컴포넌트는 역할 색만 쓴다.
- raw `sp`는 `NyummyTypography` 밖에서 쓰지 않는다. 텍스트는 `NyummyText`로 그린다.
- 그림자는 `NyummyTheme.elevation`의 blur, offset, 색을 그대로 쓴다. Compose elevation으로 임의 환산하지 않는다.
- 컴포넌트에만 필요한 고정 치수는 토큰에 넣지 않고 `NyummyComponentDimens`에 둔다.
- 48dp 최소 터치 영역과 disabled/loading semantics는 시각 상태와 별도로 보장한다.
- 그리기 블록(`Canvas`, `drawBehind`) 안에서는 `NyummyTheme`을 읽을 수 없으니 블록 밖에서 값을 꺼내 둔다.

### 색과 강약

굵기는 "제목·값인지, 내용인지"를 나누고, 색은 "중요한지, 부가 정보인지"를 나눈다.

| 글자색 | 흰 바탕 대비 | 쓰는 곳 |
|---|---|---|
| `content.primary` | 15.1:1 | 제목, 이름, 값, 기록 있는 날짜 |
| `content.secondary` | 5.6:1 | 라벨, 설명, 안내·오류 문구 |
| `content.tertiary` | 3.1:1 | 메타: 부제, 시각, 끼니 태그, 전체보기 |
| `content.quiet` | 2.5:1 | 없음·지나감: 기록 없는 날짜, 할인 전 가격, "이미 받았어요" |
| `content.disabled` | 1.8:1 | 요일 머리, 앞으로 올 날, 다른 달 날짜 |
| `content.brand` | 4.4:1 | 문장 속 핵심 숫자, 켜진 탭 |
| `content.voice` | 13.3:1 | 냐미 말투, 코인 숫자 |
| `content.danger` | 4.1:1 | 할인율, 삭제 |

1. 오류, 안내, 눌러야 하는 글자는 `secondary`보다 연하게 쓰지 않는다.
2. `tertiary`는 없어도 화면을 이해할 수 있는 부가 정보에만 쓴다.
3. `quiet`는 "없음·지나감"을 나타낼 때만 쓴다.
4. 문장 안에서는 핵심 숫자만 `brand` Bold로 강조한다("이번 달 **4일** 기록했어요!"). `brand`와 `danger`를 작은 글자에 쓸 때는 Bold로만 쓴다.
5. 상태색(`brand`, `danger`, `promo`)은 뜻이 있을 때만 쓴다.

- 버튼, 칩, 썸에 검정(`content.primary` 같은 짙은 회색) 채움을 쓰지 않는다. 주 행동은 `bg.actionPrimary`, 보조는 `bg.actionTonal` 또는 `bg.actionNeutral`이다.
- 버튼 위계는 `NyummyButtonStyle`의 Primary(초록 채움) > Tonal(연초록, 목록 행 안 행동) > Neutral(회색) > Text(배경 없음) 순이다. Danger(빨강)는 삭제·탈퇴 확정에만 쓴다. 크기는 L(56)과 S(32) 두 가지이고, 눌리면 덮개 대신 위계마다 정한 눌림색으로 바뀐다.
- 화면 하단 고정 버튼은 `NyummyBottomCta`다. 흰 바탕에 위로 옅은 그림자를 드리우고, 버튼 위에 짧은 안내(tertiary)를 둘 수 있다.
- 계약 테스트는 역할 묶음마다 대비 하한을 둔다. 본문급 4.5, 굵은 강조·버튼 글자 3.9, 메타·태그 2.9, 없음·지나감 2.4이고 `disabled`는 검사하지 않는다. 하한보다 낮은 조합이 나오면 테스트를 낮추지 말고 디자인을 고친다.

### 글꼴

| 역할 | 글꼴 | 쓰는 곳 |
|---|---|---|
| Ui | Pretendard Medium·Bold | title, body, label. 굵기는 두 가지만 쓴다 |
| Accent | AndongKaturi(엄마까투리체) | number(숫자), voice(냐미 말투), display(브랜드 문구와 연출 무대 글자) |

- 화면 제목은 `titleXl`(Pretendard 24 Bold)이다. `display`를 화면 제목에 쓰지 않는다.
- 자간은 Pretendard 글자와 voice가 -3%, 숫자와 display는 0이다. 줄 높이는 피그마 글자 상자 높이를 그대로 고정한다.
- AndongKaturi는 한글이 2,350자뿐이다. accent 스타일 문장에 이 글꼴에 없는 글자가 있으면 `NyummyText`가 문장 전체를 같은 크기의 Pretendard Medium으로 그린다.
- 출처: Pretendard는 SIL OFL 1.1, 엄마까투리체는 안동시 공공저작물이다. 앱 안 고지는 설정 > 약관 > 글꼴 출처에 둔다.

### 캐릭터와 아이콘

- 아이콘은 Lucide 벡터(`nyummy_ic_*`)만 쓴다. 이모지나 직접 그린 도형으로 대신하지 않는다.
- 냐미 정지 일러스트는 `NyummyPose` + `NyummyPoseImage`(밀도별 WebP)로 그린다. 포즈별 쓰임은 `NyummyPose` 문서 주석을 따른다.
- 픽셀 스프라이트 애니메이션은 `NyummySpriteView`, `NyummySpriteSequenceView`로 재생한다. 스프라이트 시트만 프레임 계산을 위해 `drawable-nodpi`에 둔다.

## 제품 상태와 디자인 variant의 경계

- 식사 분석 서버 상태는 `WAITING`, `ANALYZING`, `COMPLETED`, `FAILED` 네 값이고, 그 밖의 값은 앱에서 `UNKNOWN`으로 받는다([MealAnalysisStatusParser.kt](../../history/data/src/main/java/com/dandi/nyummy/history/data/util/MealAnalysisStatusParser.kt), [MealAnalysisStatus.kt](../../history/entity/src/main/java/com/dandi/nyummy/history/entity/MealAnalysisStatus.kt)).
- 재분석 중 표시는 UI 파생 상태(`HistoryUIState.isReanalyzing`)이지 새로운 서버 enum이 아니다.
- 히스토리는 분석 중/실패 식사를 목록에서 제거하지 않는다.
- 캘린더는 영양 평가를 그리지 않고, 그날 기록이 있으면 기록 점(`colors.data.recordMarker`)만 찍는다(`HistoryCalendar`의 `hasRecord`).
- 캘린더와 목록은 실제 사진 대신 `foodIconIds`로 CDN 이미지(`NyummyCdn.foodIconUrl` → `https://cdn.nyummy.co.kr/icons/{iconId}.png`)를 내려받아 표시한다. 아이콘은 분석이 끝난 식사에만 쓴다(서버는 분석 전이거나 실패한 식사에도 기본 iconId 1을 준다). iconId가 비어 있거나 이미지를 받지 못하면 기본은 아무것도 그리지 않고, 자리를 채워야 하는 곳은 `NyummyFoodIcon`의 `placeholder` 슬롯을 넘긴다.

## 변경 절차

1. Figma 디자인 시스템 페이지에서 바뀐 변수, 글자 스타일, 컴포넌트를 확인한다.
2. 색, 글자, 간격 값이 바뀌면 `token/`, `theme/` 파일을 고치고, 같은 변경에서 계약 테스트(`NyummyTokenContractTest`, `NyummyTypographyContractTest`)의 기대값도 Figma 값으로 맞춘다.
3. 새 역할 색이나 글자 역할이 필요하면 데이터 클래스, 기본값, 사용처를 한 변경으로 맞춘다.
4. 컴포넌트는 Figma 컴포넌트의 variant와 상태를 모두 옮기고, 공개 API는 stateless를 기본으로 한다. interaction은 callback으로 노출하고 화면 고유 동작은 공통 컴포넌트에 넣지 않는다.
5. 디버그 카탈로그(`NyummyDsCatalogActivity`)에 섹션을 추가하고, Compose UI 테스트(`common/presentation/src/androidTest/.../designsystem/component/`)를 함께 둔다.

디버그 카탈로그 실행:

```bash
adb shell am start -n com.dandi.nyummy/com.dandi.nyummy.main.presentation.catalog.NyummyDsCatalogActivity --es section "Bottom Nav"
```

## 안정성

VO를 Compose 파라미터로 전달하면 [`compose_stability.conf`](../../compose_stability.conf)에 등록한다. UIState 컬렉션은 `ImmutableList`/`ImmutableSet`을 사용한다. 공통 컴포넌트의 상태 모델은 immutable enum/data class로 두고, caller가 소유하는 상태를 내부에서 복제하지 않는다.
