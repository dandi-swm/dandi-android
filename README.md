# 냐미(Nyummy) Android

단디(Dandi) 팀의 AI 식사 기록 앱 **냐미(Nyummy)** 안드로이드 저장소입니다. 사진 한 장으로 식사를 기록하면(원샷 촬영) AI가 영양 정보를 분석해 참고용으로 보여주고, 고양이 캐릭터 냐미를 돌보는 경험으로 기록 습관을 만드는 앱입니다. 멀티모듈 클린 아키텍처(4레이어) + MVI + Navigation3 기반이며, 새 기능은 feature당 4개 모듈로 추가합니다.

> 2026-08-31 기준 현행화. 초기의 "빈 아키텍처 템플릿" 서술(appRoutes 빈 목록, feature 모듈 없음)은 폐기되었습니다. 아래 표가 실제 구현 상태입니다.

## 현재 모듈 (총 31개)

| 모듈 | 역할 | 상태 |
|---|---|---|
| :app | Application, Manifest, 전체 Hilt 그래프 조립 | 동작 |
| :common:entity / :common:domain / :common:data / :common:presentation | 공통 값·상수, BaseUseCase·오류·네비게이션 계약, Retrofit/OkHttp/Json·BaseRemoteDataSource, MVI 기반·디자인 시스템·JankStats | 동작 |
| :main:domain / :main:presentation | Navigation3 앱 셸, AppRouteRegistry, 딥링크 매칭 | 동작 |
| :main:entity / :main:data | (내용 없음) | 빈 스캐폴드 |
| :intro:entity / :intro:domain / :intro:data / :intro:presentation | 인트로: RemoteConfig 버전 체크, 권한 요청, refreshToken 자동 로그인 | 실동작 (실 API) |
| :auth:entity / :auth:domain / :auth:data / :auth:presentation | 이메일 로그인, 회원가입 3단계 퍼널, 이메일 인증코드 | 실동작 (실 API) |
| :home:entity / :home:domain / :home:presentation | 홈(마이룸): 고양이 스프라이트, 코인·스트릭·오늘 kcal 게이지 | 목업 (HomeMockData) |
| :home:data | (내용 없음) | 빈 스캐폴드 |
| :meal:entity / :meal:domain / :meal:presentation | 식사 기록: CameraX 촬영 → 확인 | 부분 동작 (제출 미연동) |
| :meal:data | (내용 없음) | 빈 스캐폴드 |
| :history:entity / :history:domain / :history:data / :history:presentation | 월간 캘린더 42칸, 일별 영양 카드, 식사 상세·이름 수정·삭제 | 실동작 (실 API) |
| :tti | 플랫폼 비의존 TTI 계측 | 동작 |
| :baselineprofile | Baseline Profile 수집과 콜드 스타트 Macrobenchmark | 동작 |

등록 상태의 단일 기준은 [settings.gradle.kts](settings.gradle.kts)입니다.

새 feature는 반드시 다음 의존 방향의 4개 모듈로 만듭니다.

~~~text
<feature>:presentation ──▶ <feature>:domain ──▶ <feature>:entity
<feature>:data ──────────▶ <feature>:domain ──▶ <feature>:entity
~~~

presentation과 data는 서로 직접 의존하지 않습니다. entity와 domain은 Android에 의존하지 않는 Kotlin/JVM 모듈입니다.

## 등록 라우트 (7개)

[AppRouteRegistry.kt](main/presentation/src/main/java/com/dandi/nyummy/main/presentation/navigation/AppRouteRegistry.kt) 기준입니다.

| 경로 | 화면 | 하단 탭 |
|---|---|---|
| `""` (루트) | 인트로 (버전 체크·자동 로그인) | - |
| `/login` | 로그인 수단 선택 | - |
| `/login/email` | 이메일 로그인 | - |
| `/signup` | 회원가입 3단계 퍼널 | - |
| `/home` | 홈 (마이룸) | O |
| `/meal/record` | 식사 기록 (카메라) | - |
| `/history` | 히스토리 (월간 캘린더) | O |

하단 탭 바는 5칸(홈·히스토리·퀘스트·컬렉션·상점)이지만 실제 라우팅되는 탭은 홈·히스토리 2개뿐이며, 퀘스트·컬렉션·상점은 post-중간평가 백로그입니다.

## 구현 현황 (2026-08-31)

### 실동작 (실 API 연동)

- **인트로**: RemoteConfig 버전 체크, 권한 요청, refreshToken 자동 로그인.
- **인증(auth)**: 이메일 로그인·회원가입 3단계 퍼널·이메일 인증코드. `AuthApiService` 5개 엔드포인트(login / signup / email-verification / email-verification/confirm / refresh). 단, signup·인증코드 confirm은 서버 쪽이 아직 스텁입니다(앱 연동 자체는 완료).
- **히스토리(history)**: 월간 캘린더, 일별 영양 카드, 식사 상세·이름 수정·삭제. `HistoryApiService` 5개 엔드포인트(monthly / daily / 상세 GET / 이름 PUT / DELETE). 낙관적 갱신과 경쟁 조건 처리 포함.
- **디자인 시스템**: [DesignTokens.kt](common/presentation/src/main/java/com/dandi/nyummy/common/presentation/ui/token/DesignTokens.kt) 607줄 + 공통 컴포넌트 14파일. Figma DS v1.0.0-rc.3과 대조 검증 완료.

