# 냐미(Nyummy) Android

단디 팀의 AI 식사 기록 앱 냐미(Nyummy)의 멀티모듈 클린아키텍처 Android 프로젝트. feature 모듈은 `intro`/`auth`/`onboarding`/`home`/`meal`/`history`/`mailbox`/`room`/`settings`/`legal`/`collection`/`shop`/`quest`/`achievement`/`widget`(화면 없는 기기 홈 화면 위젯) 15세트, 여러 feature가 함께 쓰는 고양이 도메인 `cat`(entity·domain·data + 홈과 마이룸이 같이 쓰는 리그 렌더러 `cat:presentation`), 공통 `common`, 앱 셸 `main`이 있다(`app`/`tti`/`baselineprofile` 포함 총 75모듈). 새 feature는 4모듈 구조와 `docs/architecture` 규칙을 따르고, 가장 완성도 높은 `auth`/`history`(실 API 연동)와 `common` 패턴을 우선 참고한다.

## 현재 구현 현황 (2026-10-26)

- 등록 라우트 28개(`AppRouteRegistry.kt` 기준): `""`(인트로 루트) `/onboarding` `/login` `/login/email` `/login/password-reset` `/signup` `/signup/social` `/home` `/home/prepare`(로그인 직후 냐미 방 준비) `/meal/record` `/meal/foods/{mealId}`(찾은 음식 고르기) `/history` `/history/meals/{mealId}`(식사 상세, `?v=1|2`로 분석 버전을 넘기면 조회 없이 그 상세를 연다) `/shop` `/shop/promotions/{promotionId}` `/shop/cart` `/shop/coins` `/shop/coins/charge`(코인 충전 이벤트) `/mailbox` `/room` `/settings` `/settings/meal-time` `/settings/profile`(마이페이지) `/settings/inquiry` `/mailbox/inquiry/write` `/mailbox/inquiry/{inquiryId}` `/legal/terms` `/legal/privacy`. 링크 별칭은 `/meals/{mealId}`·`/inquiries/{inquiryId}`·`/shop/{tab}`(→ `/shop` 탭·정렬 인자). 컬렉션·퀘스트·업적은 Page만 있고 등록하지 않았다. 문의는 설정으로 옮겼지만 작성·상세 경로는 `/mailbox/inquiry/...` 그대로다.
- 디자인은 피그마 v2(외주 정리안 기반, `v2 · 01 Foundations`~) 기준이다. 글꼴은 Pretendard(UI)와 AndongKaturi(냐미 말투·강조)이고, AndongKaturi에 모양이 없는 글자가 든 문장은 통째로 Pretendard로 그린다(`foundation/NyummyAccentFallback.kt`). 글꼴 출처는 설정 > 약관의 "글꼴 출처".
- 바텀 네비 4탭(홈·마이룸·기록·상점, v2 단색 채움 아이콘) + 기록 탭 앞 바 안의 초록 카메라 버튼(식사 기록 진입) — `NyummyNavigation.kt`의 `NyummyMainTabs`/`RootComposable.kt`.
- 원격 구성(Firebase Remote Config, `common/domain/.../remoteconfig/RemoteConfigKey.kt`): debug 빌드는 키 뒤에 `_debug`를 붙여 읽는다. 기능 플래그 `android_mail_item_tab_enabled`·`android_my_room_enabled`·`android_shop_enabled`는 2.2.1(15)에서 회수했다(앱은 더 읽지 않고, 2.2.0(14) 이하가 읽으므로 콘솔 운영 키는 true로 둔다). 강제 업데이트 키는 `docs/remote-config-force-update.md`. 코인 충전 이벤트(가짜 결제 A/B 테스트, Firebase A/B Testing)는 JSON 키 `android_coin_charge_event`로 홈 공지 맨 앞 장과 `/shop/coins/charge`를 켜고, 단계마다 `coin_charge_*` 분석 이벤트와 사용자 속성 `coin_charge_group`을 남긴다(`docs/remote-config-coin-charge-event.md`). 홈 공지 버튼은 상점을 열고, 실험 대상에게만 상점 탭 줄 끝에 "코인 · Event" 탭이 생겨 선물 코인을 받기 전까지 충전 화면으로 보낸다(받은 뒤엔 "준비 중" 안내). 묶음 가격은 앱에 고정(`CoinChargePackages`)하고, "결제하기"는 `POST /api/v1/coins/gifts`(가격 그룹 헤더 + 묶음 번호)로 선물 코인을 받는다. 상점 추천 탭 배치 A/B는 `android_shop_layout_variant`(A·B)를 추천 탭 조회 헤더 `X-Shop-Layout-Variant`로 보내고, 추천 탭 노출·칸별 누름·담기·구매를 `shop_*` 이벤트와 사용자 속성 `shop_layout_group`으로 남긴다(`docs/remote-config-shop-layout-variant.md`). 냐미 리그(파츠 + 본 + 착용 슬롯) 전환은 `android_cat_rig_enabled`(기본 false, debug는 `CatRigDebugActivity`로 기기에서 덮어씀)로 켜고, 구조와 고양이, 아이템, 클립 더하는 법은 `docs/architecture/cat-rig.md`.
- App Links: `https://link.nyummy.co.kr/<라우트 path>` (autoVerify, S3+CloudFront 서빙, 검증 완료). `assetlinks.json`·폴백 페이지·지문 추가/검증 절차는 `docs/app-links/`. 푸시는 FCM(`app/.../push/`)으로 받고, 디바이스 토큰을 서버에 등록하며, 알림을 탭하면 서버가 준 App Link로 들어간다.
- Airbridge(광고 성과 측정, 앱 이름 `nyummy`): SDK를 직접 부르는 코드는 `common/data/.../analytics/Airbridge*.kt`뿐이고, release 빌드(또는 `-Pnyummy.analyticsInDebug=true`)이면서 `AIRBRIDGE_SDK_TOKEN`이 있을 때만 켠다. 요금제가 표준 이벤트만 받아서, `AnalyticsHelper` 뒤의 `CompositeAnalyticsHelper`가 GA4에는 전부 보내고 Airbridge에는 핵심 전환 7종만 보낸다(`AirbridgeEventMapper.kt`). 트래킹 링크(`nyummy://<앱 링크 경로>` 스킴, `nyummy.abr.ge`, `nyummy.airbridge.io`)로 열리면 `app/.../link/AirbridgeDeeplinkCallbacks`가 목적지를 앱 링크로 바꿔 `navigateToAppLinkAfterEntry`로 넘기고, 내비게이션 호스트가 진입 흐름(인트로, 로그인, 가입, 온보딩, 방 준비)이 끝난 뒤에 연다(`AppLinkAfterEntry`, 진입 흐름 화면은 `AppRoute.isEntryFlow`). 트래킹 링크로 켜지면 경로와 상관없이 인트로부터 시작한다.
- API 계약은 dev 서버 Swagger(`https://ddanddi.nyummy.co.kr/swagger-ui/index.html`)가 기준이다. ApiService 17개:
  - `AuthApiService`: 로그인, 소셜 로그인, 회원가입, 이메일 인증 발송·확인, 비밀번호 재설정, 토큰 재발급, 로그아웃
  - `OnboardingApiService`: 고양이 생성 `POST /api/v1/cats`(이름과 평소 식사 시각을 한 번에 보냄, 성공은 본문 없는 201)
  - `CatApiService`: 내 고양이 조회(체형·이름, 이름은 기기에 기억해 문구에 넣음)·이름 변경 / `HomeApiService`: 홈 요약
  - `MealRecordApiService`: 식사 생성 v2(`POST /api/v2/meals`, 응답에 인식 상태와 찾은 음식), 찾은 음식 조회·확정(`PUT /api/v2/meals/{mealId}/foods`), 기록 취소, 튜토리얼 식사(v1) / `HistoryApiService`: 월간(v1)·일별(v2, 식사마다 `analysisVersion`) 조회, v1 상세·이름 수정·삭제, 영양 분석 재시도(v2)·평가(v1), v2 상세·이름 수정·삭제, v2 음식 목록 저장·한 줄 수정·한 줄 삭제, v2 분석 상태 조회
  - `MailboxApiService`: 우편 목록, 하나 받기, 모두 받기, 받은 우편 삭제·복구
  - `RoomApiService`: 마이룸 조회, 보유 아이템 목록, 꾸민 방 저장
  - `ShopApiService`: 추천 탭 SDUI(칸마다 따로 해석, 모르는 칸은 버림), 인테리어·방 배경 상품 목록, 프로모션 상품 모음, 상단 바(잔액·장바구니 종류 수) / `CartApiService`: 장바구니 조회·담기·수량·선택·빼기 / `PurchaseApiService`: 구매(`Idempotency-Key` 헤더) / `CoinApiService`: 코인 내역, 선물 코인 받기(코인 충전 이벤트, `X-Coin-Package-Variant` 헤더)
  - `SettingsApiService`: 내 프로필(집사 이름·성별·생년월일·키·몸무게·끼니 시각) 조회·변경, 끼니 시각 변경, 알림 수신 설정 조회·변경, 회원 탈퇴 / `InquiryApiService`: 문의 목록·상세·작성·삭제·복구
  - `common:data`: `ImageUploadApiService`(presigned URL 발급), `ImageStorageApiService`(S3 직접 PUT, `@NoAuthApi`), `DeviceTokenApiService`(푸시 토큰 등록)
