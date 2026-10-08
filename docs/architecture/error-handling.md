# 에러 핸들링

## 왜

- HTTP 에러를 **data가 던지고 domain이 처리**하도록 고정하면, "어디서 다이얼로그를 띄울지"가 feature마다 흔들리지 않는다.
- 서버 에러 코드(`cause.message`)를 feature별 enum(`HttpErrorType`)으로 매핑해, 공통 처리(401/404/5xx)와 feature 고유 처리를 기계적으로 분리한다.
- 서버가 주는 `message`와 HTTP 상태 문자열은 **디버그용**이다. 사용자에게는 앱이 정한 문구(`errorMsg`, 공통 문구)만 보여 준다.

## 전파 체인

```
Retrofit Response ─▶ BaseRemoteDataSource.checkResponse()      [data]   실패 시 HttpResponseException throw
                 ─▶ RepositoryImpl (변환만, 처리 안 함)          [data]
                 ─▶ UseCase: try/catch                          [domain] ① 서버 code 처리 ② 공통 처리 ③ 화면 기본 안내
                 ─▶ ViewModel: Result.onFailure / runCatching   [presentation] UI 상태 복구 + 스낵바
```

## 계약

골든 예제: [HttpError.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/error/HttpError.kt), [ErrorParser.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/error/ErrorParser.kt), [BaseRemoteDataSource.kt](../../common/data/src/main/java/com/dandi/nyummy/common/data/BaseRemoteDataSource.kt)

```kotlin
class HttpResponseException(
    val status: HttpResponseStatus,   // enum (code, msg)
    val rawCode: Int,
    val errorRequestUrl: String,
    msg: String? = null,
    cause: Throwable? = null,         // errorBody — 서버 에러 type 문자열이 여기 담김
) : Exception(msg, cause)

interface HttpErrorType { val type: String; val errorMsg: String; val isHandledOnDomain: Boolean }

fun HttpResponseException.isCommonErrorHandling(): Boolean   // 401 || 404 || 5xx
inline fun <reified ErrorType> HttpResponseException.handlingErrorOnUseCase(): ErrorType?
        where ErrorType : Enum<ErrorType>, ErrorType : HttpErrorType
    // enum 중 type == cause.message && isHandledOnDomain 인 것
```

## UseCase 처리 패턴

> 아래 `Intro` 계열 이름은 에러 처리 흐름을 보여주는 레거시 예시 식별자이며, 현재 저장소의 모듈 경로를 뜻하지 않습니다.

```kotlin
// BaseUseCase 생성자는 4개: resourceHelper, messageHelper, navigationHelper, ttiHelper.
// 모두 super로 그대로 넘긴다.
class GetIntroUseCase @Inject constructor(
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    operator fun invoke(): Result<IntroVO> = try {
        // 현재 intro의 repository 호출은 스텁(주석) 상태이므로 catch는 예시용이다.
        // val result = introRepository.getIntro()
        Result.success(...)
    } catch (e: HttpResponseException) {
        handleHttpError<IntroErrorType>(
            e,
            onDomainError = { errorType ->                                        // ① 서버 code
                when (errorType) {
                    IntroErrorType.REQUIRED_FORCE_UPDATE -> messageHelper.showOneButtonDialog(
                        cantIgnore = true, descText = errorType.errorMsg, onClickButton = { ... })
                }
            },
            // ② 맞는 code가 없으면 BaseUseCase가 공통 오류(401, 404, 5xx)로 안내한다.
            onUnknownError = { messageHelper.showSnackBar(messageText = "...") }, // ③ 화면 기본 안내
        )
        Result.failure(e)
    }
}
```

- **순서는 서버 code가 먼저다.** 같은 404라도 `api.meal.notFound`처럼 code가 있으면 비즈니스 에러이므로 화면에 맞게 안내하고, code가 없을 때만 공통 처리로 넘긴다. [BaseUseCase.handleHttpError](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/base/BaseUseCase.kt)가 이 순서를 고정한다.
- 공통 처리는 [BaseUseCase.executeCommonErrorHanding](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/base/BaseUseCase.kt): 401→로그인 만료 다이얼로그, 404→"준비 중인 기능이에요.", 그 외→"잠시 문제가 생겼어요" 다이얼로그. 상태 코드나 서버 메시지는 보여 주지 않는다.
- 로그인 전 화면(이메일 로그인, 소셜 로그인과 가입)처럼 공통 401 안내("로그인 만료")가 맞지 않는 곳은 code를 먼저 본 뒤 상태 코드로 직접 분기한다.
- 단순 위임 UseCase는 처리 없이 **그대로 전파**하고 ViewModel이 `runCatching { ... }.onFailure { dispatch(Failed); messageHelper.showSnackBar(...) }`로 UI 복구한다 — 어느 쪽이든 "처리 위치는 한 곳"이 원칙.

## {Feature}ErrorType 규칙

- 위치: **`<feature>/domain/`** (entity 아님)
- `enum class : HttpErrorType`, `type` = 서버가 내려주는 에러 식별 문자열 (예: `"api.intro.requiredForceUpdate"`)
- `type`은 백엔드 `exception/errorcode/*ErrorCode.kt`의 `code`와 철자까지 같아야 한다. 서버 코드가 바뀌면(예: `api.ses.emailSendFailed` → `api.email.sendFailed`) 배포가 끝날 때까지 이전 code도 함께 둔다.
- UseCase에서 처리하지 않을 항목은 `isHandledOnDomain = false`로 두고 presentation에서 분기

## 사용자 노출 (MessageHelper)

domain에서 직접 사용 가능한 인터페이스 ([MessageHelper.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/helper/MessageHelper.kt)): `showToast / showSnackBar(iconType, messageText|messageRes, CTA) / showOneButtonDialog(cantIgnore=true면 닫기 불가) / showTwoButtonDialog`. 구현은 presentation의 MessageHelperImpl — RootComposable의 SnackbarHost/다이얼로그가 `effect: Flow<MessageEffect>`를 구독한다.
