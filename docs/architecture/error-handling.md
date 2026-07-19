# 에러 핸들링

## 왜

- HTTP 에러를 **data가 던지고 domain이 처리**하도록 고정하면, "어디서 다이얼로그를 띄울지"가 feature마다 흔들리지 않는다.
- 서버 에러 코드(`cause.message`)를 feature별 enum(`HttpErrorType`)으로 매핑해, 공통 처리(401/404/5xx)와 feature 고유 처리를 기계적으로 분리한다.

## 전파 체인

```
Retrofit Response ─▶ BaseRemoteDataSource.checkResponse()      [data]   실패 시 HttpResponseException throw
                 ─▶ RepositoryImpl (변환만, 처리 안 함)          [data]
                 ─▶ UseCase: try/catch                          [domain] ① 공통 처리 ② feature 처리 ③ 전파
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
        handleIntroError(e)
        Result.failure(e)
    }

    private fun handleIntroError(e: HttpResponseException) {
        if (e.isCommonErrorHandling()) { executeCommonErrorHanding(e); return }   // ① 공통(BaseUseCase)
        val errorType = e.handlingErrorOnUseCase<IntroErrorType>() ?: return      // ② feature 매핑
        when (errorType) {
            IntroErrorType.REQUIRED_FORCE_UPDATE -> messageHelper.showOneButtonDialog(
                cantIgnore = true, descText = "...", onClickButton = { ... })
        }
    }
}
```

- 공통 처리는 [BaseUseCase.executeCommonErrorHanding](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/base/BaseUseCase.kt): 401→세션 만료 다이얼로그, 404→기능 미지원, 그 외→임시 오류.
- 단순 위임 UseCase는 처리 없이 **그대로 전파**하고 ViewModel이 `runCatching { ... }.onFailure { dispatch(Failed); messageHelper.showSnackBar(...) }`로 UI 복구한다 — 어느 쪽이든 "처리 위치는 한 곳"이 원칙.

## {Feature}ErrorType 규칙

- 위치: **`<feature>/domain/`** (entity 아님)
- `enum class : HttpErrorType`, `type` = 서버가 내려주는 에러 식별 문자열 (예: `"api.intro.requiredForceUpdate"`)
- UseCase에서 처리하지 않을 항목은 `isHandledOnDomain = false`로 두고 presentation에서 분기

## 사용자 노출 (MessageHelper)

domain에서 직접 사용 가능한 인터페이스 ([MessageHelper.kt](../../common/domain/src/main/java/com/dandi/nyummy/common/domain/helper/MessageHelper.kt)): `showToast / showSnackBar(iconType, messageText|messageRes, CTA) / showOneButtonDialog(cantIgnore=true면 닫기 불가) / showTwoButtonDialog`. 구현은 presentation의 MessageHelperImpl — RootComposable의 SnackbarHost/다이얼로그가 `effect: Flow<MessageEffect>`를 구독한다.
