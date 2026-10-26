# 냐미(Nyummy) Android

단디 팀의 AI 식사 기록 앱 냐미(Nyummy)의 멀티모듈 클린아키텍처 Android 프로젝트. feature 모듈은 `intro`/`auth`/`onboarding`/`home`/`meal`/`history`/`mailbox`/`room`/`settings`/`legal`/`collection`/`shop`/`quest`/`achievement` 14세트, 여러 feature가 함께 쓰는 고양이 도메인 `cat`(presentation 없이 entity·domain·data 3모듈), 공통 `common`, 앱 셸 `main`이 있다(`app`/`tti`/`baselineprofile` 포함 총 70모듈). 새 feature는 4모듈 구조와 `docs/architecture` 규칙을 따르고, 가장 완성도 높은 `auth`/`history`(실 API 연동)와 `common` 패턴을 우선 참고한다.

## 현재 구현 현황 (2026-10-26)

- 등록 라우트 20개(`AppRouteRegistry.kt` 기준): `""`(인트로 루트) `/onboarding` `/login` `/login/email` `/login/password-reset` `/signup` `/signup/social` `/home` `/meal/record` `/history` `/shop` `/mailbox` `/room` `/settings` `/settings/meal-time` `/settings/inquiry` `/mailbox/inquiry/write` `/mailbox/inquiry/{inquiryId}` `/legal/terms` `/legal/privacy`. 컬렉션·퀘스트·업적은 Page만 있고 등록하지 않았다. 문의는 설정으로 옮겼지만 작성·상세 경로는 `/mailbox/inquiry/...` 그대로다.
- 바텀 네비 4탭(홈·마이룸·기록·상점) + 기록 탭 앞 가운데 카메라 버튼(식사 기록 진입) — `NyummyNavigation.kt`의 `NyummyMainTabs`/`RootComposable.kt`. 마이룸·상점은 기능 플래그가 꺼져 있으면 탭을 눌렀을 때 "준비 중"이라고 알린다.
- 기능 플래그(Firebase Remote Config, `common/domain/.../remoteconfig/RemoteConfigKey.kt`): `android_mail_item_tab_enabled`(우편함 아이템 탭 + 식사 기록 뒤 상자를 우편함으로 보내는 연출), `android_my_room_enabled`, `android_shop_enabled`. 기본값은 모두 false이고 debug 빌드는 키 뒤에 `_debug`를 붙여 읽는다. 강제 업데이트 키는 `docs/remote-config-force-update.md`.
- App Links: `https://link.nyummy.co.kr/<라우트 path>` (autoVerify, S3+CloudFront 서빙, 검증 완료). `assetlinks.json`·폴백 페이지·지문 추가/검증 절차는 `docs/app-links/`. 푸시는 FCM(`app/.../push/`)으로 받고, 디바이스 토큰을 서버에 등록하며, 알림을 탭하면 서버가 준 App Link로 들어간다.
- API 계약은 dev 서버 Swagger(`https://ddanddi.nyummy.co.kr/swagger-ui/index.html`)가 기준이다. ApiService 13개:
  - `AuthApiService`: 로그인, 소셜 로그인, 회원가입, 이메일 인증 발송·확인, 비밀번호 재설정, 토큰 재발급, 로그아웃
  - `OnboardingApiService`: 고양이 생성 `POST /api/v1/cats`(이름과 평소 식사 시각을 한 번에 보냄, 성공은 본문 없는 201)
  - `CatApiService`: 냐미 상태별 애니메이션 / `HomeApiService`: 홈 요약
  - `MealRecordApiService`: 식사 생성 / `HistoryApiService`: 월간·일별·상세 조회, 이름 수정, 삭제, 영양 분석 재시도·평가
  - `MailboxApiService`: 우편 목록, 하나 받기, 모두 받기, 받은 우편 삭제·복구
  - `RoomApiService`: 마이룸 조회, 보유 아이템 목록, 꾸민 방 저장
  - `SettingsApiService`: 내 프로필, 끼니 시각, 알림 수신 설정 조회·변경, 회원 탈퇴 / `InquiryApiService`: 문의 목록·상세·작성·삭제·복구
  - `common:data`: `ImageUploadApiService`(presigned URL 발급), `ImageStorageApiService`(S3 직접 PUT, `@NoAuthApi`), `DeviceTokenApiService`(푸시 토큰 등록)
