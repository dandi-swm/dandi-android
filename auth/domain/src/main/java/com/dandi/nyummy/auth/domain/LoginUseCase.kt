package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {


    /**
     * 이메일 로그인. 성공 시 홈으로 이동한다.
     *
     * 발급 토큰 저장은 data 레이어에서 담당한다.
     */
    suspend fun login(email: String, password: String): Result<Unit> = try {
        repository.login(email = email, password = password)
        navigationHelper.navigateTo(HomePage)
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        handleLoginError(e)
        Result.failure(e)
    }

    private fun handleLoginError(e: HttpResponseException) {
        if(e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
            return
        }
        val errorType = e.handlingErrorOnUseCase<AuthErrorType>() ?: return
        messageHelper.showOneButtonDialog(descText = errorType.errorMsg)
    }

}