### 목업 / 부분 동작

- **홈은 100% 목업**입니다. 코인·스트릭·말풍선 등 모든 데이터가 `HomeMockData`에서 나오며 home:data는 빈 모듈입니다. 고양이는 자체 스프라이트 렌더러(`NyummySpriteView`, doze/sleep_loop/wake 시퀀스)로 움직이지만 식사 데이터와 연결되어 있지 않습니다.
- **식사 기록은 CameraX 촬영 → 확인까지만 동작**합니다. 제출(ClickSubmit)은 미연동이며 meal:data는 빈 모듈입니다. 서버 측 업로드·분석 API(presigned-url → S3 PUT → `POST /meals` → Gemini 분석)는 구현 완료 상태이므로, `MealRecordViewModel.kt`의 "백엔드 미구현" TODO 주석은 낡은 서술입니다. 앱 쪽 연동이 중간평가 백로그입니다.

### 미구현

- 소셜 로그인: 카카오 버튼이 테스트 계정 하드코딩(`LoginViewModel.kt`)으로 이메일 로그인을 호출합니다. 제거 예정.
- 비밀번호 찾기.
- 알려진 버그: 캘린더 `foodIconIds`가 서버 `List<Long>` ↔ 앱 문자열 키 불일치로 항상 salad 폴백. 캘린더 마커를 "기록 여부만"으로 단일화하는 결정에 따라 계약 자체가 폐기 예정입니다.

## 기술 기반

| 영역 | 현재 구성 |
|---|---|
| 언어·빌드 | Kotlin 2.3.21, AGP 9.2.1, JDK 17 |
| Android | compileSdk/targetSdk 37, minSdk 24 |
| UI | Jetpack Compose, Material3, Navigation3 |
| DI | Hilt + KSP |
| 비동기 | Coroutines, Flow |
| 네트워크 | Retrofit 3, OkHttp 5, kotlinx.serialization |
| 카메라 | CameraX 1.5.1 |
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

일부 문서의 코드 예시는 템플릿 시절 레거시 예시이며 문서 내 주석으로 표기되어 있습니다. 실제 패턴은 auth·history·intro 모듈을 우선 참고합니다. 디자인 스펙에서 기능을 구현할 때는 [디자인 스펙 → 코드 가이드](docs/DESIGN_TO_CODE_GUIDE.md)를 먼저 확인합니다.

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
API_BASE_URL=
~~~

API_BASE_URL에는 개발용 EC2 서버 주소를 넣습니다(현재 dev 서버는 cleartext HTTP). 저장소에는 실제 키·주소를 기록하지 않습니다. 값이 없으면 빌드 가능한 placeholder(`https://example.com/`)가 사용되므로, 네트워크 기능 검증 전에는 환경별 값을 주입해야 합니다.

## 빌드와 테스트

시스템 JDK가 없으면 Android Studio의 JBR을 지정합니다.

~~~bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
./gradlew test
~~~

단위 테스트는 auth(검증기·Repository·ApiService), history(ViewModel·캘린더 그리드·UseCase), intro(UseCase), main(라우트 매칭)에 있고, 디자인 시스템 계약 테스트(DesignSystemContractTest)는 androidTest에 있습니다.

Kotlin/Gradle 코드를 수정한 작업은 최소 :app:assembleDebug 성공 후 종료합니다. PR 전에는 다음 검증도 권장합니다.

~~~bash
./gradlew :app:lintDebug
~~~

성능 수집은 API 28 이상 디바이스 또는 에뮬레이터가 필요합니다.

~~~bash
./gradlew :app:generateBaselineProfile
./gradlew :baselineprofile:connectedBenchmarkAndroidTest
~~~

현재 Baseline Profile 시나리오는 앱 콜드 스타트만 수집합니다. 핵심 사용자 흐름(로그인·홈·히스토리)이 안정화되면 시나리오와 검증 조건도 함께 확장해야 합니다.

## 남은 빈 지점

- main:entity, main:data, home:data, meal:data는 빈 스캐폴드입니다. 특히 meal:data 채우기(식사 기록 E2E 연동)가 중간평가 구현 백로그 1순위입니다.
- TTIReporter와 release TTI/Jank sink는 외부 관측 도구가 연결되기 전까지 no-op입니다.
- 새 기능은 [make-new-feature-module 스킬](.agents/skills/make-new-feature-module/SKILL.md)로 4모듈 구조를 만든 뒤 라우트와 앱 의존성을 추가합니다.