- Swagger에는 있지만 앱이 아직 쓰지 않는 API: `GET /api/v1/meals/{mealId}/analysis`, `PATCH /api/v1/meals/{mealId}/nutrition`, `PATCH /api/v1/users/me/password`, `PUT /api/v1/inquiries/{inquiryId}`, `DELETE /api/v1/rooms/owned-items/{ownedRoomItemId}`, `POST /api/v1/rooms/owned-items/{ownedRoomItemId}/restore`. 컬렉션·퀘스트·업적 API는 아직 Swagger에 없다.
- 로그인 직후 이동은 응답 `redirectUrl`(앱 링크)을 스택을 비우고 연다(`openAfterLogin`, `navigateToAppLink(asRoot = true)`). 홈이면 앱 링크 대신 방 준비 화면 `/home/prepare`를 거쳐 홈으로 간다. 앱 안 이동·푸시·외부 앱 링크는 모두 `navigateToAppLink` → `handleAppLink` 한 길이다. 회원가입 직후는 항상 온보딩. 온보딩은 미연시형 냥줍 대본(`OnboardingScript`) → 이름 짓기(앱에서 검사만) → 평소 식사 시각 → 고양이 생성(`FinishOnboardingUseCase`) → 방 준비 → 홈. 방 준비 화면과, 자동 로그인으로 홈에 가는 인트로(`GetIntroUseCase`)는 냐미 애니메이션과 시트를 미리 받고(`PrefetchCatAnimationsUseCase`, 최대 3초 대기) 홈으로 간다. 홈 요약도 같이 받기 시작하지만 기다리지 않고, 홈의 첫 조회가 그 요청을 이어받는다(`PrefetchHomeSummaryUseCase`).
- 홈: 꾸민 방(`HomeRoomMapper`) 위에 냐미가 상태별 애니메이션으로 살고, 위에 지갑·우편·설정, 연속 기록 배너, 눈 버튼(화면 요소를 숨기고 방만 보기)이 뜬다. 화면이 보일 때마다 홈 요약을 다시 읽는다.
- 식사 기록: 촬영(또는 갤러리 첨부 — EXIF 촬영일이 기기 타임존 기준 오늘인 사진만, `ImportGalleryMealPhotoUseCase`) → 확인 → 제출(`SubmitMealUseCase`, v2) → 서버 인식 결과로 갈린다: 음식을 찾았으면 찾은 음식 고르기(체크로 고르고 이름을 적어 추가, "N개 가져가기"로 `SubmitMealFoodsUseCase`, X는 기록 취소 뒤 홈), 음식이 아니면 만화 끝장 화면([홈으로]·[다시 찍기], 연출 없음), 인식이 오래 걸리면 "오래 걸리고 있어요" 시트 뒤 홈 → 고른 뒤 연출(`MealCeremonyStage`: 띠욧 컷신 → 맛 리액션 → 먹었어요 → 보상 상자가 우편함으로) → 홈. 건너뛰기는 띠욧 컷신만 건너뛴다. 앱이 종료됐다 돌아와도 찍은 사진과 연출 단계를 되살리고(`MealRecordSnapshot`), 업로드 중 종료됐으면 다시 보내지 않고 확인하라고 안내한다. 접수에 실패하면 안내 스낵바와 함께 곧장 홈으로 간다. 고르기 화면은 기록 탭의 확인 대기 식사에서도 열리고(`/meal/foods/{mealId}`, `MealFoodPickViewModel`), 이때는 가져간 뒤 연출 없이 기록 탭으로 돌아간다. 고르기·음식 아님·지연 화면에서 앱이 종료되면 되살리지 않고 홈으로 간다.
- 기록: 월간 캘린더 → 일별 목록 → 식사 상세 페이지 `/history/meals/{mealId}`. 일별 카드는 상태로 갈린다: 인식 중·계산 대기·계산 중은 눌리지 않는 분석 중 카드, 확인 대기(`CONFIRMING`)는 누르면 고르기 화면, 실패는 재시도·삭제(목록에서 한다), 완료는 `analysisVersion`에 맞는 상세. V1 상세는 이름 수정·삭제·영양 분석 평가(`MealDetailViewModel`). V2 상세(`detail/v2`, `MealDetailV2ViewModel`)는 "사진 속 음식" 줄(아이콘·이름·인분·열량·탄단지)을 보여 주고, 손잡이를 끌어 순서를 바꾸고(맨 위가 대표 음식), 음식을 추가하고, 음식 고치기 시트(`MealFoodEditSheet`: 아이콘·이름·인분 0.5~5·"직접 설정하기"를 체크해야 고치는 영양·지우기)로 한 줄을 고친다. 서버가 영양을 다시 계산하는 동안에는 분석 상태를 2초마다 묻고(`AwaitMealAnalysisUseCase`, 60초 한도) 음식 목록을 고칠 수 없다. 푸시·링크처럼 버전을 모르는 진입은 v2 단건을 조회해 고른다(`MealDetailEntryViewModel`).
- 우편함: 받은 보상·코인·아이템 목록, 하나/모두 받기, 받은 우편만 밀어서 삭제(되돌리기로 복구), 보상 상자 열기 연출(`RewardBoxOpening`).
- 마이룸: 보관함 시트에서 아이템을 방에 놓고, 끌어 옮기고, 두 손가락으로 크기를 바꾸고, 한 단계씩 되돌린 뒤 저장하면 홈으로 간다(편집 동작·칸·시트 조각은 놓아보기와 같이 쓴다: `model/RoomDraftEdits.kt`, `component/RoomSheetParts.kt`).
- 기기 홈 화면 위젯(`widget`, 4x2 RemoteViews): 서버를 부르지 않고, 앱이 `WidgetHelper`로 넘긴 값(식사 접수 시각, 홈 요약의 연속일·오늘 기록 수, 끼니 시각)과 기기 시계로 상태(밤·기다림·배빵빵·놓침·일 단위·재회·로그아웃)를 계산해 그린다(`WidgetPlanner`). 상태가 바뀌는 시각에 알람으로 다시 그리고, 누르면 홈으로 간다. 런처가 레이아웃의 앱 글꼴을 불러오지 않아 문구는 앱이 까투리로 그린 비트맵이다. 시안은 피그마 `v2 · 60 Widget` > `Widget · 20 사용 시안 · 상태별`, debug 빌드는 `NyummyWidgetDebugReceiver`로 상태를 고정해 볼 수 있다.
- 설정: 맨 위 집사 줄 → 마이페이지 `/settings/profile`(고양이 이름·집사 이름·성별·생년월일·키·몸무게를 시트로 고침, 생년월일·키·몸무게는 회원가입과 같은 휠 피커 `NyummyWheelPicker`/`NyummyBirthDatePicker`), 알림(앱 알림 = 기기 알림 권한, 서비스·마케팅 알림 = 서버 값, 식사 시간), 약관, 로그아웃, 회원 탈퇴, 문의하기(목록·작성·상세·삭제·복구). 약관(`legal`)은 `www.nyummy.co.kr/{ko,jp}/policy/` 정책 페이지(앱 언어가 일본어면 jp, 그 밖은 ko)를 WebView로 연다(옛 GitHub Pages 주소도 앱 안에서 연다).
- 상점: 상단 바(코인 칩 → 코인 내역, 장바구니 배지)와 추천(서버 SDUI, 칸마다 따로 해석)·인테리어·방 배경 탭(`/shop/{tab}?sort=` 링크는 탭·정렬로 연다). 담기는 늘 1개(카드 담기 버튼, 수량 선택 없는 상세 시트). 프로모션 `/shop/promotions/{id}`, 장바구니 `/shop/cart`(요청은 누른 순서대로 하나씩 보내고 마지막 응답만 반영, 응답을 다 받기 전에는 구매를 막음, 구매 확인·코인 부족 시트·완료 화면, 같은 구매 키 1회 재시도), 코인 내역 `/shop/coins`(홈 지갑 버튼도 연다, 화면이 보일 때마다 새로 읽음). 장바구니 맨 위 "내 방에 놓아 보기"는 라우트가 아니라 장바구니 화면의 상태로, 마이룸 배경만 가져온 방(가진 인테리어는 보이지 않음)에 담은 아이템을 체크와 상관없이 수량만큼 놓아 본다(저장 없음, 장바구니 화면이 떠 있는 동안만 기억, "n개 구매하기"·뒤로 가기는 장바구니로). 화면은 `room`의 `RoomTryOnPage`이고 `main`이 장바구니의 `ShopTryOnContent` 슬롯에 꽂는다.
- "준비 중" 플레이스홀더 화면: 컬렉션·퀘스트·업적. 이 3개 feature의 entity·data는 비어 있고 domain에는 Page만 있다. 그 밖의 빈 모듈은 `main:entity` `main:data` `legal:data` `intro:entity` `onboarding:entity`.
- 소셜 로그인: 카카오·구글 동작(카카오 SDK / Credential Manager로 OIDC ID 토큰 → `SocialLoginUseCase` 서버 검증 → 기존 회원 redirectUrl 분기 / 신규 회원 `/signup/social` 프로필 입력). 제공자 추가는 `auth/presentation/.../social/SocialLoginClient` 구현 후 `socialLoginClientOf`에 등록. 네이버 로그인은 보류(로그인 화면에 버튼 없음, `SocialLoginType.NAVER`만 남김). 로그인 화면은 원형 버튼 3개(카카오 축약형·구글·이메일). 비밀번호 찾기(`/login/password-reset`)는 이메일 → 인증 코드 → 임시 비밀번호 메일 발송 순서로 한 라우트 안에서 진행한다. 테스트 계정 로그인 버튼은 debug 빌드에서 `local.properties`의 `TEST_LOGIN_EMAIL`/`TEST_LOGIN_PASSWORD`가 있을 때만 노출 — 자격 증명을 소스에 하드코딩 금지(공개 저장소).

