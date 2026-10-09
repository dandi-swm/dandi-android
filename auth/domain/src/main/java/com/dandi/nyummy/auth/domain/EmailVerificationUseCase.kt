package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.tti.TTIHelper
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class EmailVerificationUseCase @Inject constructor(
    private val repository: AuthRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /**
     * 이메일 인증 코드 발송. 성공 시 코드 확인에 쓸 챌린지 토큰을 반환한다.
     *
     * 메일 발송 실패(500)처럼 서버가 code를 주는 오류는 공통 오류보다 먼저 code에 맞는 문구로 안내해,
     * 사용자가 메일이 가지 않았다는 걸 알 수 있게 한다.
     */
    suspend fun sendCode(email: String, purpose: EmailVerificationPurpose): Result<String> = try {
        val challenge = repository.requestEmailVerification(email = email, purpose = purpose)
        Result.success(challenge.emailChallengeToken)
    } catch (e: HttpResponseException) {
        handleHttpError<AuthErrorType>(
            e,
            onDomainError = { showError(it.errorMsg) },
            onUnknownError = { showError(SEND_FAILED_MESSAGE) },
        )
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        showError(NETWORK_ERROR_MESSAGE)
        Result.failure(e)
    }

    /**
     * 이메일 인증 코드 확인. 성공 시 회원가입에 쓸 인증 완료 토큰을 반환한다.
     *
     * 코드가 틀렸거나 만료된 것처럼 서버가 code를 주는 오류는 다이얼로그 대신 코드 입력란 아래
     * 인라인으로 보여줘야 하므로 [CodeVerificationFailedException]으로 반환한다.
     */
    suspend fun confirmCode(authCode: String, emailChallengeToken: String): Result<String> = try {
        val verified = repository.confirmEmailVerification(
            authCode = authCode,
            emailChallengeToken = emailChallengeToken,
        )
        Result.success(verified.emailVerifiedToken)
    } catch (e: HttpResponseException) {
        var inlineMessage: String? = null
        handleHttpError<AuthErrorType>(
            e,
            onDomainError = { inlineMessage = it.errorMsg },
            onUnknownError = { inlineMessage = AuthErrorType.UNKNOWN.errorMsg },
        )
        Result.failure(inlineMessage?.let(::CodeVerificationFailedException) ?: e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(CodeVerificationFailedException(NETWORK_ERROR_MESSAGE))
    }

    private fun showError(message: String) {
        messageHelper.showOneButtonDialog(descText = message)
    }

    private companion object {
        const val SEND_FAILED_MESSAGE = "인증 메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요."
        const val NETWORK_ERROR_MESSAGE = "네트워크 연결을 확인한 뒤 다시 시도해주세요."
    }
}

/** 인증 코드 확인 실패 — 코드 입력란에 인라인으로 표시할 메시지를 담는다. */
class CodeVerificationFailedException(override val message: String) : Exception(message)
