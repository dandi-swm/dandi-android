# Copilot 지침 — Dandi (Android)

## 리뷰 언어
- 모든 코드 리뷰 코멘트와 요약(summary)은 **한국어**로 작성한다.
- 코드 예시/식별자는 원문 그대로 두되, 설명 문장은 한국어로 쓴다.

## 프로젝트 개요
멀티모듈 클린아키텍처 Android 프로젝트. feature 는 `entity / domain / data / presentation` 4모듈로 구성된다.

## 리뷰 시 중점 확인 (불변 규칙 — 위반 시 지적)
- **의존 방향**: `presentation → domain → entity`, `data → domain → entity`. presentation↔data 직접 의존 금지. `entity`/`domain` 은 순수 Kotlin/JVM (Android 의존 금지).
- **MVI**: View 진입점은 `onIntent(Intent)` 하나. 상태 변이는 `dispatch(ReducerEvent) → reduce()` 한 곳에서만. UIState 컬렉션은 `ImmutableList`/`ImmutableSet`.
- **디자인 토큰**: 색은 `DesignSystemThemeImpl.designSystemColor.*`, 타이포는 `DesignSystemThemeImpl.typeScale.*`(`DandiText` 경유)만 사용. raw hex / raw sp 금지.
- **DTO/VO**: DTO 는 `@Serializable` + 전 필드 nullable, VO 는 비-nullable + 기본값. 변환은 data 레이어의 `toVO()` 에서만.
- **에러 처리**: data 는 `HttpResponseException` throw 만. 다이얼로그/네비게이션/스낵바 등 처리는 domain UseCase 에서 `isCommonErrorHandling()` / `handlingErrorOnUseCase<ErrorType>()` 로.
- **네비게이션**: 화면 이동은 `navigationHelper.navigateTo(Page)` 만. 새 화면은 `AppRouteRegistry.kt` 에 등록.
- **디자인 시스템 컴포넌트 재사용**: 새 Composable 컴포넌트를 추가하기 전에 `common:presentation` 의 기존 디자인 시스템 컴포넌트(`Dandi*`, `Nyummy*` — 예: `DandiText`, `NyummyButton`, `NyummyBadge`, `NyummyMealRow`, `NyummyLinearProgress`, `NyummyModalScrim`, `NyummyEditDialog` 등)로 대체 가능한지 먼저 확인한다. 이미 있는 버튼/텍스트/카드/다이얼로그/프로그레스 등을 feature 모듈에서 새로 만들면 **지적하고 기존 공용 컴포넌트 재사용을 제안**한다. 공용 컴포넌트로 부족하면 임의 재구현 대신 공용 컴포넌트 확장/추가를 권한다.

## 리뷰 톤
- 근거 없는 스타일 취향 지적은 지양하고, 위 불변 규칙 위반·버그·널 안정성·동시성 문제를 우선한다.
- 사소한 제안은 nit: 접두사를 붙인다.

## 커밋/PR 규칙
- 한국어, Conventional Commits 형식(`type: 제목`).
