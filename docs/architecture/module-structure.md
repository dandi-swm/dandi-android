# 모듈 구조

## 왜

- feature 단위 4-레이어 분리는 **빌드 병렬화**(모듈별 컴파일), **의존 격리**(presentation이 data 구현을 모름), **AI/사람 모두에게 예측 가능한 파일 위치**를 제공한다.
- entity/domain을 순수 Kotlin/JVM으로 유지하면 단위 테스트가 JVM에서 즉시 돌고, 플랫폼 교체(KMP 등)의 여지가 생긴다.

## 구조

```
app                                  실행 가능한 Application (모든 feature 모듈을 implementation)
├─ common/{entity,domain,data,presentation}   공유 베이스 (MviViewModel, BaseUseCase, 테마/토큰, NetworkModule)
├─ main/{entity,domain,data,presentation}     네비게이션 셸 (AppNavHost, AppRouteRegistry, 딥링크)
├─ <feature>/{entity,domain,data,presentation}  기능 단위 (intro / auth / onboarding / home / meal / history /
│                                               mailbox / room / settings / legal / collection / shop / quest / achievement)
├─ cat/{entity,domain,data}          여러 feature가 함께 쓰는 고양이 도메인 (presentation 없음)
├─ tti                               TTI 계측 (순수 JVM)
└─ baselineprofile                   Macrobenchmark / Baseline Profile 수집
```

**공유 도메인 변형 (`cat`)**: 화면 없이 여러 feature가 같은 데이터를 쓸 때는 presentation 없이 entity·domain·data 3모듈만 둔다. 쓰는 쪽 presentation이 `:cat:domain`을 의존한다(지금은 `home`, `room`). 화면이 생기면 그 화면은 이 모듈이 아니라 쓰는 feature 쪽에 둔다.

**의존 방향 (단방향, 위반 금지):**

```
presentation ──▶ domain ──▶ entity
data ──────────▶ domain ──▶ entity
(presentation ↔ data 직접 의존 금지 — domain 인터페이스로만 통신)
```

- `entity`/`domain`/`tti` = 순수 Kotlin/JVM (`kotlin-jvm` 플러그인, Android 의존 금지)
- `data`/`presentation` = `com.android.library` (presentation은 `buildFeatures { buildConfig = true; compose = true }`)
- 모든 모듈 공통: JVM 17, `-Xexplicit-backing-fields`
- Android 모듈(data/presentation) 공통: `compileOptions { isCoreLibraryDesugaringEnabled = true }` + `coreLibraryDesugaring(libs.android.desugar.jdk.libs)` — `java.time`을 쓰므로 빠뜨리면 낮은 API에서 죽는다

## build.gradle.kts 3종 템플릿

새 모듈은 아래 골든 예제를 그대로 복제한다 (버전은 전부 `gradle/libs.versions.toml` 카탈로그 참조):

| 타입 | 골든 예제 | 핵심 |
|---|---|---|
| kotlin-jvm (entity/domain) | [history/entity](../../history/entity/build.gradle.kts), [history/domain](../../history/domain/build.gradle.kts) | `alias(libs.plugins.kotlin.jvm)` + `jvmToolchain(17)`. entity는 `api(project(":common:entity"))` |
| android-library data | [history/data](../../history/data/build.gradle.kts) | `android.library` + `kotlinx.serialization` + `hilt.android` + `ksp`. retrofit/serialization 의존 + desugaring |
| android-library presentation | [history/presentation](../../history/presentation/build.gradle.kts) | `android.library` + `kotlin.compose` + `hilt.android` + `ksp`. `composeCompiler { stabilityConfigurationFiles.add(compose_stability.conf) }` + compose 컴파일러 리포트 출력 + desugaring |

namespace = `com.dandi.nyummy.<feature>.<layer>`.

## 새 feature 배선 (4곳에 추가만, 기존 수정 금지)

1. [settings.gradle.kts](../../settings.gradle.kts) — `include(":<feature>:{presentation,domain,data,entity}")` 4줄
2. [app/build.gradle.kts](../../app/build.gradle.kts) — 4모듈 `implementation(project(...))` 블록
3. [main/presentation/build.gradle.kts](../../main/presentation/build.gradle.kts) — `<feature>:domain`(Page 객체)과 `<feature>:presentation`(Page Composable) `implementation` 2줄. 빠뜨리면 AppRouteRegistry가 컴파일되지 않는다
4. [AppRouteRegistry.kt](../../main/presentation/src/main/java/com/dandi/nyummy/main/presentation/navigation/AppRouteRegistry.kt) — `AppRoute` 항목 (navigation.md 참고)

전체 절차는 `make-new-feature-module` 스킬이 자동화한다.

## compose_stability.conf

[compose_stability.conf](../../compose_stability.conf) — Compose 컴파일러가 stable로 취급할 클래스 등록.

- 지금은 비어 있다. Kotlin 2.x는 strong skipping이 기본이라 불안정한 파라미터도 인스턴스가 같으면 recomposition을 건너뛰므로, 등록하지 않아도 동작에는 문제가 없다.
- 등록 기준: 모든 필드가 `val` + 원시/enum/String인 entity VO(entity는 순수 Kotlin이라 `@Immutable`을 달 수 없다). 프로파일링에서 해당 VO 때문에 불필요한 recomposition이 보일 때 한 줄 추가한다.
- `kotlinx.collections.immutable.*`는 기본 stable이므로 등록 불필요 — UIState 컬렉션은 항상 ImmutableList/ImmutableSet 사용
