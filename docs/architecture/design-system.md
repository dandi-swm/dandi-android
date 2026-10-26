# Nyummy 디자인 시스템 아키텍처

## 기준과 우선순위

- Figma 파일: [Nyummy 냐미](https://www.figma.com/design/olJJrCuFYJypKJCR3RNSn9/Nyummy-%EB%83%90%EB%AF%B8)
- 디자인 시스템 기준 페이지는 `01 Foundations`(색, 글자, 간격, 모서리, 그림자)부터 `03 Components`(공통 컴포넌트)까지다.
- 화면 조합과 플로우는 그 뒤 번호의 화면 페이지를 따른다. 화면 페이지는 공통 컴포넌트의 토큰, 크기, variant를 새로 정의하지 않는다.
- `Legacy`, `Archive`, `Exploration` 표시가 있는 프레임은 구현 기준이 아니다.

충돌 시 현재 사용자 지시 → 위 디자인 시스템 페이지 → 화면 페이지 → 이 문서 순으로 적용한다. 제품 상태와 데이터 의미는 현재 SRS/API 계약이 우선하며, Figma의 예시 문구나 샘플 데이터로 도메인 enum을 만들지 않는다.

## 계층과 소비 API

```text
Figma variables (palette / 2 Semantic / 3 Dimension) + text & effect styles
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
| [`token/NyummyPalette.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/token/NyummyPalette.kt) | 원색 팔레트(Gray, Evergreen, Gold, Berry, Sky × 10단계, 외부 브랜드 색). `internal`이라 화면에서 직접 못 쓴다. |
| [`theme/NyummyColors.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/theme/NyummyColors.kt) | 역할 색. `bg`, `content`, `border`, `data`, `external` 그룹. Figma `2 Semantic`과 1:1. |
| [`theme/NyummyTypography.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/theme/NyummyTypography.kt) | 글자 역할 20종(display, title, body, label, voice, number)과 글꼴 4종. |
| [`theme/NyummyDimensions.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/theme/NyummyDimensions.kt) | 간격(`s2`~`s48`, `gutter`), 모서리(`xs`~`l`, `full`), 선 두께, 크기(터치 영역, 아이콘, 캐릭터, 버튼, 하단 내비). Figma `3 Dimension`과 1:1. |
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

### 글꼴

| 역할 | 글꼴 | 쓰는 곳 |
|---|---|---|
| Ui | Asta Sans | title, body, label |
| Display | Jua | display(큰 제목) |
| Voice | Gowun Dodum | 냐미 말풍선 |
| Number | Nunito | 숫자 |

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
