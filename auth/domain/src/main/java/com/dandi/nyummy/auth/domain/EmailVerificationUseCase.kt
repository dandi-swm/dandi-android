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