- Swagger에는 있지만 앱이 아직 쓰지 않는 API: `GET /api/v1/cats`, `GET /api/v1/meals/{mealId}/analysis`, `PATCH /api/v1/users/me/password`, `PUT /api/v1/inquiries/{inquiryId}`, `DELETE /api/v1/rooms/owned-items/{ownedRoomItemId}`, `POST /api/v1/rooms/owned-items/{ownedRoomItemId}/restore`. 상점·컬렉션·퀘스트·업적 API는 아직 Swagger에 없다.
- 로그인 직후 이동은 응답 `redirectUrl`의 path로 결정(`PostLoginDestination`: `/onboarding`이면 온보딩, 그 외 홈). 회원가입 직후는 항상 온보딩. 온보딩은 미연시형 냥줍 대본(`OnboardingScript`) → 이름 짓기(앱에서 검사만) → 평소 식사 시각 → 고양이 생성(`FinishOnboardingUseCase`) → 홈.
- 홈: 꾸민 방(`HomeRoomMapper`) 위에 냐미가 상태별 애니메이션으로 살고, 위에 지갑·우편·설정, 연속 기록 배너, 눈 버튼(화면 요소를 숨기고 방만 보기)이 뜬다. 화면이 보일 때마다 홈 요약을 다시 읽는다.
- 식사 기록: 촬영(또는 갤러리 첨부 — EXIF 촬영일이 기기 타임존 기준 오늘인 사진만, `ImportGalleryMealPhotoUseCase`) → 확인 → 제출(`SubmitMealUseCase`) → 연출(`MealCeremonyStage`: 띠욧 컷신 → 맛 리액션 → 먹었어요 → 보상 상자가 우편함으로) → 홈. 건너뛰기는 띠욧 컷신만 건너뛴다. 앱이 종료됐다 돌아와도 찍은 사진과 연출 단계를 되살리고(`MealRecordSnapshot`), 업로드 중 종료됐으면 다시 보내지 않고 확인하라고 안내한다. 접수에 실패하면 안내 스낵바와 함께 곧장 홈으로 간다.
- 기록: 월간 캘린더 → 일별 목록 → 식사 상세(이름 수정, 삭제, 영양 분석 재시도·평가).
- 우편함: 받은 보상·코인·아이템 목록, 하나/모두 받기, 받은 우편만 밀어서 삭제(되돌리기로 복구), 보상 상자 열기 연출(`RewardBoxOpening`).
- 마이룸: 보관함 시트에서 아이템을 방에 놓고, 끌어 옮기고, 두 손가락으로 크기를 바꾸고, 한 단계씩 되돌린 뒤 저장하면 홈으로 간다.
- 설정: 알림(앱 알림 = 기기 알림 권한, 서비스·마케팅 알림 = 서버 값, 식사 시간), 약관, 로그아웃, 회원 탈퇴, 문의하기(목록·작성·상세·삭제·복구). 약관(`legal`)은 GitHub Pages 정책 페이지를 WebView로 연다.
- "준비 중" 플레이스홀더 화면: 컬렉션·상점·퀘스트·업적. 이 4개 feature의 entity·data는 비어 있고 domain에는 Page만 있다. 그 밖의 빈 모듈은 `main:entity` `main:data` `legal:data` `intro:entity` `onboarding:entity`.
- 소셜 로그인: 카카오·구글 동작(카카오 SDK / Credential Manager로 OIDC ID 토큰 → `SocialLoginUseCase` 서버 검증 → 기존 회원 redirectUrl 분기 / 신규 회원 `/signup/social` 프로필 입력). 제공자 추가는 `auth/presentation/.../social/SocialLoginClient` 구현 후 `socialLoginClientOf`에 등록. 네이버 로그인은 보류(로그인 화면에 버튼 없음, `SocialLoginType.NAVER`만 남김). 로그인 화면은 원형 버튼 3개(카카오 축약형·구글·이메일). 비밀번호 찾기(`/login/password-reset`)는 이메일 → 인증 코드 → 임시 비밀번호 메일 발송 순서로 한 라우트 안에서 진행한다. 테스트 계정 로그인 버튼은 debug 빌드에서 `local.properties`의 `TEST_LOGIN_EMAIL`/`TEST_LOGIN_PASSWORD`가 있을 때만 노출 — 자격 증명을 소스에 하드코딩 금지(공개 저장소).

