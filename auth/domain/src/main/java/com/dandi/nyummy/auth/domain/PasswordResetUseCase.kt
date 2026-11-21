package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.CommonErrorPolicy
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 비밀번호 찾기의 마지막 단계. 이메일 인증을 마친 토큰으로 비밀번호를 재설정하면
 * 서버가 임시 비밀번호로 바꾸고 가입한 이메일로 보낸다. 인증 코드 발송과 확인은 [EmailVerificationUseCase]가 맡는다.
 */
class PasswordResetUseCase @Inject constructor(
    private val repository: AuthRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    /**
     * 실패는 다이얼로그로 안내한다. 인증이 만료됐거나 무효하거나 가입되지 않은 이메일이면
     * 이메일 입력부터 다시 해야 하므로 [EmailVerificationRestartRequiredException]을 돌려준다.
     * 메일 발송 실패처럼 그 밖의 실패는 같은 인증 토큰으로 다시 시도할 수 있다.
     * 로그인 전 화면이라 code 없는 401도 로그인 만료로 보지 않는다.
     */
    suspend fun resetPassword(verifiedToken: String): Result<Unit> = request(
        AuthErrorType.entries,
        commonErrors = CommonErrorPolicy.SIGNED_OUT,
        onBusinessError = { type, cause ->
            // 공통 문구는 인증 메일 기준이라, 임시 비밀번호 메일을 못 보낸 경우는 따로 안내한다.
            val message = if (type == AuthErrorType.EMAIL_SEND_FAILED) StringResource.AUTH_TEMP_PASSWORD_MAIL_FAILED else type.errorMsg
            showError(resourceHelper.getString(message))
            Result.failure(if (type in RESTART_ERRORS) EmailVerificationRestartRequiredException() else cause)
        },
        onNetworkError = { showError(resourceHelper.getString(StringResource.NETWORK_ERROR)) },
        onUnknownError = { showError(resourceHelper.getString(StringResource.AUTH_PASSWORD_RESET_FAILED)) },
    ) {
        repository.resetPassword(verifiedToken)
    }

    private fun showError(message: String) {
        messageHelper.showOneButtonDialog(descText = message)
    }

    private companion object {
        /** 이메일 입력부터 다시 해야 하는 오류. 인증 만료, 무효한 인증, 가입되지 않은 이메일. */
        val RESTART_ERRORS = setOf(
            AuthErrorType.VERIFICATION_EXPIRED,
            AuthErrorType.INVALID_VERIFIED_TOKEN,
            AuthErrorType.EMAIL_NOT_FOUND,
        )
    }
}
