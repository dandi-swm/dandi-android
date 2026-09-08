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

    /** 이메일 인증 코드 발송. 성공 시 코드 확인에 쓸 챌린지 토큰을 반환한다. */
    suspend fun sendCode(email: String): Result<String> = try {
        val challenge = repository.requestEmailVerification(email = email)
        Result.success(challenge.emailChallengeToken)
    } catch (e: HttpResponseException) {
        handleEmailVerificationError(e)
        Result.failure(e)
    }

    /**
     * 이메일 인증 코드 확인. 성공 시 회원가입에 쓸 인증 완료 토큰을 반환한다.
     *
     * 도메인 에러(코드 불일치/만료 등)는 다이얼로그 대신 코드 입력란 아래
     * 인라인으로 보여줘야 하므로 [CodeVerificationFailedException]으로 반환한다.
     */
    suspend fun confirmCode(authCode: String, emailChallengeToken: String): Result<String> = try {
        val verified = repository.confirmEmailVerification(
            authCode = authCode,
            emailChallengeToken = emailChallengeToken,
        )
        Result.success(verified.emailVerifiedToken)
    } catch (e: HttpResponseException) {
        val errorType = e.handlingErrorOnUseCase<AuthErrorType>()
        when {
            errorType in INLINE_CODE_ERRORS -> Result.failure(
                CodeVerificationFailedException(requireNotNull(errorType).errorMsg)
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

    companion object {
        /** 코드 입력란 아래 인라인으로 표시하는 에러 — 재입력/재발송으로 사용자가 복구 가능한 경우 */
        private val INLINE_CODE_ERRORS = setOf(
            AuthErrorType.MAIL_CODE_MISMATCH,
            AuthErrorType.MAIL_CODE_EXPIRED,
            AuthErrorType.MAIL_NOT_FOUND,
        )
    }
}

/** 인증 코드 확인 실패 — 코드 입력란에 인라인으로 표시할 메시지를 담는다. */
class CodeVerificationFailedException(override val message: String) : Exception(message)
