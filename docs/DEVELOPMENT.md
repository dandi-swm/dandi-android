# 개발 가이드

냐미 안드로이드 저장소의 개발 규칙, 로컬 설정, 빌드와 테스트 방법입니다. 프로젝트 소개는 [README](../README.md)를 봅니다.

## 모듈 규칙

등록 상태의 단일 기준은 [settings.gradle.kts](../settings.gradle.kts)입니다. 새 feature는 반드시 다음 의존 방향의 4개 모듈로 만듭니다.

~~~text
<feature>:presentation ──▶ <feature>:domain ──▶ <feature>:entity
<feature>:data ──────────▶ <feature>:domain ──▶ <feature>:entity
~~~

presentation과 data는 서로 직접 의존하지 않습니다. entity와 domain은 Android에 의존하지 않는 Kotlin/JVM 모듈입니다. 새 기능은 [make-new-feature-module 스킬](../.agents/skills/make-new-feature-module/SKILL.md)로 4모듈 구조를 만든 뒤 라우트와 앱 의존성을 추가합니다.

## 아키텍처 문서

| 문서 | 내용 |
|---|---|
| [모듈 구조](architecture/module-structure.md) | 현재 모듈과 새 feature 배선 |
| [MVI](architecture/mvi.md) | Intent, UIState, ReducerEvent, ViewModel 계약 |
| [Navigation3](architecture/navigation.md) | Page/NavRoute, 레지스트리, 딥링크와 백스택 |
| [데이터 레이어](architecture/data-layer.md) | DTO/VO, BaseRemoteDataSource, Retrofit/Hilt |
| [에러 핸들링](architecture/error-handling.md) | HttpResponseException과 BaseUseCase 처리 경계 |
| [디자인 시스템](architecture/design-system.md) | Figma 토큰, 색상·타이포 소비 규칙 |
| [성능 계측](architecture/performance.md) | TTI, JankStats, Baseline Profile |

일부 문서의 코드 예시는 템플릿 시절 레거시 예시이며 문서 내 주석으로 표기되어 있습니다. 실제 패턴은 auth·history·intro 모듈을 우선 참고합니다. 디자인 스펙에서 기능을 구현할 때는 [디자인 스펙 → 코드 가이드](DESIGN_TO_CODE_GUIDE.md)를 먼저 확인합니다.

## 핵심 계약

- 화면 입력은 ViewModel의 onIntent(Intent) 하나로 들어갑니다.
- 상태는 dispatch(ReducerEvent) → reduce()에서만 변경합니다.
- UIState 컬렉션은 ImmutableList 또는 ImmutableSet을 사용합니다.
- 화면 이동은 NavigationHelper.navigateTo(Page)로 요청하고, 모든 렌더 라우트는 AppRouteRegistry에 등록합니다.
- DTO는 @Serializable이며 모든 필드를 nullable로 둡니다. VO는 비-nullable 기본값을 가지며 변환은 data의 toVO()에서만 수행합니다.
- HTTP 실패는 data에서 HttpResponseException으로 올리고, 공통/기능별 처리는 domain UseCase가 담당합니다.
- 화면의 색상과 타이포는 NyummyTheme 토큰과 NyummyText를 사용합니다. raw hex와 raw sp를 화면 코드에 두지 않습니다.

## 로컬 설정

[common/data/build.gradle.kts](../common/data/build.gradle.kts)가 아래 값을 local.properties에서 BuildConfig로 주입합니다.

~~~properties
API_KEY=
API_BASE_URL_DEV=
API_BASE_URL_PROD=
~~~

[auth/presentation/build.gradle.kts](../auth/presentation/build.gradle.kts)는 카카오 로그인 네이티브 앱 키를 BuildConfig와 매니페스트(리다이렉트 스킴 `kakao{키}://oauth`)로 주입합니다. 없으면 환경변수 `KAKAO_NATIVE_APP_KEY`를 쓰고, 둘 다 없으면 SDK 초기화를 건너뛰어 카카오 버튼은 "준비 중" 안내만 띄웁니다.

~~~properties
KAKAO_NATIVE_APP_KEY=
~~~

카카오 디벨로퍼스 콘솔에는 Android 플랫폼(패키지 `com.dandi.nyummy`)과 빌드에 쓰는 서명 키의 키 해시를 등록하고, 카카오 로그인과 OpenID Connect를 활성화해야 합니다(ID 토큰을 서버가 검증). 디버그 키 해시는 PC마다 다르므로 각자 등록합니다.

~~~bash
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | openssl sha1 -binary | openssl base64
~~~

[app/build.gradle.kts](../app/build.gradle.kts)는 Meta 광고 설치 측정(facebook-core 앱 이벤트)의 앱 ID와 클라이언트 토큰을 문자열 리소스로 주입합니다. 앱 ID는 Meta 개발자 콘솔의 앱 설정 > 기본 설정, 클라이언트 토큰은 고급 설정 > 보안에 있습니다. 설치, 실행 자동 기록과 광고 ID 수집은 release 빌드에서만 켜지고, release 빌드는 두 값이 없으면 실패합니다. debug는 값이 없어도 빌드되고 SDK 초기화만 건너뜁니다.

~~~properties
META_APP_ID=
META_CLIENT_TOKEN=
~~~

debug 빌드는 `API_BASE_URL_DEV`(개발 서버), release 빌드는 `API_BASE_URL_PROD`(운영 서버)를 씁니다. local.properties에 없으면 같은 이름의 환경변수를 읽습니다. debug는 예전 키 `API_BASE_URL`도 받지만, release가 개발 서버에 붙지 않도록 운영 값은 다른 키로 대신하지 않습니다. 저장소에는 실제 키·주소를 기록하지 않습니다. 값이 없으면 빌드 가능한 placeholder(`https://example.com/`)가 사용되므로, 네트워크 기능 검증 전에는 환경별 값을 주입해야 합니다.

## 빌드와 테스트

시스템 JDK가 없으면 Android Studio의 JBR을 지정합니다.

~~~bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
./gradlew test
~~~

단위 테스트는 auth(검증기·Repository·ApiService·소셜 로그인/가입 UseCase·로그인 ViewModel), history(ViewModel·캘린더 그리드·UseCase), intro(UseCase), main(라우트 매칭)에 있고, 디자인 시스템 토큰 계약 테스트(NyummyTokenContractTest, NyummyTypographyContractTest)는 common:presentation 단위 테스트에, 컴포넌트 UI 테스트는 androidTest에 있습니다.

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

