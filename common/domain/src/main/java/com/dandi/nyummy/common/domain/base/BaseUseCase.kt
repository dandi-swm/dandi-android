package com.dandi.nyummy.common.domain.base

import com.dandi.nyummy.common.domain.error.CommonErrorPolicy
import com.dandi.nyummy.common.domain.error.DomainFailure
import com.dandi.nyummy.common.domain.error.HttpErrorType
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isSessionExpired
import com.dandi.nyummy.common.domain.error.toDomainFailure
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.tti.TTIHelper
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

open class BaseUseCase(
    protected open val resourceHelper: ResourceHelper,
    protected open val messageHelper: MessageHelper,
    protected open val navigationHelper: NavigationHelper,
    // 이 UseCase 를 주입받은 ViewModel 과 같은 측정 인스턴스(@ViewModelScoped)
    protected open val ttiHelper: TTIHelper,
    /**
     * 로그인 만료를 앱 전체에서 한 번만 정리하고 안내한다. 앱에서는 Hilt가 하위 UseCase 생성자로 늘 넣는다.
     * null은 이것을 넘기지 않는 단위 테스트용이고, 그때는 이 UseCase가 다이얼로그와 이동만 한다(정리, 중복 막기 없음).
     */
    protected open val sessionExpiryHandler: SessionExpiryHandler? = null,
) {

    /**
     * 서버 요청 하나를 실행하고, 실패하면 정해진 순서로 안내한 뒤 [Result]로 돌려준다. UseCase의 try/catch는 이것으로 쓴다.
     *
     * 순서([toDomainFailure]):
     * 1. 백엔드가 준 code가 [errorTypes]에 있으면 [onBusinessError]가 결과를 고른다(성공으로 보기, 화면용 예외로 바꾸기 등).
     *    같은 404라도 "지운 기록"처럼 code가 있으면 공통 안내보다 먼저 여기로 온다.
     *    [HttpErrorType.isHandledOnDomain]이 false인 code는 화면이 처리하므로 안내 없이 실패만 돌려준다.
     * 2. [commonErrors]가 맡는 공통 오류(로그인 만료 401, 404, 5xx)는 [executeCommonErrorHanding]이 안내한다.
     * 3. 응답 전에 끊기면 [onNetworkError](없으면 [onUnknownError]), 그 밖의 응답 오류와 예상하지 못한 예외는 [onUnknownError].
     * 코루틴 취소는 실패가 아니라서 그대로 다시 던진다. 1번 밖의 실패는 모두 원래 예외로 [Result.failure]를 돌려준다.
     *
     * @param errorTypes 이 기능이 아는 서버 code. 보통 `HistoryErrorType.entries`.
     */
    protected suspend fun <E : HttpErrorType, T> request(
        errorTypes: List<E>,
        commonErrors: CommonErrorPolicy = CommonErrorPolicy.ALL,
        onBusinessError: suspend (type: E, cause: HttpResponseException) -> Result<T> = { _, cause -> Result.failure(cause) },
        onNetworkError: ((cause: IOException) -> Unit)? = null,
        onUnknownError: (cause: Exception) -> Unit = {},
        block: suspend () -> T,
    ): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        when (val failure = e.toDomainFailure(errorTypes, commonErrors)) {
            is DomainFailure.Business ->
                if (failure.type.isHandledOnDomain) onBusinessError(failure.type, failure.cause) else Result.failure(e)
            else -> {
                notifyFailure(failure, onNetworkError = onNetworkError, onUnknownError = onUnknownError)
                Result.failure(e)
            }
        }
    }

    /** 등록할 서버 code가 없는 요청. 나머지는 위 [request]와 같다. */
    protected suspend fun <T> request(
        commonErrors: CommonErrorPolicy = CommonErrorPolicy.ALL,
        onNetworkError: ((cause: IOException) -> Unit)? = null,
        onUnknownError: (cause: Exception) -> Unit = {},
        block: suspend () -> T,
    ): Result<T> = request(
        errorTypes = emptyList<HttpErrorType>(),
        commonErrors = commonErrors,
        onNetworkError = onNetworkError,
        onUnknownError = onUnknownError,
        block = block,
    )

    /**
     * 이미 나눈 실패([toDomainFailure])를 [request]와 같은 순서로 안내만 한다.
     * [Result]가 아닌 값을 돌려주는 UseCase(구매, 코인 받기, 분석 대기)가 쓴다.
     */
    protected fun <E : HttpErrorType> notifyFailure(
        failure: DomainFailure<E>,
        onBusinessError: (type: E) -> Unit = {},
        onNetworkError: ((cause: IOException) -> Unit)? = null,
        onUnknownError: (cause: Exception) -> Unit = {},
    ) {
        when (failure) {
            is DomainFailure.Business -> if (failure.type.isHandledOnDomain) onBusinessError(failure.type)
            is DomainFailure.Common -> executeCommonErrorHanding(failure.cause)
            is DomainFailure.Network -> if (onNetworkError != null) onNetworkError(failure.cause) else onUnknownError(failure.cause)
            is DomainFailure.UnknownHttp -> onUnknownError(failure.cause)
            is DomainFailure.Unexpected -> onUnknownError(failure.cause)
        }
    }

    /**
     * 응답 오류([HttpResponseException]) 하나만 [request]와 같은 순서로 안내한다. 새 코드는 [request]를 쓴다.
     * 1. 백엔드가 준 code가 [ErrorType]에 있으면 [onDomainError]. [HttpErrorType.isHandledOnDomain]이 false면 아무 안내도 하지 않는다.
     * 2. 맞는 code가 없으면 공통 오류(로그인 만료 401, 404, 5xx). 다른 code가 붙은 401은 로그인 만료가 아니다([isSessionExpired]).
     * 3. 둘 다 아니면(등록하지 않은 code의 4xx 등) [onUnknownError].
     */
    protected inline fun <reified ErrorType> handleHttpError(
        e: HttpResponseException,
        onDomainError: (ErrorType) -> Unit,
        onUnknownError: () -> Unit = {},
    ) where ErrorType : Enum<ErrorType>, ErrorType : HttpErrorType {
        when (val failure = e.toDomainFailure(enumValues<ErrorType>().toList())) {
            is DomainFailure.Business -> if (failure.type.isHandledOnDomain) onDomainError(failure.type)
            is DomainFailure.Common -> executeCommonErrorHanding(e)
            else -> onUnknownError()
        }
    }

    /**
     * 여러 화면에 공통인 오류(로그인 만료 401, 404, 5xx)를 안내한다.
     * 서버의 에러 메시지와 HTTP 상태는 디버그용이라 사용자에게 보여 주지 않고, 정해 둔 문구만 쓴다.
     * 백엔드 code로 구분되는 오류는 [request]가 이 함수보다 먼저 처리한다.
     *
     * 로그인 만료는 [SessionExpiryHandler]가 앱 전체에서 한 번만 세션을 정리하고 안내한다.
     * 로그인 만료를 안내한 뒤 다시 로그인하기 전까지는 다른 공통 다이얼로그로 그 안내를 덮지 않는다.
     */
    fun executeCommonErrorHanding(e: HttpResponseException) {
        when {
            // code가 붙은 다른 401(인증 코드, 소셜 토큰 오류 등)은 로그인 만료가 아니라 아래 일시 오류로 안내한다.
            e.isSessionExpired() -> sessionExpiryHandler?.onSessionExpired() ?: showSessionExpiredDialog()

            sessionExpiryHandler?.isExpired == true -> Unit

            e.rawCode == 404 -> {
                messageHelper.showOneButtonDialog(
                    cantIgnore = true,
                    descText = resourceHelper.getString(StringResource.NOT_READY_MESSAGE),
                    buttonText = resourceHelper.getString(StringResource.GO_BACK),
                    onClickButton = {
                        navigationHelper.navigateToBack()
                    }
                )
            }

            else -> {
                messageHelper.showOneButtonDialog(
                    titleText = resourceHelper.getString(StringResource.TEMPORARY_ERROR_TITLE),
                    descText = resourceHelper.getString(StringResource.TRY_AGAIN_LATER),
                )
            }
        }
    }

    /** [sessionExpiryHandler] 없이 만든 UseCase(단위 테스트)의 로그인 만료 안내. */
    private fun showSessionExpiredDialog() {
        messageHelper.showOneButtonDialog(
            cantIgnore = true,
            descText = resourceHelper.getString(StringResource.SESSION_EXPIRED_MESSAGE),
            buttonText = resourceHelper.getString(StringResource.SIGN_IN_AGAIN),
            onClickButton = { navigationHelper.navigateToInitial() },
        )
    }
}
