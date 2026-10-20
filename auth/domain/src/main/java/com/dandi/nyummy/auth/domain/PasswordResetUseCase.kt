package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.tti.TTIHelper
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /**
     * 실패는 다이얼로그로 안내한다. 인증이 만료됐거나 무효하거나 가입되지 않은 이메일이면
     * 이메일 입력부터 다시 해야 하므로 [EmailVerificationRestartRequiredException]을 돌려준다.
     * 메일 발송 실패처럼 그 밖의 실패는 같은 인증 토큰으로 다시 시도할 수 있다.
     */
    suspend fun resetPassword(verifiedToken: String): Result<Unit> = try {
        repository.resetPassword(verifiedToken)
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        Result.failure(handleResetError(e))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        showError(NETWORK_ERROR_MESSAGE)
        Result.failure(e)
    } catch (e: Exception) {
        showError(RESET_FAILED_MESSAGE)
        Result.failure(e)
    }

    private fun handleResetError(e: HttpResponseException): Exception {
        var failure: Exception = e
        handleHttpError<AuthErrorType>(
            e,
            onDomainError = { errorType ->
                // 공통 문구는 인증 메일 기준이라, 임시 비밀번호 메일을 못 보낸 경우는 따로 안내한다.
                showError(if (errorType == AuthErrorType.EMAIL_SEND_FAILED) MAIL_SEND_FAILED_MESSAGE else errorType.errorMsg)
                if (errorType in RESTART_ERRORS) failure = EmailVerificationRestartRequiredException()
            },
            onUnknownError = { showError(RESET_FAILED_MESSAGE) },
        )
        return failure
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

        const val MAIL_SEND_FAILED_MESSAGE = "임시 비밀번호 메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요."
        const val RESET_FAILED_MESSAGE = "비밀번호를 재설정하지 못했어요. 잠시 후 다시 시도해 주세요."
        const val NETWORK_ERROR_MESSAGE = "네트워크 연결을 확인한 뒤 다시 시도해주세요."
    }
}
