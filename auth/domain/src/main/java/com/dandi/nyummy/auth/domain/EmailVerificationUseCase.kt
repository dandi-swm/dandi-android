package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

class EmailVerificationUseCase @Inject constructor(
    private val repository: AuthRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /** 이메일 인증 코드 발송 */
    suspend fun sendCode(email: String): Result<Unit> = try {
        repository.requestEmailVerification(email = email)
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        handleEmailVerificationError(e)
        Result.failure(e)
    }

    /**
     * 이메일 인증 코드 확인.
     *
     * 도메인 에러(코드 불일치/만료 등)는 다이얼로그 대신 코드 입력란 아래
     * 인라인으로 보여줘야 하므로 [CodeVerificationFailedException]으로 반환한다.
     */
    suspend fun confirmCode(email: String, verificationCode: String): Result<Unit> = try {
        repository.confirmEmailVerification(email = email, verificationCode = verificationCode)
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        val errorType = e.handlingErrorOnUseCase<AuthErrorType>()
        when {
            errorType == AuthErrorType.MAIL_CODE_MISMATCH -> Result.failure(
                CodeVerificationFailedException(errorType.errorMsg)
            )
            e.isCommonErrorHandling() -> {
                executeCommonErrorHanding(e)
                Result.failure(e)
            }

            else -> Result.failure(CodeVerificationFailedException(AuthErrorType.UNKNOWN.errorMsg))
        }
    }

    private fun handleEmailVerificationError(e: HttpResponseException) {
        val errorType = e.handlingErrorOnUseCase<AuthErrorType>()
        if (errorType != null) {
            messageHelper.showOneButtonDialog(descText = errorType.errorMsg)
            return
        }
        if (e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
        }
    }
}

/** 인증 코드 확인 실패 — 코드 입력란에 인라인으로 표시할 메시지를 담는다. */
class CodeVerificationFailedException(override val message: String) : Exception(message)