## 빌드 / 테스트

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"  # 시스템 JDK 없을 때
./gradlew :app:assembleDebug    # 빌드 검증 (코드 수정 후 필수 — gradle-build-check 스킬)
./gradlew test                  # 단위 테스트 (run-android-tests 스킬)
```

코드를 수정한 턴은 반드시 빌드 검증 후 종료한다. Play 내부 테스트 트랙 배포는 fastlane(`bundle exec fastlane android internal version_name:2.2.3`, versionCode와 versionName은 배포 때 정하므로 `app/build.gradle.kts` 버전은 손으로 올리지 않음)과 `.github/workflows/release.yml`(수동 실행 또는 `v*` 태그, `release` environment 시크릿)로 하고, 출시 노트는 `fastlane/release-notes/<버전 이름>/<로케일>.txt`에 두면 lane이 함께 올린다(없으면 멈춤). 시크릿 목록과 순서는 `docs/DEVELOPMENT.md`의 "배포" 절을 본다. API 키/BASE_URL은 `local.properties`(`API_KEY`, debug는 `API_BASE_URL_DEV`·release는 `API_BASE_URL_PROD`) → BuildConfig 주입(debug 전용 테스트 계정 `TEST_LOGIN_EMAIL`/`TEST_LOGIN_PASSWORD`, 카카오 네이티브 앱 키 `KAKAO_NATIVE_APP_KEY`, 구글 OAuth 웹 클라이언트 ID `GOOGLE_WEB_CLIENT_ID`도 동일 — 키가 없으면 해당 제공자 버튼은 "준비 중" 안내). 베이스 URL은 debug→개발 서버, release→운영 서버이며 값이 없으면 placeholder로 빌드만 된다(release는 운영 키만 읽는다). release 서명도 같은 방식으로 `RELEASE_STORE_FILE`/`RELEASE_STORE_PASSWORD`/`RELEASE_KEY_ALIAS`/`RELEASE_KEY_PASSWORD`(local.properties, 없으면 환경변수)에서 읽고, 하나라도 없으면 release는 서명하지 않는다(debug 키로 대신 서명하지 않음). Meta 광고 설치 측정 키 `META_APP_ID`/`META_CLIENT_TOKEN`도 같은 곳에서 읽고, release는 없으면 빌드가 실패한다(자동 기록은 release만). Airbridge SDK 토큰 `AIRBRIDGE_SDK_TOKEN`도 같은 곳에서 읽고, 없으면 빌드는 되지만 SDK를 켜지 않는다. 단위 테스트는 빈 스캐폴드와 플레이스홀더 feature를 뺀 대부분의 모듈에, 디자인 시스템 토큰 계약 테스트는 `common/presentation`의 단위 테스트에, 컴포넌트·화면 UI 테스트는 androidTest(`common`/`auth`/`history`/`home`/`intro`/`onboarding`/`room`의 presentation, `meal:data`)에 있다.

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
