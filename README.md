# Dandi Android

Dandi Android의 공통 아키텍처 기반 저장소입니다. 현재는 제품 기능을 담은 feature 모듈이나 화면 라우트가 없으며, 앱 셸·공통 계층·성능 계측 기반만 유지합니다. 새 기능은 이 기반 위에 4개 모듈로 추가합니다.

> 현재 [AppRouteRegistry.kt](main/presentation/src/main/java/com/dandi/nyummy/main/presentation/navigation/AppRouteRegistry.kt)의 appRoutes는 빈 목록입니다. 문서의 feature 구조와 라우트 예시는 새 기능에 적용할 계약이지, 구현 완료된 화면 목록이 아닙니다.

## 현재 모듈

| 모듈 | 역할 |
|---|---|
| :app | Application, Manifest, 전체 Hilt 그래프 조립 |
| :common:entity | 공통 순수 Kotlin 값과 상수 |
| :common:domain | BaseUseCase, 오류·메시지·네비게이션 계약 |
| :common:data | Retrofit/OkHttp/Json과 BaseRemoteDataSource |
| :common:presentation | MVI 기반, 디자인 시스템, 공통 UI, JankStats 연결 |
| :main:entity / :main:domain / :main:data / :main:presentation | Navigation3 앱 셸과 딥링크 매칭 기반 |
| :tti | 플랫폼 비의존 TTI 계측 |
| :baselineprofile | Baseline Profile 수집과 콜드 스타트 Macrobenchmark |

등록 상태의 단일 기준은 [settings.gradle.kts](settings.gradle.kts)입니다.

새 feature는 반드시 다음 의존 방향의 4개 모듈로 만듭니다.

~~~text
<feature>:presentation ──▶ <feature>:domain ──▶ <feature>:entity
<feature>:data ──────────▶ <feature>:domain ──▶ <feature>:entity
~~~

presentation과 data는 서로 직접 의존하지 않습니다. entity와 domain은 Android에 의존하지 않는 Kotlin/JVM 모듈입니다.

## 기술 기반

| 영역 | 현재 구성 |
|---|---|
| 언어·빌드 | Kotlin 2.3.21, AGP 9.2.1, JDK 17 |
| Android | compileSdk/targetSdk 37, minSdk 24 |
| UI | Jetpack Compose, Material3, Navigation3 |
| DI | Hilt + KSP |
| 비동기 | Coroutines, Flow |
| 네트워크 | Retrofit 3, OkHttp 5, kotlinx.serialization |
| 상태 안정성 | kotlinx.collections.immutable, Compose stability 설정 |
| 성능 | TTI, AndroidX JankStats, Macrobenchmark, Baseline Profile |

버전의 단일 기준은 [gradle/libs.versions.toml](gradle/libs.versions.toml)입니다.

## 아키텍처 문서

| 문서 | 내용 |
|---|---|
| [모듈 구조](docs/architecture/module-structure.md) | 현재 모듈과 새 feature 배선 |
| [MVI](docs/architecture/mvi.md) | Intent, UIState, ReducerEvent, ViewModel 계약 |
| [Navigation3](docs/architecture/navigation.md) | Page/NavRoute, 레지스트리, 딥링크와 백스택 |
| [데이터 레이어](docs/architecture/data-layer.md) | DTO/VO, BaseRemoteDataSource, Retrofit/Hilt |
| [에러 핸들링](docs/architecture/error-handling.md) | HttpResponseException과 BaseUseCase 처리 경계 |
| [디자인 시스템](docs/architecture/design-system.md) | Figma 토큰, 색상·타이포 소비 규칙 |
| [성능 계측](docs/architecture/performance.md) | TTI, JankStats, Baseline Profile |

디자인 스펙에서 기능을 구현할 때는 [디자인 스펙 → 코드 가이드](docs/DESIGN_TO_CODE_GUIDE.md)를 먼저 확인합니다.

## 핵심 계약

- 화면 입력은 ViewModel의 onIntent(Intent) 하나로 들어갑니다.
- 상태는 dispatch(ReducerEvent) → reduce()에서만 변경합니다.
- UIState 컬렉션은 ImmutableList 또는 ImmutableSet을 사용합니다.
- 화면 이동은 NavigationHelper.navigateTo(Page)로 요청하고, 모든 렌더 라우트는 AppRouteRegistry에 등록합니다.
- DTO는 @Serializable이며 모든 필드를 nullable로 둡니다. VO는 비-nullable 기본값을 가지며 변환은 data의 toVO()에서만 수행합니다.
- HTTP 실패는 data에서 HttpResponseException으로 올리고, 공통/기능별 처리는 domain UseCase가 담당합니다.
- 화면의 색상과 타이포는 DesignSystemThemeImpl 토큰과 DandiText를 사용합니다. raw hex와 raw sp를 화면 코드에 두지 않습니다.

## 로컬 설정

[common/data/build.gradle.kts](common/data/build.gradle.kts)가 아래 값을 local.properties에서 BuildConfig로 주입합니다.

~~~properties
API_KEY=
API_BASE_URL=https://example.com/
~~~

저장소에는 실제 키를 기록하지 않습니다. 값이 없으면 빌드 가능한 placeholder가 사용되므로, 네트워크 기능 검증 전에는 환경별 값을 주입해야 합니다.

## 빌드와 테스트

시스템 JDK가 없으면 Android Studio의 JBR을 지정합니다.

~~~bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
./gradlew test
~~~

Kotlin/Gradle 코드를 수정한 작업은 최소 :app:assembleDebug 성공 후 종료합니다. PR 전에는 다음 검증도 권장합니다.

~~~bash
./gradlew :app:lintDebug
~~~

성능 수집은 API 28 이상 디바이스 또는 에뮬레이터가 필요합니다.

~~~bash
./gradlew :app:generateBaselineProfile
./gradlew :baselineprofile:connectedBenchmarkAndroidTest
~~~

현재 Baseline Profile 시나리오는 앱 콜드 스타트만 수집합니다. 실제 핵심 사용자 흐름이 등록되면 시나리오와 검증 조건도 함께 확장해야 합니다.

## 현재 기반의 의도된 빈 지점

- appRoutes가 비어 있어 등록된 화면과 유효한 딥링크 경로가 없습니다. URI 없는 콜드 스타트의 기본 화면 정책도 아직 비어 있습니다.
- common:data에는 공용 네트워크 구성만 있고 기능별 ApiService/DTO/Repository는 없습니다.
- TTIReporter와 release TTI/Jank sink는 외부 관측 도구가 연결되기 전까지 no-op입니다.
- 새 기능은 [make-new-feature-module 스킬](.agents/skills/make-new-feature-module/SKILL.md)로 4모듈 구조를 만든 뒤 라우트와 앱 의존성을 추가합니다.
