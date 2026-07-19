# 모듈 구조

## 왜

- feature 단위 4-레이어 분리는 **빌드 병렬화**(모듈별 컴파일), **의존 격리**(presentation이 data 구현을 모름), **AI/사람 모두에게 예측 가능한 파일 위치**를 제공한다.
- entity/domain을 순수 Kotlin/JVM으로 유지하면 단위 테스트가 JVM에서 즉시 돌고, 플랫폼 교체(KMP 등)의 여지가 생긴다.

## 구조

> `intro`/`search`/`favorite`/`fullScreenMedia`는 원본 구조를 설명하기 위한 레거시 feature 예시이며, 현재 저장소에 존재하는 모듈 경로가 아닙니다.

```
app                                  실행 가능한 Application (모든 feature 모듈을 implementation)
├─ common/{entity,domain,data,presentation}   공유 베이스 (MviViewModel, BaseUseCase, 테마/토큰, NetworkModule)
├─ main/{entity,domain,data,presentation}     네비게이션 셸 (AppNavHost, AppRouteRegistry, 딥링크)
├─ <feature>/{entity,domain,data,presentation}  기능 단위 (intro / search / favorite / fullScreenMedia)
├─ tti                               TTI 계측 (순수 JVM)
└─ baselineprofile                   Macrobenchmark / Baseline Profile 수집
```

**의존 방향 (단방향, 위반 금지):**

```
presentation ──▶ domain ──▶ entity
data ──────────▶ domain ──▶ entity
(presentation ↔ data 직접 의존 금지 — domain 인터페이스로만 통신)
```

- `entity`/`domain`/`tti` = 순수 Kotlin/JVM (`kotlin-jvm` 플러그인, Android 의존 금지)
- `data`/`presentation` = `com.android.library` (presentation은 대개 compose 활성 — 레거시 `fullScreenMedia` 예시는 ViewBinding Fragment 화면이라 `kotlin.compose`/`composeCompiler {}` 없이 `viewBinding = true`)
- 모든 모듈 공통: JVM 17, `-Xexplicit-backing-fields`

## build.gradle.kts 3종 템플릿

새 모듈은 아래 골든 예제를 그대로 복제한다 (버전은 전부 `gradle/libs.versions.toml` 카탈로그 참조):

| 타입 | 골든 예제 | 핵심 |
|---|---|---|
| kotlin-jvm (entity/domain) | entity/domain `build.gradle.kts` (레거시 intro 예시) | `alias(libs.plugins.kotlin.jvm)` + `jvmToolchain(17)`. entity는 `api(project(":common:entity"))` |
| android-library data | data `build.gradle.kts` (레거시 intro 예시) | `android.library` + `kotlinx.serialization` + `hilt.android` + `ksp`. retrofit/serialization 의존 |
| android-library presentation | presentation `build.gradle.kts` (레거시 intro 예시) | `android.library` + `kotlin.compose` + `hilt.android` + `ksp`. `composeCompiler { stabilityConfigurationFiles += compose_stability.conf }` |

namespace = `com.dandi.nyummy.<feature>.<layer>`.

## 새 feature 배선 (3곳에 추가만, 기존 수정 금지)

1. [settings.gradle.kts](../../settings.gradle.kts) — `include(":<feature>:{presentation,domain,data,entity}")` 4줄
2. [app/build.gradle.kts](../../app/build.gradle.kts) — 4모듈 `implementation(project(...))` 블록
3. [AppRouteRegistry.kt](../../main/presentation/src/main/java/com/dandi/nyummy/main/presentation/navigation/AppRouteRegistry.kt) — `AppRoute` 항목 (navigation.md 참고)

전체 절차는 `make-new-feature-module` 스킬이 자동화한다.

## compose_stability.conf

[compose_stability.conf](../../compose_stability.conf) — Compose 컴파일러가 stable로 취급할 클래스 등록.

- 등록 기준: 모든 필드가 `val` + 원시/enum/String인 entity VO
- `kotlinx.collections.immutable.*`는 기본 stable이므로 등록 불필요 — UIState 컬렉션은 항상 ImmutableList/ImmutableSet 사용
- 새 VO를 Compose 파라미터로 쓰면 여기에 한 줄 추가
