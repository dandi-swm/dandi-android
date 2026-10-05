package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.onboarding.domain.OnboardingPage
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
     * 이메일 로그인. 성공 시 응답의 redirectUrl 이 가리키는 화면(온보딩 미완료면 온보딩, 그 외 홈)으로 이동한다.
     *
     * 발급 토큰 저장은 data 레이어에서 담당한다.
     */
    suspend fun login(email: String, password: String): Result<Unit> = try {
        val token = repository.login(email = email, password = password)
        val destination = PostLoginDestination.from(token.redirectUrl)
        repository.setOnboardingIncomplete(destination == OnboardingPage)
        navigationHelper.navigateToAsRoot(destination)
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        handleLoginError(e)
        Result.failure(e)
    }

    private fun handleLoginError(e: HttpResponseException) {

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