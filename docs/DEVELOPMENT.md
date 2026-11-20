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


## 배포

release AAB는 fastlane으로 만들어 Play Console 내부 테스트 트랙에 올립니다. 스토어 문구, 스크린샷, 변경 사항은 올리지 않으므로 Console에서 직접 관리합니다.

- [fastlane/Fastfile](../fastlane/Fastfile)의 `android internal` lane: internal, alpha, beta, production 트랙의 versionCode 중 최댓값을 조회해 그보다 1 큰 값을 `-Pnyummy.versionCode=<n>` Gradle 프로퍼티로 넘겨 `:app:bundleRelease`를 돌리고, `app/build/outputs/bundle/release/app-release.aab`를 internal 트랙에 올립니다. 계산한 versionCode는 lane 로그에 남고 git에는 커밋하지 않습니다. 트랙이 모두 비어 있으면 [app/build.gradle.kts](../app/build.gradle.kts)의 versionCode 기본값을 씁니다. versionName은 lane 옵션 `version_name:` 또는 `NYUMMY_VERSION_NAME` 환경변수(옵션 우선)로 받아 `-Pnyummy.versionName=<이름>`으로 넘기고, Play 업로드의 출시 이름에도 같은 값을 줍니다. 값이 없거나 `숫자.숫자.숫자` 형식이 아니면(앞의 `v`도 안 됨) 빌드 전에 멈춥니다. 네 트랙의 출시 이름에 같은 버전 이름이 온전한 토큰으로 들어 있으면 이미 배포한 이름으로 보고 멈춥니다(출시 이름 문자열에 기대는 검사입니다).
- lane은 빌드 전에 release에 필요한 키가 빌드 스크립트가 읽는 곳에 있는지 먼저 확인하고, 없으면 빠진 키 이름만 알리고 멈춥니다. 키가 가리키는 키스토어와 서비스 계정 JSON 파일이 실제로 있는지도 봅니다. 비어 있는 트랙은 계산에서 빠지고, 트랙 조회 자체가 실패하면 lane도 실패합니다.
- [.github/workflows/release.yml](../.github/workflows/release.yml): 수동 실행(workflow_dispatch, 필수 입력 `version_name`)과 `v*` 태그 push에서만 돕니다. 태그는 앞의 `v`를 뗀 값을 버전 이름으로 쓰고, 두 경우 모두 형식을 먼저 검사해 `NYUMMY_VERSION_NAME`으로 lane에 넘깁니다. 공개 저장소라 pull_request 트리거는 두지 않습니다. 시크릿은 저장소가 아니라 `release` environment에 등록하고, 하나라도 비어 있으면 빠진 이름을 알리고 바로 실패합니다. 수동 실행은 main에서만, 태그는 main에 들어간 커밋에서만 진행합니다. 운영 키가 든 빌드 결과가 공개 저장소의 Actions 캐시에 남지 않도록 Gradle 캐시를 끄고 빌드합니다. Ruby 버전은 저장소 루트의 `.ruby-version`을 따릅니다.

### 시크릿

| 이름 | 내용 | 빌드에 전달되는 방식 |
|---|---|---|
| `RELEASE_KEYSTORE_B64` | 업로드 키스토어 파일(base64) | runner 임시 경로에 풀고 `RELEASE_STORE_FILE` 환경변수로 경로 전달 |
| `RELEASE_STORE_PASSWORD` | 키스토어 비밀번호 | 환경변수 |
| `RELEASE_KEY_ALIAS` | 키 별칭 | 환경변수 |
| `RELEASE_KEY_PASSWORD` | 키 비밀번호 | 환경변수 |
| `API_BASE_URL_PROD` | 운영 서버 주소 | 환경변수 |
| `META_APP_ID` | Meta 앱 ID | 환경변수 |
| `META_CLIENT_TOKEN` | Meta 클라이언트 토큰 | 환경변수 |
| `KAKAO_NATIVE_APP_KEY` | 카카오 네이티브 앱 키 | 환경변수 |
| `GOOGLE_WEB_CLIENT_ID` | 구글 OAuth 웹 클라이언트 ID | 환경변수 |
| `AIRBRIDGE_SDK_TOKEN` | Airbridge SDK 토큰 | 환경변수 |
| `GOOGLE_SERVICES_JSON_B64` | app/google-services.json(base64) | `app/google-services.json`으로 복원 |
| `PLAY_SERVICE_ACCOUNT_JSON_B64` | Play 서비스 계정 JSON 키(base64) | 임시 파일로 풀고 `PLAY_SERVICE_ACCOUNT_JSON_PATH`로 Appfile에 전달 |

