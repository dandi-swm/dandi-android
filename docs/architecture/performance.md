# 성능 계측 (TTI / JankStats / Baseline Profile)

## 왜

- 화면이 늘어날 때마다 "이 페이지 왜 느리지"를 측정 없이 추측하지 않도록, **페이지 단위 계측을 아키텍처에 내장**한다.
- 새 feature를 만들 때 TTI/Jank 계측을 따라 붙이는 것이 컨벤션이다 (필수는 페이지 단위 Jank — AppNavHost가 자동 적용, TTI는 핵심 화면에 선택 적용).

## 1. TTI (Time To Interactive)

순수 JVM 모듈 `:tti`. 인터페이스: [TTIHelper.kt](../../tti/src/main/java/com/dandi/nyummy/tti/TTIHelper.kt)

`TTIHelper` 는 **`@ViewModelScoped`** 로 바인딩되어 ViewModel 인스턴스당 1개 생성되고, 같은 ViewModel 에 주입되는 UseCase 들과 동일 인스턴스를 공유한다([CommonPresentationModule.kt](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/CommonPresentationModule.kt)의 `CommonViewModelModule`). reporter / logger / `@TtiDispatcher` 는 Hilt 싱글톤이고, 프로세스 전역이어야 하는 페이지별 인스턴스 번호 카운터만 `TTIHelperImpl` 의 companion 에 둔다.

```kotlin
interface TTIHelper {                    // 한 번의 TTI 측정(화면 인스턴스 1개)에 대한 핸들
    fun startTTITracking(page: TTIPage)   // ViewModel init 에서 1회 호출 (중복 호출은 로그만 남기고 무시)
    fun startTTITimeline(category)        // 구간 시작 (VIEW_CREATION / API_REQUEST / IMAGE_LOADED ...)
    fun endTTITimeline(category)          // 구간 끝
    fun endTTITracking()                  // 사용자가 쓸 수 있는 상태가 된 순간
    fun shotTTILogging()                  // 리포트 발사
    fun addTTIMetaData(metadata, value)
}
```

- `startTTITracking` 전에 들어온 호출은 무시된다. 트래킹을 시작하지 않는 ViewModel 아래의 UseCase 가 마크를 찍어도 안전하다.
- **순서 보장**: 모든 마크는 `@TtiDispatcher`(= `@IoDispatcher.limitedParallelism(1)`, [CoroutineModule.kt](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/coroutine/CoroutineModule.kt)) 한 줄에서 호출한 순서대로 실행된다.
- **멀티 인스턴스**: 같은 페이지가 여러 번 열려도 인스턴스별로 따로 측정된다. 로그 키는 `"{pageName}#{instanceNo}_{millis}"` 이고 `instance_no` 는 페이지별로 단조 증가한다(프로세스 재시작 시 1부터).
- **리포트는 인스턴스당 정확히 1회**: `shotTTILogging` 과 20초(`TTI_TIMEOUT_MILLISECONDS`) 타임아웃 중 먼저 오는 쪽이다. 발사 후 늦게 온 마크와 shot 은 무시되고 인스턴스 scope 는 정리된다.
- **이탈 안전망**: ViewModel 이 사라질 때(`ViewModelLifecycle.addOnClearedListener`) 아직 보내지 않은 측정을 `shotTTILogging()` 으로 보낸다. Compose 화면은 `onDestroyView` 같은 이탈 콜백이 없어서, 끝 표시까지 마친 측정이 보고되지 않는 일을 막는다.
- **시계**: 구간 길이만 쓰므로 단조 시계(`System.nanoTime()`)로 잰다. 측정 중 기기 시각이 바뀌어도 값이 틀어지지 않는다.

적용 절차:

1. feature/domain에 `object {Feature}TTIPage : TTIPage` 를 정의한다. `timelines` 에는 **실제로 측정하는 구간만** 넣는다. 마지막 타임라인이 완성되어야 `endTTITracking()` 이 받아들여지므로, 찍지 않는 구간을 넣으면 매번 미완료로 리포트된다.
2. ViewModel 생성자로 `val ttiHelper: TTIHelper` 를 주입받고, `init` 에서 다른 작업보다 먼저 `ttiHelper.startTTITracking({Feature}TTIPage)` 를 호출한다.
3. View(Composable)는 `viewModel.ttiHelper` 로 구간을 찍고, 핵심 콘텐츠가 보일 때 `endTTITracking()` 을 호출한다. 이탈 시 shot 은 위 안전망이 맡으므로 필요할 때만 직접 부른다.
4. `BaseUseCase` 도 같은 `ttiHelper` 를 받으므로 UseCase 안에서 API 구간(`API_REQUEST_READY_TIME` / `API_RESPONSE_TIME`)을 suspend 호출과 같은 코루틴 안에서 start/end 로 감싼다.
5. UseCase 는 ViewModel 에서만 주입한다. `TTIHelper` 가 ViewModel 스코프라 Activity, Worker, Singleton 에서는 주입할 수 없다.

