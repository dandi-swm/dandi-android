package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.CommonErrorPolicy
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

class EmailVerificationUseCase @Inject constructor(
    private val repository: AuthRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    /**
     * 이메일 인증 코드 발송. 성공 시 코드 확인에 쓸 챌린지 토큰을 반환한다.
     *
     * 메일 발송 실패(500)처럼 서버가 code를 주는 오류는 공통 오류보다 먼저 code에 맞는 문구로 안내해,
     * 사용자가 메일이 가지 않았다는 걸 알 수 있게 한다. 로그인 전 화면이라 code 없는 401도 로그인 만료로 보지 않는다.
     */
    suspend fun sendCode(email: String, purpose: EmailVerificationPurpose): Result<String> = request(
        AuthErrorType.entries,
        commonErrors = CommonErrorPolicy.SIGNED_OUT,
        onBusinessError = { type, cause ->
            showError(resourceHelper.getString(type.errorMsg))
            Result.failure(cause)
        },
        onNetworkError = { showError(resourceHelper.getString(StringResource.NETWORK_ERROR)) },
        // 등록하지 않은 응답 오류와 응답을 읽지 못한 경우(본문 없는 200 등).
        onUnknownError = { showError(resourceHelper.getString(StringResource.AUTH_EMAIL_SEND_FAILED)) },
    ) {
        repository.requestEmailVerification(email = email, purpose = purpose).emailChallengeToken
    }

    /**
     * 이메일 인증 코드 확인. 성공 시 회원가입에 쓸 인증 완료 토큰을 반환한다.
     *
     * 코드가 틀렸거나 만료된 것처럼 서버가 code를 주는 오류는 다이얼로그 대신 코드 입력란 아래
     * 인라인으로 보여줘야 하므로 [CodeVerificationFailedException]으로 반환한다. 네트워크 끊김과 그 밖의 실패도 같다.
     * 공통 안내(404, 5xx)를 한 실패만 원래 예외로 돌려준다.
     */
    suspend fun confirmCode(authCode: String, emailChallengeToken: String): Result<String> {
        var inlineMessage: StringResource? = null
        val result = request(
            AuthErrorType.entries,
            commonErrors = CommonErrorPolicy.SIGNED_OUT,
            onBusinessError = { type, _ ->
                Result.failure(CodeVerificationFailedException(resourceHelper.getString(type.errorMsg)))
            },
            onNetworkError = { inlineMessage = StringResource.NETWORK_ERROR },
            onUnknownError = { inlineMessage = StringResource.AUTH_CODE_CONFIRM_FAILED },
        ) {
            repository.confirmEmailVerification(authCode = authCode, emailChallengeToken = emailChallengeToken).emailVerifiedToken
        }
        return inlineMessage?.let { Result.failure(CodeVerificationFailedException(resourceHelper.getString(it))) } ?: result
    }

    private fun showError(message: String) {
        messageHelper.showOneButtonDialog(descText = message)
    }
}

/** 인증 코드 확인 실패 — 코드 입력란에 인라인으로 표시할 메시지를 담는다. */
class CodeVerificationFailedException(override val message: String) : Exception(message)