`GOOGLE_SERVICES_JSON_B64`는 PR 검사용 저장소 시크릿과 같은 값이지만, environment 시크릿에도 따로 등록합니다. 값을 명령줄 인자로 넘기면 셸 기록에 남으므로 아래처럼 입력 프롬프트나 파일로 넘깁니다.

파일은 base64로 넣습니다. macOS는 `base64 -i <파일>`, Linux(GNU coreutils)는 `base64 -w 0 <파일>`을 씁니다(Linux의 `-i`는 다른 뜻입니다). 아래는 macOS 예시입니다.

~~~bash
base64 -i path/to/upload.jks | gh secret set RELEASE_KEYSTORE_B64 --env release
base64 -i app/google-services.json | gh secret set GOOGLE_SERVICES_JSON_B64 --env release
base64 -i path/to/play-service-account.json | gh secret set PLAY_SERVICE_ACCOUNT_JSON_B64 --env release

# 문자열은 프롬프트에 붙여 넣는다(값을 명령에 쓰지 않는다)
for name in RELEASE_STORE_PASSWORD RELEASE_KEY_ALIAS RELEASE_KEY_PASSWORD API_BASE_URL_PROD \
            META_APP_ID META_CLIENT_TOKEN KAKAO_NATIVE_APP_KEY GOOGLE_WEB_CLIENT_ID AIRBRIDGE_SDK_TOKEN; do
  gh secret set "$name" --env release
done
~~~

`release` environment는 GitHub 저장소 Settings > Environments에서 만들고, 배포 브랜치와 태그를 `main`과 `v*`로 제한하거나 승인자를 두는 것을 권장합니다.

### Play 서비스 계정 만들기

1. Google Cloud Console에서 Play Console과 연결된 프로젝트를 고르고 Google Play Android Developer API를 사용 설정합니다.
2. IAM 및 관리자 > 서비스 계정에서 서비스 계정을 만들고, 키 탭에서 JSON 키를 만들어 내려받습니다. 이 파일이 `PLAY_SERVICE_ACCOUNT_JSON_B64`의 원본이며 저장소에 두지 않습니다.
3. Play Console > 사용자 및 권한 > 새 사용자 초대에 서비스 계정 이메일을 넣고, 앱 권한에서 냐미 앱을 추가합니다.
4. 권한은 "앱 정보 보기(읽기 전용)", "테스트 트랙에 앱 출시", "출시 관리"처럼 업로드에 필요한 만큼만 줍니다. 초대를 적용하는 데 시간이 걸릴 수 있습니다.
5. 로컬에서 확인할 때는 `bundle exec fastlane run validate_play_store_json_key json_key:<키 경로>`로 키가 동작하는지 봅니다.

### 배포 순서

Play API는 한 번도 올라간 적 없는 앱에는 올릴 수 없으므로, 첫 번째 AAB는 Play Console에서 직접 올려야 합니다. 앱이 아직 초안(Draft) 상태이면 Play가 완료 상태 출시를 받지 않으므로 Console에서 첫 출시를 마친 뒤 이 lane을 씁니다.

1. versionCode와 versionName은 모두 배포할 때 정해지므로 app/build.gradle.kts의 버전 숫자는 손으로 올리지 않습니다(파일 값은 로컬 빌드용 기본값입니다).
2. main에서 `v2.2.3`처럼 태그를 붙여 push하면 versionName 2.2.3으로 배포합니다. Actions 탭에서 "Release (Play internal)"를 수동 실행할 때는 버전 이름(예: 2.2.3)을 입력합니다. Play에 같은 이름의 출시가 이미 있으면 lane이 거부합니다.
3. 업로드가 끝나면 Play Console 내부 테스트에서 빌드를 확인하고, 변경 사항을 적은 뒤 프로덕션(또는 다른 트랙)으로 승격합니다.

로컬에서 lane을 돌릴 때는 `.ruby-version`의 Ruby와 `Gemfile.lock`의 Bundler를 준비하고 `bundle install` 뒤 `bundle exec fastlane android internal version_name:2.2.3`처럼 버전 이름을 주고 실행합니다. 키는 빌드 스크립트가 읽는 곳에 있어야 합니다.

- `PLAY_SERVICE_ACCOUNT_JSON_PATH`: 환경변수로만 줍니다(Appfile이 읽음).
- `API_BASE_URL_PROD`: local.properties에 키가 있으면 빈 값이어도 환경변수를 보지 않으므로, 환경변수로 줄 때는 local.properties에서 그 줄을 지웁니다.
- 나머지(서명 4종, `META_APP_ID`, `META_CLIENT_TOKEN`, `KAKAO_NATIVE_APP_KEY`, `GOOGLE_WEB_CLIENT_ID`, `AIRBRIDGE_SDK_TOKEN`): local.properties, 없으면 환경변수.
