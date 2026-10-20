package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.domain.AuthRepository
import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.EmailChallengeVO
import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import com.dandi.nyummy.auth.entity.EmailVerifiedVO
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginVO
import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// auth ViewModel 테스트들이 함께 쓰는 가짜 구현.

/** 서버 에러 바디의 `code` 를 cause 로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
internal fun httpException(code: Int, errorCode: String? = null): HttpResponseException =
    HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/api/v1/auth",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )

internal class FakeAuthRepository : AuthRepository {
    var socialLoginResult: SocialLoginVO = SocialLoginVO.empty
    val socialLoginCalls = mutableListOf<SocialCredentialVO>()
    var emailChallengeToken = ""
    var emailVerifiedToken = ""
    var confirmEmailVerificationError: Exception? = null
    var resetPasswordError: Exception? = null
    val requestEmailVerificationCalls = mutableListOf<Pair<String, EmailVerificationPurpose>>()
    val confirmEmailVerificationCalls = mutableListOf<String>()
    val resetPasswordCalls = mutableListOf<String>()

    override suspend fun setOnboardingIncomplete(incomplete: Boolean) = Unit

    override suspend fun socialLogin(credential: SocialCredentialVO): SocialLoginVO {
        socialLoginCalls += credential
        return socialLoginResult
    }

    override suspend fun login(email: String, password: String) = AuthTokenVO.empty
    override suspend fun signUp(
        verifiedToken: String,
        password: String?,
        confirmPassword: String?,
        nickname: String,
        gender: Gender?,
        birth: String?,
        height: Int?,
        weight: Int?,
    ) = Unit

    override suspend fun requestEmailVerification(email: String, purpose: EmailVerificationPurpose): EmailChallengeVO {
        requestEmailVerificationCalls += email to purpose
        return EmailChallengeVO(emailChallengeToken)
    }

    override suspend fun confirmEmailVerification(authCode: String, emailChallengeToken: String): EmailVerifiedVO {
        confirmEmailVerificationCalls += authCode
        confirmEmailVerificationError?.let { throw it }
        return EmailVerifiedVO(emailVerifiedToken)
    }

    override suspend fun resetPassword(verifiedToken: String) {
        resetPasswordCalls += verifiedToken
        resetPasswordError?.let { throw it }
    }

    override suspend fun logout() = Unit
}

internal class RecordingNavigationHelper : NavigationHelper {
    val pages = mutableListOf<Page>()
    var backCount = 0

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) {
        pages += page
    }

    override fun navigateDeepLink(route: NavRoute) = Unit
    override fun navigateToBack() {
        backCount++
    }
    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() = Unit
    override fun navigateToExternalLink(url: String) = Unit
}

internal class RecordingMessageHelper : MessageHelper {
    val dialogs = mutableListOf<String>()
    val snackBars = mutableListOf<Pair<IconType, String>>()

    val toasts = mutableListOf<String>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) {
        toasts += toastMsg
    }
    override fun showToast(messageRes: Int) = Unit
    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
        snackBars += iconType to messageText
    }

    override fun showSnackBar(
        iconType: IconType,
        messageRes: Int,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) = Unit

    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) {
        dialogs += descText
    }

    override fun showTwoButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        leftButtonText: String,
        onClickLeftButton: (() -> Unit)?,
        rightButtonText: String,
        onClickRightButton: (() -> Unit)?,
    ) {
        lastTwoButtonConfirm = onClickRightButton
    }

    /** 마지막 두 버튼 다이얼로그의 오른쪽(확인) 버튼. 테스트가 직접 눌러 본다. */
    var lastTwoButtonConfirm: (() -> Unit)? = null
}

internal object FakeResourceHelper : ResourceHelper {
    override fun getString(resource: StringResource): String = ""
}

internal object FakeTTIHelper : TTIHelper {
    override fun startTTITracking(page: TTIPage) = Unit
    override fun startTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITracking() = Unit
    override fun shotTTILogging() = Unit
    override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
}

/** 남긴 분석 기록을 모아 둔다. */
internal class RecordingAnalyticsHelper : AnalyticsHelper {
    val screens = mutableListOf<String>()
    val events = mutableListOf<AnalyticsEvent>()

    override fun logScreenView(screenName: String) {
        screens += screenName
    }

    override fun logEvent(event: AnalyticsEvent) {
        events += event
    }
}