## 빌드 / 테스트

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"  # 시스템 JDK 없을 때
./gradlew :app:assembleDebug    # 빌드 검증 (코드 수정 후 필수 — gradle-build-check 스킬)
./gradlew test                  # 단위 테스트 (run-android-tests 스킬)
```

코드를 수정한 턴은 반드시 빌드 검증 후 종료한다. API 키/BASE_URL은 `local.properties`(`API_KEY`, debug는 `API_BASE_URL_DEV`·release는 `API_BASE_URL_PROD`) → BuildConfig 주입(debug 전용 테스트 계정 `TEST_LOGIN_EMAIL`/`TEST_LOGIN_PASSWORD`, 카카오 네이티브 앱 키 `KAKAO_NATIVE_APP_KEY`, 구글 OAuth 웹 클라이언트 ID `GOOGLE_WEB_CLIENT_ID`도 동일 — 키가 없으면 해당 제공자 버튼은 "준비 중" 안내). 베이스 URL은 debug→개발 서버, release→운영 서버이며 값이 없으면 placeholder로 빌드만 된다(release는 운영 키만 읽는다). release 서명도 같은 방식으로 `RELEASE_STORE_FILE`/`RELEASE_STORE_PASSWORD`/`RELEASE_KEY_ALIAS`/`RELEASE_KEY_PASSWORD`(local.properties, 없으면 환경변수)에서 읽고, 하나라도 없으면 release는 서명하지 않는다(debug 키로 대신 서명하지 않음). Meta 광고 설치 측정 키 `META_APP_ID`/`META_CLIENT_TOKEN`도 같은 곳에서 읽고, release는 없으면 빌드가 실패한다(자동 기록은 release만). 단위 테스트는 빈 스캐폴드와 플레이스홀더 feature를 뺀 대부분의 모듈에, 디자인 시스템 토큰 계약 테스트는 `common/presentation`의 단위 테스트에, 컴포넌트·화면 UI 테스트는 androidTest(`common`/`auth`/`history`/`home`/`intro`/`onboarding`/`room`의 presentation, `meal:data`)에 있다.

## 불변 규칙 (위반 금지)

1. **의존 방향**: `presentation → domain → entity`, `data → domain → entity`. presentation↔data 직접 의존 금지. entity/domain은 순수 Kotlin/JVM(Android 의존 금지).
2. **feature = 4모듈**: `<feature>/{entity,domain,data,presentation}`. 생성은 `make-new-feature-module` 스킬로만.
3. **MVI**: View→`onIntent(Intent)` 단일 진입, 상태 변이는 `dispatch(ReducerEvent)`→`reduce()` 한 곳에서만. UIState 컬렉션은 ImmutableList/ImmutableSet.
4. **디자인 토큰**: 색, 글자, 간격, 모서리는 `NyummyTheme.colors/typography/spacing/radius/...`만 쓴다(텍스트는 `NyummyText` 경유). raw hex / raw sp 금지. 토큰 값 수정은 `common/presentation/.../designsystem/{token,theme}/`에서만 하고 계약 테스트 기대값도 함께 바꾼다. 자세한 규칙은 `docs/architecture/design-system.md`.
5. **DTO/VO**: DTO는 `@Serializable`+전 필드 nullable, VO는 비-nullable+기본값. 변환은 data 레이어 `toVO()`에서만.
6. **에러**: data는 `HttpResponseException` throw만, 처리(다이얼로그/네비게이션)는 domain UseCase에서 `isCommonErrorHandling()`/`handlingErrorOnUseCase<ErrorType>()`로.
7. **네비게이션**: 화면 이동은 `navigationHelper.navigateTo(Page)`만. 새 화면은 `AppRouteRegistry.kt`에 등록.
8. **시간/타임존**: 서버와는 UTC 순간(ISO + 오프셋)으로 주고받고, 화면 표시·"오늘" 판정만 **기기 타임존**으로 한다. 기기 타임존은 `common/entity/.../time/DeviceTime.kt`로만 읽는다(`DeviceTime.zone()`/`today()`/`parseIsoOrNull()`) — 호출부에서 `ZoneId.systemDefault()`/`TimeZone.getDefault()`/`Calendar.getInstance()`를 직접 쓰지 않는다. 서버 시간 문자열 → 기기 타임존 변환은 data 레이어 `toVO()`에서만, VO에는 변환이 끝난 값만. 날짜 계산은 `java.time`(core library desugaring 적용, 모든 Android 모듈에 `coreLibraryDesugaring` 선언)을 쓴다. 서버는 `X-Timezone-Id` 헤더(`CommonHeaderInterceptor`)의 타임존으로 하루를 끊는다.
9. **이미지 로딩**: Coil 의존성은 `common:presentation`에서만 `api`로 노출, ImageLoader 설정은 `common/presentation/.../image/NyummyImageLoaderFactory.kt` 한 곳(presigned URL 캐시 키 정규화 포함). feature 모듈에 coil 의존 개별 선언 금지.

## 네이밍

`{Feature}Page(=Composable & domain의 Page object) / {Feature}UIState / {Feature}Intent / {Feature}ReducerEvent / {Feature}ViewModel / {Feature}Repository(Impl) / {Feature}DataSource / {Feature}ApiService / {Feature}DTO / {Feature}VO / {Feature}DataModule / {Feature}ErrorType(domain에 위치)`

## 상세 문서 (패턴별 왜/어떻게/골든 예제)

- [docs/architecture/module-structure.md](docs/architecture/module-structure.md) — 모듈 규칙, build.gradle.kts 3종 템플릿
- [docs/architecture/mvi.md](docs/architecture/mvi.md) — MviViewModel 계약, 페이징 상태 패턴
- [docs/architecture/navigation.md](docs/architecture/navigation.md) — Navigation3, synthetic backstack, 딥링크
- [docs/architecture/error-handling.md](docs/architecture/error-handling.md) — 에러 전파/처리 체인
- [docs/architecture/data-layer.md](docs/architecture/data-layer.md) — DTO/VO, DataSource, Hilt DataModule, 로컬 저장소
- [docs/architecture/design-system.md](docs/architecture/design-system.md) — 토큰 구조, Figma 매핑 명세
- [docs/architecture/performance.md](docs/architecture/performance.md) — TTI, JankStats, Baseline Profile
- [docs/DESIGN_TO_CODE_GUIDE.md](docs/DESIGN_TO_CODE_GUIDE.md) — 디자인 스펙 → 코드 사용자 가이드

문서의 골든 예제는 실제 모듈(주로 `history`, `auth`, `mailbox`)을 가리킨다. 문서와 코드가 다르면 코드가 맞고, 문서를 함께 고친다.

## 스킬 인덱스

| 작업 | 스킬 |
|---|---|
| 디자인 스펙(Figma/이미지/PDF) → feature 전체 | `design-to-feature` |
| Figma 토큰 → 테마 갱신 | `design-token-sync` |
| 새 feature 4모듈 스캐폴딩 | `make-new-feature-module` |
| 네비게이션/라우트/딥링크 규칙 (화면·경로·딥링크 추가/수정 시) | `navigation-conventions` |
| 예시 JSON → 데이터 레이어 | `api-dto-code-gen` |
| Composable 1개 추가 | `compose-component` |
| 빌드 검증 (수정 후 필수) | `gradle-build-check` |
| 테스트 실행 | `run-android-tests` |
| 템플릿 → 신규 프로젝트 (디자인 스펙 Figma/이미지/PDF 동반 시 토큰+화면 생성까지 연계) | `new-project-from-archi` |
| 에뮬레이터 화면 vs 디자인 비교 | `verify-screen` |
