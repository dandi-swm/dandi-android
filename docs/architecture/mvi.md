# MVI 패턴

## 왜

- 상태 변이를 `reduce()` 한 곳으로 모으면 **상태 버그의 탐색 범위가 한 함수**로 줄고, 코루틴 여러 곳에서 `update {}`를 직접 호출할 때 생기는 레이스/유실을 차단한다.
- View 진입점을 `onIntent()` 하나로 통일하면 UI 이벤트의 전체 목록이 `Intent` sealed interface 한 파일에 드러난다.

## 계약 — MviViewModel

골든 예제: [MviViewModel.kt](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/mvi/MviViewModel.kt)

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

골든 예제는 [history/presentation](../../history/presentation/src/main/java/com/dandi/nyummy/history/presentation/)이다.

| 파일 | 역할 | 골든 예제 |
|---|---|---|
| `{Feature}Intent.kt` | `sealed interface : MviIntent` — 사용자 입력 전수 | `HistoryIntent.kt` |
| `{Feature}ReducerEvent.kt` | `sealed interface : ReducerEvent` — 상태 변이 원인 전수 | `HistoryReducerEvent.kt` |
| `{Feature}UIState.kt` | `data class : UiState` + `companion val empty` | `HistoryUIState.kt` |
| `{Feature}ViewModel.kt` | `@HiltViewModel`, onIntent/reduce 구현 | `HistoryViewModel.kt` |
| `{Feature}Page.kt` | `collectAsStateWithLifecycle()` + onIntent 호출만 | `HistoryPage.kt` |

모든 화면이 Compose `Page` + `MviViewModel`이다. 상태가 거의 없는 화면(인트로, "준비 중" 플레이스홀더)도 예외 없이 같은 세트로 만든다.

### 라우트 인자를 받는 ViewModel (signup 골든 예제)

라우트 인자로 초기 상태가 달라지는 화면은 assisted injection으로 인자를 ViewModel 생성자에 넘긴다 — [SignUpViewModel.kt](../../auth/presentation/src/main/java/com/dandi/nyummy/auth/presentation/SignUpViewModel.kt) + [SignUpPage.kt](../../auth/presentation/src/main/java/com/dandi/nyummy/auth/presentation/SignUpPage.kt).

- **ViewModel**: `@HiltViewModel(assistedFactory = Factory::class)` + `@AssistedInject constructor(@Assisted arg: ..., ...)`, 안에 `@AssistedFactory interface Factory { fun create(arg): XxxViewModel }`.
- **Page**: `viewModel: XxxViewModel = hiltViewModel<XxxViewModel, XxxViewModel.Factory>(creationCallback = { it.create(arg) })`를 기본 파라미터로 둔다.

## 규칙

1. UIState의 컬렉션은 **ImmutableList/ImmutableSet** (kotlinx.collections.immutable). `List` 금지 — Compose skip 불가.
2. 상태 가공 로직은 UIState의 메서드로 (예: `HistoryUIState.withMealStatus`, `commitMealNameEdit`) — reduce는 분기만, Compose는 반영만.
3. 부수효과(UseCase 호출, 네비게이션, 스낵바)는 onIntent 쪽 private 함수에서. reduce는 순수 함수.
4. 에러 UX는 `messageHelper.showSnackBar(...)` / 다이얼로그 (error-handling.md).

## 페이징 패턴 (mailbox 골든 예제)

서버는 cursor 방식으로 페이지를 준다. 골든 예제: [MailboxViewModel.kt](../../mailbox/presentation/src/main/java/com/dandi/nyummy/mailbox/presentation/list/MailboxViewModel.kt)의 `loadMoreMails()`, [MailboxUIState.kt](../../mailbox/presentation/src/main/java/com/dandi/nyummy/mailbox/presentation/list/MailboxUIState.kt)의 `MailItemsUiState`.

상태 필드: `isLoaded / isLoadingMore / nextCursor(마지막 묶음이면 null) / mails(UI 모델)`

- 다음 묶음 가드: `if (!isLoaded || isLoadingMore || nextCursor == null || 첫 묶음 읽는 중) return`
- 이어 붙이기: reduce에서 이미 있는 id를 걸러 내고 뒤에 붙인다(재정렬 금지 — LazyColumn key 충돌 방지). 실패하면 `isLoadingMore`만 내린다.
- 목록 끝의 "더 보기" 트리거는 디자인 시스템의 [NyummyLoadMore.kt](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/foundation/NyummyLoadMore.kt)를 쓴다.
- 외부 변화 구독: `init`에서 Flow를 `launchIn(viewModelScope)`으로 collect → `dispatch(...)` — 직접 state 갱신 금지
