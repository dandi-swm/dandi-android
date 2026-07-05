# 성능 계측 (TTI / JankStats / Baseline Profile)

## 왜

- 화면이 늘어날 때마다 "이 페이지 왜 느리지"를 측정 없이 추측하지 않도록, **페이지 단위 계측을 아키텍처에 내장**한다.
- 새 feature를 만들 때 TTI/Jank 계측을 따라 붙이는 것이 컨벤션이다 (필수는 페이지 단위 Jank — AppNavHost가 자동 적용, TTI는 핵심 화면에 선택 적용).

## 1. TTI (Time To Initial Display)

순수 JVM 모듈 `:tti`. 인터페이스: [TTIHelper.kt](../../tti/src/main/java/com/swm/dandi/tti/TTIHelper.kt)

```kotlin
interface TTIHelper {
    fun startTTITracking(page: TTIPage)          // 페이지 진입 시
    fun startTTITimeline(page, timelineCategory) // 구간 시작 (VIEW_CREATION / API_REQUEST / IMAGE_LOADED ...)
    fun endTTITimeline(page, timelineCategory)   // 구간 끝
    fun endTTITracking(page)                     // 최초 의미있는 렌더 완료
    fun shotTTILogging(page)                     // 페이지 이탈 시 로깅 발사
    fun addTTIMetaData(page, metadata, value)
}
```

적용 절차 (골든 예제: [FullScreenMediaFragment.kt](../../fullScreenMedia/presentation/src/main/java/com/swm/dandi/fullScreenMedia/presentation/FullScreenMediaFragment.kt)):

1. feature/domain에 `object {Feature}TTIPage : TTIPage` 정의 ([FullScreenMediaTTIPage.kt](../../fullScreenMedia/domain/src/main/java/com/swm/dandi/fullScreenMedia/domain/tti/FullScreenMediaTTIPage.kt))
2. 진입 시 `startTTITracking` + 구간별 `start/endTTITimeline` (뷰 생성 → 바인딩 → 이미지/API 로드)
3. 핵심 콘텐츠 표시 시 `endTTITracking`, 이탈 시(`onDestroyView` 등) `shotTTILogging`
4. API 구간(`API_REQUEST_READY_TIME` / `API_RESPONSE_TIME`) 마크는 **presentation(ViewModel)** 에서 기록한다 — [FullScreenMediaViewModel.kt](../../fullScreenMedia/presentation/src/main/java/com/swm/dandi/fullScreenMedia/presentation/FullScreenMediaViewModel.kt) 가 `ttiHelper`로 `start/endTTITimeline` 호출. `BaseUseCase`도 `ttiHelper`를 주입받지만 현재 어떤 TTI 메서드도 호출하지 않는 미사용 pass-through다 (TTI 마크는 UseCase가 아니라 ViewModel/Fragment에서 찍는다).

> 현재 TTI가 실제로 연결된 feature는 `fullScreenMedia` 하나뿐이다 (유일한 `TTIPage` 구현체 = `FullScreenMediaTTIPage`). `search` 는 `SearchUseCase`가 `ttiHelper`를 `BaseUseCase`로 넘기기만 할 뿐 TTI를 측정하지 않는다. (`SearchTTIPage` 는 [TTIPage.kt](../../tti/src/main/java/com/swm/dandi/tti/TTIPage.kt) 주석의 *예시*일 뿐 실제 구현체가 아니다.)

Logcat 출력 예: `Shot TTI Logging: ... tti.tti_time=..., tti.view_creation_time=..., tti.image_loaded_time=...`

기록 필드 ([TTIHelperImpl.kt](../../tti/src/main/java/com/swm/dandi/tti/TTIHelperImpl.kt) / [TTIEnums.kt](../../tti/src/main/java/com/swm/dandi/tti/TTIEnums.kt)): `page_name`, `api_request_ready_time`, `api_response_time`, `view_creation_time`, `view_binding_time`, `image_loaded_time`, `is_bounced`, `is_timeout`, `tti_log_version`. `endTTITracking` 가 20초(`TTI_TIMEOUT_MILLISECONDS`) 안에 도착하지 않으면 워치독이 `is_timeout`을 세우고 `stopView`로 "Timeout TTI Tracking"을 자동 발사한다.

## 2. JankStats (프레임 품질)

[jank/](../../common/presentation/src/main/java/com/swm/dandi/common/presentation/jank/) — AndroidX JankStats 기반.

- **페이지 단위는 자동**: AppNavHost가 라우트 렌더마다 `JankPageEffect(path)` 적용 — 새 화면은 등록만 하면 계측이 따라온다.
- 스크롤 리스트에는 `JankScrollWatcher(scrollableState)`를 화면에서 직접 추가 (스크롤 종료 시 구간 통계 flush). 파라미터는 `ScrollableState`라 LazyList(검색 `ContentsList`)·LazyGrid(즐겨찾기 `ContentsGrid`) 양쪽에 동일하게 쓴다.
- [JankReporter.kt](../../common/presentation/src/main/java/com/swm/dandi/common/presentation/jank/JankReporter.kt) 발사 조건: PAGE_EXIT / SCROLL_END / FROZEN_FRAME(700ms+ 즉시) / THRESHOLD_EXCEEDED(120프레임 이상 표본에서 jank 비율 5%+).
- 리포트 채널: DebugJankReport(Logcat `tag:"JankStats"`) ↔ RemoteJankReport — [JankModule](../../common/presentation/src/main/java/com/swm/dandi/common/presentation/jank/JankModule.kt)에서 `ApplicationInfo.FLAG_DEBUGGABLE`(BuildConfig.DEBUG 아님)로 분기해 바인딩 교체.

## 3. Baseline Profile / Macrobenchmark

`:baselineprofile` 모듈 — 콜드 스타트 핫 패스를 AOT 컴파일해 첫 프레임 단축.

```bash
./gradlew :app:generateBaselineProfile                     # 프로파일 재수집 (API 28+ 단말/에뮬)
./gradlew :baselineprofile:connectedBenchmarkAndroidTest   # 콜드 스타트 측정
```

- 시나리오: [BaselineProfileGenerator.kt](../../baselineprofile/src/main/java/com/swm/dandi/baselineprofile/BaselineProfileGenerator.kt) — 콜드 스타트 → 검색("kakao") → 리스트 fling 3회 → 상세(전체화면) 진입. UI 요소는 **testTag** (`search_text_field`, `search_item`)로 찾는다.
- 생성/머지된 프로파일 산출물은 `app/src/release/generated/baselineProfiles/baseline-prof.txt` 에 위치한다. `androidx.baselineprofile` 플러그인이 `:app`·`:baselineprofile` 양쪽에 적용되어 빌드 시 이 파일을 머지/패키징한다.
- **새 핵심 플로우를 추가하면 이 시나리오에 반영하고 프로파일을 재생성**한다. 화면의 testTag를 지우면 시나리오가 깨진다.
- app의 `benchmark` buildType은 release와 동일 최적화 + profileable — 수집 전용.
