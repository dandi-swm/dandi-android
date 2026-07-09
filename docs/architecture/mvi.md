# MVI 패턴

## 왜

- 상태 변이를 `reduce()` 한 곳으로 모으면 **상태 버그의 탐색 범위가 한 함수**로 줄고, 코루틴 여러 곳에서 `update {}`를 직접 호출할 때 생기는 레이스/유실을 차단한다.
- View 진입점을 `onIntent()` 하나로 통일하면 UI 이벤트의 전체 목록이 `Intent` sealed interface 한 파일에 드러난다.

## 계약 — MviViewModel

골든 예제: [MviViewModel.kt](../../common/presentation/src/main/java/com/swm/dandi/common/presentation/mvi/MviViewModel.kt)

```kotlin
interface MviIntent      // View → ViewModel (사용자 입력)
@Stable interface UiState // ViewModel → View (불변 상태)
interface ReducerEvent   // 내부 이벤트 — 외부에서 직접 dispatch 금지

abstract class MviViewModel<I : MviIntent, S : UiState, E : ReducerEvent>(
    initialState: S,
) : ViewModel() {
    val uiState: StateFlow<S>            // View에 노출되는 유일한 상태
    protected val currentState: S        // reduce 밖에서 현재 상태 읽기
    abstract fun onIntent(intent: I)     // 유일한 외부 진입점
    protected abstract fun reduce(state: S, event: E): S
    protected fun dispatch(event: E)     // reduce를 거쳐 uiState 갱신
}
```

흐름: `View → onIntent(Intent) → (UseCase 호출 등 부수효과) → dispatch(ReducerEvent) → reduce → StateFlow → Compose`

## 파일 세트 (feature/presentation)

| 파일 | 역할 | 골든 예제 |
|---|---|---|
| `{Feature}Intent.kt` | `sealed interface : MviIntent` — 사용자 입력 전수 | [SearchIntent.kt](../../search/presentation/src/main/java/com/swm/dandi/search/presentation/SearchIntent.kt) |
| `{Feature}ReducerEvent.kt` | `sealed interface : ReducerEvent` — 상태 변이 원인 전수 | [SearchReducerEvent.kt](../../search/presentation/src/main/java/com/swm/dandi/search/presentation/SearchReducerEvent.kt) |
| `{Feature}UIState.kt` | `data class : UiState` + `companion val empty` | [SearchUIState.kt](../../search/presentation/src/main/java/com/swm/dandi/search/presentation/SearchUIState.kt) |
| `{Feature}ViewModel.kt` | `@HiltViewModel`, onIntent/reduce 구현 | [SearchViewModel.kt](../../search/presentation/src/main/java/com/swm/dandi/search/presentation/SearchViewModel.kt) |
| `{Feature}Page.kt` | `collectAsStateWithLifecycle()` + onIntent 호출만 | [SearchPage.kt](../../search/presentation/src/main/java/com/swm/dandi/search/presentation/SearchPage.kt) |

> View 레이어는 화면마다 다르다 — 위 표는 Compose `Page` 변형(search/favorite)이고, intro 는 비-MVI 일반 ViewModel 변형(`IntroPage` 은 빈 Composable 스텁), fullScreenMedia 는 아래의 **Fragment 호스팅 MVI 변형**이다. Intent/UIState/ReducerEvent/MviViewModel 계약은 변형과 무관하게 동일하다.
> 하나의 feature 안에 독립 화면이 여러 개 있으면 presentation MVI/Page 세트는 화면 prefix를 사용할 수 있다(예: `PreviousMealPage`, `NewFoodViewModel`). 이 경우에도 Repository/ApiService/DataSource/DataModule/ErrorType 같은 domain/data 계약은 feature prefix를 유지한다.

### Fragment 호스팅 MVI 변형 (fullScreenMedia 골든 예제)

상세화면은 Compose `Page` 가 **없다**. ViewBinding + ViewPager2 기반의 `Fragment` 가 동일한 `MviViewModel` 을 호스팅한다 — [FullScreenMediaFragment.kt](../../fullScreenMedia/presentation/src/main/java/com/swm/dandi/fullScreenMedia/presentation/FullScreenMediaFragment.kt) + [FullScreenMediaPagerAdapter.kt](../../fullScreenMedia/presentation/src/main/java/com/swm/dandi/fullScreenMedia/presentation/FullScreenMediaPagerAdapter.kt). fullScreenMedia 를 복제할 때 이 변형을 따른다.

- **ViewModel 주입**: ViewModel 은 `@HiltViewModel(assistedFactory = Factory::class)` + `@AssistedInject` (라우트 Args 를 `@Assisted` 로 받음). Fragment 는 `by viewModels(extrasProducer = { ...withCreationCallback<Factory> { it.create(args) } })` 로 주입.
- **상태 수집**: `collectAsStateWithLifecycle()` 대신 `viewLifecycleOwner.lifecycleScope.launch { repeatOnLifecycle(STARTED) { viewModel.uiState.collect(::render) } }` 로 collect → `render(state)` 가 ViewBinding 갱신.
- **목록 반영**: 페이저 데이터는 `pagerAdapter.submit(state.mediaItems)` 로 넘기고, `binding.mediaPager.setCurrentItem(state.currentIndex, false)` 로 위치 동기화.
- **입력 진입**: 클릭/페이지 전환은 모두 `viewModel.onIntent(FullScreenMediaIntent...)` 단일 진입점으로 (예: `OnPageChangeCallback.onPageSelected` → `SelectPage`).

## 규칙

1. UIState의 컬렉션은 **ImmutableList/ImmutableSet** (kotlinx.collections.immutable). `List` 금지 — Compose skip 불가.
2. 상태 가공 로직은 UIState의 메서드로 (예: `appendPage`, `withFavorites`) — reduce는 분기만, Compose는 반영만.
3. 부수효과(UseCase 호출, 네비게이션, 스낵바)는 onIntent 쪽 private 함수에서. reduce는 순수 함수.
4. 에러 UX는 `messageHelper.showSnackBar(...)` / 다이얼로그 (error-handling.md).

## 페이징 패턴 (search 골든 예제)

상태 필드: `isLoading / isLoadingMore / isLastPage / currentPage / rawMediaItems(원본) / searchItemList(UI 모델)`

- `loadNextPage()` 가드: `if (!hasSearched || isLoading || isLoadingMore || isLastPage) return`
- `appendPage(result, page)`: 기존 urlKey 중복 제거 후 뒤에 이어붙임(재정렬 금지 — LazyColumn key 충돌 방지), `result.isEnd || items.isEmpty()` → isLastPage
- 외부 변화 구독(예: 즐겨찾기): `init { observeFavorites() }`에서 Flow를 `launchIn(viewModelScope)`으로 collect → `dispatch(FavoritesChanged)` — 직접 state 갱신 금지

## 단순 화면 변형 (intro 골든 예제)

상태가 없는 화면은 MviViewModel 없이 일반 ViewModel + `init { useCase() }`도 허용 — [IntroViewModel.kt](../../intro/presentation/src/main/java/com/swm/dandi/intro/presentation/IntroViewModel.kt). 단, 상태가 1개라도 생기면 MVI 세트로 작성한다.
