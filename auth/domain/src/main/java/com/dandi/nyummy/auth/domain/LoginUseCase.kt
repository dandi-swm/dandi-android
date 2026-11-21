package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.CommonErrorPolicy
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.WidgetHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val analyticsHelper: AnalyticsHelper,
    private val widgetHelper: WidgetHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    /**
     * 이메일 로그인. 성공 시 응답의 redirectUrl 이 가리키는 화면(온보딩 미완료면 온보딩, 그 외 홈)으로 이동한다.
     *
     * 발급 토큰 저장은 data 레이어에서 담당한다. 기기 홈 화면 위젯에 남은 앞 계정의 기록은 지운다.
     * 로그인 전 화면이라 code 없는 401도 로그인 만료가 아니라 로그인 실패로 안내한다([CommonErrorPolicy.SIGNED_OUT]).
     */
    suspend fun login(email: String, password: String): Result<Unit> = request(
        AuthErrorType.entries,
        commonErrors = CommonErrorPolicy.SIGNED_OUT,
        onBusinessError = { type, cause ->
            showError(resourceHelper.getString(type.errorMsg))
            Result.failure(cause)
        },
        onNetworkError = { showError(resourceHelper.getString(StringResource.NETWORK_ERROR)) },
        // 등록하지 않은 응답 오류와 응답을 읽지 못한 경우(본문 없는 200 등).
        onUnknownError = { showError(resourceHelper.getString(StringResource.AUTH_LOGIN_FAILED)) },
    ) {
        val token = repository.login(email = email, password = password)
        repository.setOnboardingIncomplete(token.redirectUrl.isOnboardingRedirect())
        widgetHelper.clear()
        sessionExpiryHandler?.onSignedIn()
        analyticsHelper.logEvent(AnalyticsEvent.Login(method = "email"))
        navigationHelper.openAfterLogin(token.redirectUrl)
    }

    private fun showError(message: String) {
        messageHelper.showOneButtonDialog(descText = message)
    }
}