현재 TTI가 연결된 화면은 인트로 하나다([IntroViewModel.kt](../../intro/presentation/src/main/java/com/dandi/nyummy/intro/presentation/IntroViewModel.kt), [GetIntroUseCase.kt](../../intro/domain/src/main/java/com/dandi/nyummy/intro/domain/GetIntroUseCase.kt)).
- 인트로 TTI 는 앱 진입(VM init)부터 버전 확인이 끝나 다음 행동이 정해질 때까지다. `GetIntroUseCase` 가 버전 확인 직후 `endTTITracking()` 을 부르므로 강제 업데이트 다이얼로그, 진행바 채움 대기, 화면 이동은 들어가지 않는다.
- 버전 확인이 실패하면 끝을 찍지 않는다. 재시도가 성공하면 그때 보고되고, 아니면 이탈 안전망이나 타임아웃으로 미완료 보고된다.
- 첫 실행 권한 안내를 기다린 시간은 구간에서 뺄 수 없어 `user_wait_included=true` 로 표시한다.

Logcat 출력 예(디버그 빌드, `[TTI]` 태그): `Shot TTI Logging : intro#1_... / {tti.page_name=intro, tti.instance_no=1, tti.is_bounced=false, tti.is_timeout=false, tti.tti_time=..., tti.api_response_time=..., ...}`

기록 필드 ([TTIInfo.kt](../../tti/src/main/java/com/dandi/nyummy/tti/TTIInfo.kt) / [TTIEnums.kt](../../tti/src/main/java/com/dandi/nyummy/tti/TTIEnums.kt)): `page_name`, `instance_no`, `tti_time`, `api_request_ready_time`, `api_response_time`, `view_creation_time`, `view_binding_time`, `image_loaded_time`(단위 ns, 측정하지 않은 구간은 -1), `is_bounced`(측정 구간 중 빠진 것이 있음), `is_timeout`(20초 안에 끝나지 않아 타임아웃으로 보고됨), `tti_log_version`.

외부 전송(`TTIReporter`, release 빌드의 `RemoteTTILogger`)은 관측 도구가 연결되기 전까지 no-op이다.

## 2. JankStats (프레임 품질)

[jank/](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/jank/) — AndroidX JankStats 기반.

- **페이지 단위는 자동**: AppNavHost가 라우트 렌더마다 `JankPageEffect(path)` 적용 — 새 화면은 등록만 하면 계측이 따라온다.
- 스크롤 리스트에는 `JankScrollWatcher(scrollableState)`를 화면에서 직접 추가 (스크롤 종료 시 구간 통계 flush). 파라미터는 `ScrollableState`라 LazyList(검색 `ContentsList`)·LazyGrid(즐겨찾기 `ContentsGrid`) 양쪽에 동일하게 쓴다.
- [JankReporter.kt](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/jank/JankReporter.kt) 발사 조건: PAGE_EXIT / SCROLL_END / FROZEN_FRAME(700ms+ 즉시) / THRESHOLD_EXCEEDED(120프레임 이상 표본에서 jank 비율 5%+).
- 리포트 채널: DebugJankReport(Logcat `tag:"JankStats"`) ↔ RemoteJankReport — [JankModule](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/jank/JankModule.kt)에서 `ApplicationInfo.FLAG_DEBUGGABLE`(BuildConfig.DEBUG 아님)로 분기해 바인딩 교체.

## 3. Baseline Profile / Macrobenchmark

`:baselineprofile` 모듈 — 콜드 스타트 핫 패스를 AOT 컴파일해 첫 프레임 단축.

```bash
./gradlew :app:generateBaselineProfile                     # 프로파일 재수집 (API 28+ 단말/에뮬)
./gradlew :baselineprofile:connectedBenchmarkAndroidTest   # 콜드 스타트 측정
```

- 레거시 시나리오 설명: 콜드 스타트 → 검색("kakao") → 리스트 fling 3회 → 상세(전체화면) 진입. UI 요소는 **testTag** (`search_text_field`, `search_item`)로 찾는다. 현재 [BaselineProfileGenerator.kt](../../baselineprofile/src/main/java/com/dandi/nyummy/baselineprofile/BaselineProfileGenerator.kt)는 콜드 스타트만 수집한다.
- 생성/머지된 프로파일 산출물은 `app/src/release/generated/baselineProfiles/baseline-prof.txt` 에 위치한다. `androidx.baselineprofile` 플러그인이 `:app`·`:baselineprofile` 양쪽에 적용되어 빌드 시 이 파일을 머지/패키징한다.
- **새 핵심 플로우를 추가하면 이 시나리오에 반영하고 프로파일을 재생성**한다. 화면의 testTag를 지우면 시나리오가 깨진다.
- app의 `benchmark` buildType은 release와 동일 최적화 + profileable — 수집 전용.
