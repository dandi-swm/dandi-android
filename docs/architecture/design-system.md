# Nyummy 디자인 시스템 아키텍처

## 기준과 우선순위

- Figma 파일: [Nyummy 냐미](https://www.figma.com/design/olJJrCuFYJypKJCR3RNSn9/Nyummy-%EB%83%90%EB%AF%B8?node-id=984-37241)
- **유일한 Android 디자인 시스템 기준 페이지**: `984:37241` — `02 · LIVE Android Design System · v1.0.0-rc.3`
- 화면 조합과 플로우는 Figma `10–50` 페이지를 따른다. 이 페이지들은 공통 컴포넌트의 토큰·크기·variant를 새로 정의하지 않는다.
- `99` 페이지와 `Legacy`, `Archive`, `Exploration` 표시가 있는 프레임은 구현 기준이 아니다.
- canonical 페이지에 없는 draft/example 프레임도 P0 계약으로 승격되기 전에는 공통 API를 만들 근거로 사용하지 않는다.

충돌 시 현재 사용자 지시 → 위 canonical 페이지 → `10–50` 화면/플로우 → 이 문서 순으로 적용한다. 제품 상태와 데이터 의미는 현재 SRS/API 계약이 우선하며, Figma의 예시 문구나 샘플 데이터로 도메인 enum을 만들지 않는다.

## 계층과 소비 API

```text
Figma primitives / dimensions / text & effect styles
                    ↓
ArchiPaletteColors + DefaultDesignSystemColor + DefaultDesignSystemStaticTypeScale
                    ↓ CompositionLocal
DesignSystemThemeImpl.designSystemColor / typeScale / radius / spacing / size / layout
                    ↓
DandiText + Nyummy P0 Compose components
                    ↓
Feature screen (presentation)
```

| 파일 | 책임 |
|---|---|
| [`ui/token/DesignTokens.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/ui/token/DesignTokens.kt) | palette, semantic color, type-scale 기본값. `FIGMA-TOKEN-INJECTION-POINT` 3개만 `design-token-sync` 자동 동기화 대상으로 삼는다. |
| [`ui/color/ColorSemantic.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/ui/color/ColorSemantic.kt) | Figma semantic color 이름과 1:1인 슬롯 구조. |
| [`ui/typo/DesignSystemTypeScale.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/ui/typo/DesignSystemTypeScale.kt) | 21개 text-style 슬롯 구조. |
| [`ui/theme/DesignSystemTheme.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/ui/theme/DesignSystemTheme.kt) | CompositionLocal 주입, shape/effect 접근, Material3 fallback projection. |
| [`ui/theme/DesignSystemDimensions.kt`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/ui/theme/DesignSystemDimensions.kt) | radius, spacing, size, layout, shadow/effect 값. 자동 token injection 범위 밖이므로 별도 검증한다. |
| [`component/`](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/component/) | canonical P0의 stateless Compose API와 preview/catalog sample. |

제품 UI가 직접 소비할 수 있는 값은 `DesignSystemThemeImpl.*`뿐이다.

```kotlin
DandiText(
    text = label,
    style = DesignSystemThemeImpl.typeScale.textStrongM,
    color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
)

Modifier.background(
    color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
    shape = DesignSystemThemeImpl.designSystemShape.cardDefault,
)
```

- `Color(0x...)`는 palette 정의 외 사용 금지다.
- raw `sp`는 type-scale 정의 외 사용 금지다. 텍스트는 `DandiText`로 그린다.
- product component는 palette가 아니라 semantic color를 참조한다.
- Figma drop shadow는 `radius`, `spread`, `offset`, `color`를 모두 보존한다. blur 반경을 Compose elevation으로 임의 환산하지 않는다.
- 48dp 최소 터치 영역과 disabled/loading semantics는 시각 상태와 별도로 보장한다.

## Figma provenance

### 토큰과 스타일

| 구분 | Figma ID | canonical 계약 |
|---|---|---|
| Color Primitives | `VariableCollectionId:8:2` | 515개 원색 변수. Android semantic/effect가 실제 참조하는 값만 매핑하되 기존 템플릿 palette 항목은 삭제하지 않는다. |
| Semantic Color | `VariableCollectionId:8:3` | 141개 역할 토큰. alias chain을 최종 색으로 평탄화하더라도 슬롯 이름과 역할은 1:1로 보존한다. `bg/scrim/default` 포함. |
| Dimensions | `VariableCollectionId:8:4` | 29개 radius/spacing/size/layout 변수. |
| Typography | canonical local text styles | 21개. style key와 수치는 [구현 명세](../design-system-implementation.md#typography)를 따른다. |
| Effects | canonical local effect styles | 6개. style key와 수치는 [구현 명세](../design-system-implementation.md#effects)를 따른다. |

이름 매핑은 다음 규칙을 사용한다.

| Figma | Kotlin | 예 |
|---|---|---|
| `palette/{family}/{tone}` | `ArchiPaletteColors.{Family}{Tone}` | `palette/forest/600` → `Forest600` |
| `{role}/{variant}/{level}` | `DesignSystemSemanticColors.{role}{Variant}{Level}` | `bg/default/level0` → `bgDefaultLevel0` |
| `{group}/{weight}/{size}` | `DesignSystemTypeScale.{group}{Weight}{Size}` | `text/strong/M` → `textStrongM` |

### Published P0 canonical component roots

아래 노드만 Android 공통 디자인 시스템 v1 P0의 published contract다.

| 영역 | Figma component root |
|---|---|
| Common | Button `990:1052`, IconButton `990:1079`, Input `992:765`, TopAppBar `993:681`, Snackbar `993:690`, Card `993:695`, ListRow `993:699`, Chip `993:728`, Badge `993:739`, LinearProgress `993:753`, Loading Indicator `993:762`, Modal Scrim `993:764` |
| Overlay / state | Dialog `956:563`, Bottom Sheet `956:645`, State Surface `475:62` |
| Navigation | Bottom Navigation `634:11768`, Bottom Navigation Item `619:44573`; icons Home `619:44557`, History `619:44559`, Quest `619:44562`, Collection `619:44565`, Shop `619:44570` |
| Meal / history | Floating Today Meals `884:554`, Meal Row `800:13506`, Calendar Header Action `800:13515`, Calendar Day Cell `844:12774`, Analysis Banner `1042:11`, Daily Nutrition Summary `1043:64`, Meal Detail Card `481:87`, Meal Nutrition Indicator `1044:66`, Photo Picker `1046:588` |
| Assets | Salad `125:44`, Pasta `125:42`, Rice `125:50`, Chevron Left `22:54`, Chevron Right `22:58` |

노드별 variant, 상태, geometry는 [Nyummy Android Design System 구현 명세](../design-system-implementation.md)에 고정한다. P1 또는 향후 컴포넌트는 canonical 페이지에 root와 상태 계약이 추가된 뒤 별도 작업으로 승격한다.

## 제품 상태와 디자인 variant의 경계

- 식사 분석 API 상태는 `ANALYSIS`, `COMPLETED`, `FAILED` 세 값이다.
- `Retrying`은 재분석 요청/폴링 중인 UI 파생 상태이지 새로운 서버 enum이 아니다.
- 히스토리는 분석 중/실패 식사를 목록에서 제거하지 않는다.
- 캘린더의 영양 평가는 `POSITIVE`, `NEGATIVE`, `UNRECORDED`를 UI variant `Positive`, `OutOfRange`, `NoRecord`에 매핑한다. `None`은 marker를 숨기는 표현 상태다.
- 캘린더와 목록은 실제 사진 대신 `foodIconIds`를 로컬 픽셀 아트 asset에 매핑한다. unknown ID fallback과 노출 개수 정책은 product contract에서 결정하며 Figma 샘플 3개를 전체 ID 체계로 간주하지 않는다.
- 홈 Bottom Sheet는 Bottom Navigation을 가리지 않고 그 위에서 열린다.

## 변경 절차

1. canonical page와 component root ID를 확인한다.
2. color/type 변경은 `design-token-sync`로 variable/style을 추출하고, injection marker 안만 갱신한다.
3. 새 semantic/type 슬롯이 필요하면 인터페이스, `withStringKey`, 기본값, consumer를 한 변경으로 맞춘다.
4. dimensions/effects/components는 canonical node의 bound variable, geometry, variant 전체를 별도로 추출한다.
5. public Compose API는 stateless를 기본으로 하고, interaction은 callback으로 노출한다. 임의 product behavior를 공통 컴포넌트에 넣지 않는다.
6. [검증 게이트](../design-system-implementation.md#verification-gates)를 전부 통과한 뒤 계약 버전을 올린다.

## 안정성

VO를 Compose 파라미터로 전달하면 [`compose_stability.conf`](../../compose_stability.conf)에 등록한다. UIState 컬렉션은 `ImmutableList`/`ImmutableSet`을 사용한다. 공통 컴포넌트의 상태 모델은 immutable enum/data class로 두고, caller가 소유하는 상태를 내부에서 복제하지 않는다.
