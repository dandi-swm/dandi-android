# 에러 핸들링

## 왜

- HTTP 에러를 **data가 던지고 domain이 처리**하도록 고정하면, "어디서 다이얼로그를 띄울지"가 feature마다 흔들리지 않는다.
- 서버 에러 코드(`cause.message`)를 feature별 enum(`HttpErrorType`)으로 매핑해, 공통 처리(401/404/5xx)와 feature 고유 처리를 기계적으로 분리한다.
- 서버가 주는 `message`와 HTTP 상태 문자열은 **디버그용**이다. 사용자에게는 앱이 정한 문구(`errorMsg`, 공통 문구)만 보여 준다.
- 도메인은 순수 Kotlin이라 문구를 한국어로 적지 않는다. `errorMsg`와 UseCase 안내 문구는 `StringResource` enum으로 고르고 `resourceHelper.getString(...)`으로 꺼낸다. 문구 자체는 `common/presentation/src/main/res/values*/domain_messages.xml`(영어 기본·한국어·일본어)에 있고 `ResourceHelperImpl`이 enum을 리소스로 바꾼다.
- ViewModel이 스낵바·다이얼로그에 넘길 문구는 feature 문자열 리소스에 두고 `messageRes` 오버로드나 `StringProvider`(common:presentation)로 읽는다.

## 전파 체인

```
Retrofit Response ─▶ BaseRemoteDataSource.checkResponse()      [data]   실패 시 HttpResponseException throw
OkHttp 네트워크 실패 ─▶ IOException                             [data]   (토큰 재발급의 일시적인 실패도 IOException)
                 ─▶ RepositoryImpl (변환만, 처리 안 함)          [data]
                 ─▶ UseCase: BaseUseCase.request { }             [domain] ① 서버 code ② 공통 처리 ③ 네트워크 ④ 화면 기본 안내
                 ─▶ ViewModel: Result.onSuccess / onFailure      [presentation] UI 상태 복구 (+ 필요하면 UseCase.isNotified로 안내 여부 확인)
```

## 계약

골든 예제: [DomainFailure.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/error/DomainFailure.kt), [HttpError.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/error/HttpError.kt), [ErrorParser.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/error/ErrorParser.kt), [BaseUseCase.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/base/BaseUseCase.kt), [BaseRemoteDataSource.kt](../../common/data/src/main/java/com/dandi/nyummy/common/data/BaseRemoteDataSource.kt)

```kotlin
class HttpResponseException(
    val status: HttpResponseStatus,   // enum (code, msg)
    val rawCode: Int,
    val errorRequestUrl: String,
    msg: String? = null,
    cause: Throwable? = null,         // errorBody — 서버 에러 type 문자열이 여기 담김
) : Exception(msg, cause)

interface HttpErrorType { val type: String; val errorMsg: StringResource?; val isHandledOnDomain: Boolean }

val HttpResponseException.serverErrorCode: String?            // 에러 바디의 code. 없으면 null
fun HttpResponseException.isSessionExpired(): Boolean         // 401 && (code 없음 || "api.auth.unauthorized" || "api.auth.tokenExpired")

// 공통 안내 범위
enum class CommonErrorPolicy { ALL, SIGNED_OUT, SESSION_ONLY, NONE }

// 예외를 처리 순서대로 나눈 결과. CancellationException은 나누지 않고 다시 던진다.
sealed interface DomainFailure<out E : HttpErrorType> { Business, Common, UnknownHttp, Network, Unexpected }
fun <E : HttpErrorType> Exception.toDomainFailure(errorTypes: List<E>, policy: CommonErrorPolicy = ALL): DomainFailure<E>

// runCatching과 같지만 코루틴 취소는 다시 던진다. 조용히 넘겨도 되는 기기 저장, 장식용 조회에 쓴다.
suspend inline fun <T> runSuspendCatching(block: suspend () -> T): Result<T>
```

- data는 에러 바디의 `code`만 `cause`에 담는다. 바디에 `code`가 없으면 `cause`는 null이고, 원문 바디는 디버그용으로 `message`에만 남는다.

### 처리 순서 (`toDomainFailure`가 강제)

| 순서 | 조건 | 결과 | 안내 |
|---|---|---|---|
| ① | 서버 code가 기능의 ErrorType에 있다 | `Business(type)` | UseCase의 `onBusinessError`가 결과를 고른다 |
| ② | 정책이 맡는 공통 오류(로그인 만료 401, 404, 5xx) | `Common` | `executeCommonErrorHanding` |
| ③ | 그 밖의 응답 오류(등록하지 않은 code의 4xx, 정책 밖의 공통 오류) | `UnknownHttp` | `onUnknownError` |
| ④ | `IOException`(응답 전에 끊김) | `Network` | `onNetworkError`, 없으면 `onUnknownError` |
| ⑤ | 그 밖의 `Exception`(응답 해석 실패 등) | `Unexpected` | `onUnknownError` |
| - | `CancellationException` | 다시 던짐 | 없음 |

