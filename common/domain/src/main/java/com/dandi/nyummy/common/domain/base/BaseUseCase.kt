package com.dandi.nyummy.common.domain.base

import com.dandi.nyummy.common.domain.error.HttpErrorType
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.error.isSessionExpired
import com.dandi.nyummy.common.domain.error.registeredErrorType
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.tti.TTIHelper

open class BaseUseCase(
    protected open val resourceHelper: ResourceHelper,
    protected open val messageHelper: MessageHelper,
    protected open val navigationHelper: NavigationHelper,
    // 이 UseCase 를 주입받은 ViewModel 과 같은 측정 인스턴스(@ViewModelScoped)
    protected open val ttiHelper: TTIHelper,
) {

    /**
     * 서버 오류를 정해진 순서로 처리한다.
     * 1. 백엔드가 준 code가 [ErrorType]에 있으면 [onDomainError]로 화면에 맞는 안내를 한다.
     *    같은 404라도 "지운 기록"처럼 code가 있으면 여기서 먼저 처리된다.
     *    [HttpErrorType.isHandledOnDomain]이 false인 code는 presentation이 처리하므로 여기서는 아무 안내도 하지 않는다.
     * 2. 맞는 code가 없으면 공통 오류(로그인 만료 401, 404, 5xx)로 안내한다.
     *    다른 code가 붙은 401은 로그인 만료가 아니다([isSessionExpired]).
     * 3. 둘 다 아니면(등록하지 않은 code의 4xx 등) [onUnknownError]로 화면의 기본 안내를 한다.
     */
    protected inline fun <reified ErrorType> handleHttpError(
        e: HttpResponseException,
        onDomainError: (ErrorType) -> Unit,
        onUnknownError: () -> Unit = {},
    ) where ErrorType : Enum<ErrorType>, ErrorType : HttpErrorType {
        val errorType = e.registeredErrorType<ErrorType>()
        when {
            errorType?.isHandledOnDomain == true -> onDomainError(errorType)
            errorType != null -> Unit
            e.isCommonErrorHandling() -> executeCommonErrorHanding(e)
            else -> onUnknownError()
        }
    }

    /**
     * 여러 화면에 공통인 오류(로그인 만료 401, 404, 5xx)를 안내한다.
     * 서버의 에러 메시지와 HTTP 상태는 디버그용이라 사용자에게 보여 주지 않고, 정해 둔 문구만 쓴다.
     * 백엔드 code로 구분되는 오류는 [handleHttpError]가 이 함수보다 먼저 처리한다.
     */
    fun executeCommonErrorHanding(e: HttpResponseException) {
        when {
            // code가 붙은 다른 401(인증 코드, 소셜 토큰 오류 등)은 로그인 만료가 아니라 아래 일시 오류로 안내한다.
            e.isSessionExpired() -> {
                messageHelper.showOneButtonDialog(
                    cantIgnore = true,
                    descText = SESSION_EXPIRED_MESSAGE,
                    buttonText = SESSION_EXPIRED_BUTTON,
                    onClickButton = {
                        // 목적지(로그인 화면)를 직접 지정하지 않는다.
                        // common:domain 이 feature 의 Page 를 참조하면 순환 의존이 되므로
                        // "초기 화면으로 이동" 신호만 날리고 실제 이동은 NavHost 가 결정한다.
                        navigationHelper.navigateToInitial()
                    },
                )
            }

            e.rawCode == 404 -> {
                messageHelper.showOneButtonDialog(
                    cantIgnore = true,
                    descText = NOT_READY_MESSAGE,
                    buttonText = NOT_READY_BUTTON,
                    onClickButton = {
                        navigationHelper.navigateToBack()
                    }
                )
            }

            else -> {
                messageHelper.showOneButtonDialog(
                    titleText = TEMPORARY_ERROR_TITLE,
                    descText = TEMPORARY_ERROR_MESSAGE,
                )
            }
        }
    }

    private companion object {
        const val SESSION_EXPIRED_MESSAGE = "로그인이 만료됐어요. 다시 로그인해 주세요."
        const val SESSION_EXPIRED_BUTTON = "로그인하기"
        const val NOT_READY_MESSAGE = "준비 중인 기능이에요."
        const val NOT_READY_BUTTON = "돌아가기"
        const val TEMPORARY_ERROR_TITLE = "잠시 문제가 생겼어요"
        const val TEMPORARY_ERROR_MESSAGE = "잠시 후 다시 시도해 주세요."
    }
}
