# 디자인 시스템 & Figma 토큰 매핑

## 왜

- 색/타이포를 **시맨틱 토큰 슬롯**으로만 소비하면, 디자인 시스템 교체(Figma 토큰 갱신)가 `DesignTokens.kt` 한 파일 수정으로 끝난다 — 화면 코드는 무수정.
- 팔레트(원색) → 시맨틱(역할) 2단 구조라 "같은 회색이 배경과 보더에 쓰였는데 배경만 바꾸고 싶다"가 가능하다.

## 구조 (3단)

```
ArchiPaletteColors (원색 팔레트)          ─┐
DefaultDesignSystemColor : DesignSystemSemanticColors    ├─ 기본값: ui/token/DesignTokens.kt (단일 수정 지점)
DefaultDesignSystemStaticTypeScale : DesignSystemTypeScale ─┘
        ↓ CompositionLocal 주입 (DesignSystemTheme)
DesignSystemThemeImpl.designSystemColor.* / DesignSystemThemeImpl.typeScale.*   ← 화면 코드가 소비하는 유일한 API
```

| 파일 | 역할 |
|---|---|
| [ui/token/DesignTokens.kt](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/ui/token/DesignTokens.kt) | **토큰 기본값 전부.** `FIGMA-TOKEN-INJECTION-POINT` 마커 3개(palette / semantic-colors / type-scale) — `design-token-sync` 스킬의 유일한 수정 대상 |
| [ui/color/ColorSemantic.kt](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/ui/color/ColorSemantic.kt) | 슬롯 구조 `DesignSystemSemanticColors` (bg/border/content × level + accent/error) |
| [ui/typo/DesignSystemTypeScale.kt](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/ui/typo/DesignSystemTypeScale.kt) | 타이포 슬롯 (titleStrongL, textRegularM, ...) |
| [ui/theme/DesignSystemTheme.kt](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/ui/theme/DesignSystemTheme.kt) | CompositionLocal 주입 + Material3 colorScheme 폴백 매핑 |

## Figma ↔ 코드 매핑 명세 (디자이너와 공유)

| Figma | 코드 | 예 |
|---|---|---|
| Variable `palette/{name}/{step}` | `ArchiPaletteColors.{Name}{Step}` | `palette/gray/900` → `Gray900` |
| Variable `{role}/{variant}/{level}` | `DesignSystemSemanticColors.{role}{Variant}{Level}` | `bg/default/level0` → `bgDefaultLevel0`, `content/accent` → `contentAccent` |
| Text Style `{group}/{weight}/{size}` | `DesignSystemTypeScale.{group}{Weight}{Size}` | `title/strong/L` → `titleStrongL` |

Figma 파일이 이 네이밍을 따르면 `design-token-sync` 스킬이 100% 자동 매핑한다. 따르지 않으면 스킬이 역할 기반 추정 후 매핑 리포트로 확인을 요청한다.

## 소비 규칙 (화면 코드)

```kotlin
DandiText(
    text = ...,
    style = DesignSystemThemeImpl.typeScale.textStrongM,       // raw sp 금지
    color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,  // raw hex 금지
)
Modifier.background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1)
```

- 텍스트는 [DandiText](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/component/Text.kt) 경유 (Material Text 직접 사용 지양)
- 시맨틱 슬롯은 팔레트만 참조 — `Color(0xFF...)` 리터럴은 DesignTokens.kt 팔레트 정의에만 존재 가능
- 공통 컴포넌트 우선 재사용: DandiText / BackArrowButton ([component/](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/component/)). `FavoriteHeartButton` / `ContentsList` / `MediaSearchBar`는 레거시 컴포넌트 예시이며 현재 소스 경로를 뜻하지 않는다.

## 슬롯 구조 변경 (신중하게)

새 시맨틱 슬롯(예: `contentWarning`)이 필요하면 4곳을 함께 수정: ① `DesignSystemSemanticColors` 필드 ② 그 안의 `withStringKey` ③ `DesignTokens.kt` 기본값 ④ (타이포면) `DesignSystemTypeScale` + `withStringKey`. — 슬롯 추가는 사용자 확인 후 진행이 원칙 (`design-token-sync` 스킬 §2.4).

## compose_stability 정책

VO를 Compose 파라미터로 쓰면 [compose_stability.conf](../../compose_stability.conf)에 등록 (module-structure.md 참고). UIState 컬렉션은 Immutable* 사용으로 등록 불필요.