### 공통 안내 범위 (`CommonErrorPolicy`)

| 값 | 로그인 만료 401 | 404, 5xx | 쓰는 곳 |
|---|---|---|---|
| `ALL` (기본) | 공통 안내 | 공통 안내 | 로그인한 화면 대부분 |
| `SIGNED_OUT` | 화면 안내(로그인 만료 아님) | 공통 안내 | 이메일 로그인, 가입, 인증 코드, 비밀번호 찾기 |
| `SESSION_ONLY` | 공통 안내 | 화면 안내 | 화면을 꾸미는 조회, 토글 저장, 기록 취소 |
| `NONE` | 화면 안내 | 화면 안내 | 소셜 로그인(제공자별 문구) |

## UseCase 처리 패턴

골든 예제: [DeleteMealUseCase.kt](../../history/domain/src/main/java/com/dandi/nyummy/history/domain/DeleteMealUseCase.kt) + [HistoryErrorType.kt](../../history/domain/src/main/java/com/dandi/nyummy/history/domain/HistoryErrorType.kt)

```kotlin
// BaseUseCase 생성자는 resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler.
// 모두 super로 그대로 넘긴다. sessionExpiryHandler는 Hilt가 넣고, 단위 테스트는 생략할 수 있게 기본값 null을 둔다.
class DeleteMealUseCase @Inject constructor(
    private val repository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long): Result<Unit> = request(
        HistoryErrorType.entries,
        onBusinessError = { type, cause ->                       // ① 서버 code: 결과를 직접 고른다
            if (type == HistoryErrorType.MEAL_NOT_FOUND) Result.success(Unit) else { showError(type.errorMsg); Result.failure(cause) }
        },
        // ② 공통 오류(401, 404, 5xx)는 request가 안내한다.
        onUnknownError = { showError(StringResource.HISTORY_DELETE_FAILED) },  // ③④⑤ 화면 기본 안내
    ) {
        repository.deleteMeal(mealId)
    }
}
```

- **UseCase에 try/catch를 새로 쓰지 않는다.** `request`가 성공 감싸기, 취소 다시 던지기, 처리 순서, 실패 반환을 맡는다. 등록할 서버 code가 없으면 `request(commonErrors = ...) { }` 오버로드를 쓴다.
- `Result`가 아닌 값을 돌려주거나(구매, 코인 받기, 분석 대기) 모든 실패에 분석 사유를 남겨야 하면 `catch (e: Exception)`에서 `e.toDomainFailure(...)`로 나누고 `notifyFailure(...)`로 안내한다. 이때도 `CancellationException`을 먼저 다시 던진다.
- 공통 404 안내는 "준비 중" 다이얼로그 뒤 뒤로 간다. 화면에 남아야 하는 요청(구매)은 `SESSION_ONLY`로 404와 5xx를 공통 안내에서 빼고, `onUnknownError`에서 이동 없는 `showTemporaryErrorDialog()`를 쓴다.
- **401이라고 다 로그인 만료가 아니다.** 서버는 로그인 세션(AccessToken) 실패에만 `api.auth.unauthorized`(토큰이 없거나 올바르지 않음)와 `api.auth.tokenExpired`(만료)를 주고, 회원가입·비밀번호 찾기의 인증 토큰 오류는 `api.auth.invalidEmailChallengeToken`, `api.auth.invalidVerifiedToken`처럼 따로 준다(백엔드 #103). 그래서 code가 없거나 이 두 code인 401만 로그인 만료로 본다. 로그인 전 화면은 `SIGNED_OUT` 정책으로 code 없는 401도 그 화면의 실패로 안내한다.
- `isHandledOnDomain = false`로 등록한 code는 `request`가 아무 안내도 하지 않고 실패로 돌려준다. 공통 안내로 새지 않는다.
- 화면이 실패를 따로 안내해야 하면, `HttpResponseException`을 직접 열지 말고 UseCase에 `fun isNotified(error: Throwable) = error.isCommonErrorNotified(ErrorType.entries, 정책)`을 두고 ViewModel이 그것을 본다.
- 실패해도 흐름을 이어 가는 작업(기기 저장, 장식용 조회)은 `runSuspendCatching { }`을 쓴다. suspend 호출을 `runCatching`으로 감싸지 않는다(취소를 삼킨다).

## 세션 만료 (로그인 만료)

```
요청 401 ─▶ TokenAuthenticator: Mutex로 재발급 1회
             ├─ 성공 → 새 토큰으로 재시도(화면은 모름)
             ├─ refresh 401(리프레시 토큰 무효) → 토큰 삭제, 원래 401 전파 ─▶ 세션 만료
             └─ 네트워크 끊김, 5xx → IOException ─▶ 화면은 네트워크 오류, 토큰은 남음
UseCase ─▶ executeCommonErrorHanding ─▶ SessionExpiryHandler.onSessionExpired()
             ├─ 처음 한 번만 통과(AtomicBoolean). 동시에 401이 여러 개 와도 정리, 다이얼로그, 이동은 한 번
             ├─ 이 기기 세션 정리: 로그인 토큰, 푸시 토큰, 냐미 기억, 튜토리얼, 위젯 (로그아웃과 같은 정리)
             ├─ 닫을 수 없고 다른 다이얼로그가 덮지 못하는 "로그인 만료" 다이얼로그(showPinnedDialog)
             └─ 확인 → 정리가 끝나기를 기다린 뒤 navigateToInitial() → NavHost가 스택을 비우고 /login
로그인, 소셜 로그인, 가입 성공 ─▶ SessionExpiryHandler.onSignedIn() (다음 만료를 다시 처리)
```

- 상태는 둘이다. `isExpired`(만료를 처리했고 아직 다시 로그인하지 않음)는 다시 로그인할 때까지 남아, 그 사이의 401을 다시 정리하거나 안내하지 않는다. `isNoticeShowing`(만료 안내를 띄운 뒤 로그인 화면으로 옮기기 전)인 동안만 다른 공통 안내(404, 5xx)를 띄우지 않는다. 로그인 화면에 도착하면 풀려서 로그인, 가입 요청의 404와 5xx를 평소처럼 안내한다.
- 만료 다이얼로그는 고정 다이얼로그다. RootComposable은 다이얼로그 자리가 하나라, 고정 다이얼로그가 떠 있는 동안 오는 다른 1버튼, 2버튼 다이얼로그(화면의 비즈니스 안내 포함)를 버린다(`canShowDialogOver`). 덮이면 확인 버튼의 로그인 이동이 사라져 토큰 없는 화면에 갇힌다.
- 결과 값을 돌려주는 UseCase는 만료를 따로 알린다. 예: 영양 계산 대기(`AwaitMealAnalysisUseCase`)는 `SESSION_EXPIRED`를 돌려주고, 화면은 지연 안내나 뒤로 가기를 하지 않는다.
- 로그아웃, 탈퇴도 같은 정리를 하고, 정리 하나하나를 `runSuspendCatching`으로 감싸 기기 저장소 오류가 나도 로그인 화면으로 간다.

- 네트워크 레이어는 이동이나 정리를 트리거하지 않는다. 세션 만료의 주체는 domain(`SessionExpiryHandler`)이다.
- 토큰을 지운 뒤에는 Authenticator가 재발급하지 않으므로 자동 로그인이 다시 일어나지 않는다.

## {Feature}ErrorType 규칙

- 위치: **`<feature>/domain/`** (entity 아님)
- `enum class : HttpErrorType`, `type` = 서버가 내려주는 에러 식별 문자열 (예: `"api.intro.requiredForceUpdate"`)
- `type`은 백엔드 `exception/errorcode/*ErrorCode.kt`의 `code`와 철자까지 같아야 한다. 서버 코드가 바뀌면(예: `api.ses.emailSendFailed` → `api.email.sendFailed`) 배포가 끝날 때까지 이전 code도 함께 둔다.
- UseCase에서 처리하지 않을 항목은 `isHandledOnDomain = false`로 두고 presentation에서 분기

## 사용자 노출 (MessageHelper)

domain에서 직접 사용 가능한 인터페이스 ([MessageHelper.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/helper/MessageHelper.kt)): `showToast / showSnackBar(iconType, messageText|messageRes, CTA) / showOneButtonDialog(cantIgnore=true면 닫기 불가) / showPinnedDialog(닫기 불가, 떠 있는 동안 다른 다이얼로그를 버림. 로그인 만료 전용) / showTwoButtonDialog`. 구현은 presentation의 MessageHelperImpl — RootComposable의 SnackbarHost/ToastHost(`NyummyToastHostState`)/다이얼로그가 `effect: Flow<MessageEffect>`를 구독한다.
