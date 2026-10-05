package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.EmailChallengeVO
import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import com.dandi.nyummy.auth.entity.EmailVerifiedVO
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginVO
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

/** 서버 에러 바디의 `code` 를 cause 로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
internal fun httpException(code: Int, errorCode: String? = null): HttpResponseException =
    HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/api/v1/auth",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )

internal class FakeAuthRepository(
    var loginResult: AuthTokenVO = AuthTokenVO.empty,
    var loginError: Exception? = null,
    var socialLoginResult: SocialLoginVO = SocialLoginVO.empty,
    var socialLoginError: Exception? = null,
    var signUpError: Exception? = null,
) : AuthRepository {

    data class SignUpCall(
        val verifiedToken: String,
        val password: String?,
        val confirmPassword: String?,
        val nickname: String,
    )

    val socialLoginCalls = mutableListOf<SocialCredentialVO>()
    val signUpCalls = mutableListOf<SignUpCall>()
    var onboardingIncomplete = false
        private set

    override suspend fun setOnboardingIncomplete(incomplete: Boolean) {
        onboardingIncomplete = incomplete
    }

    override suspend fun socialLogin(credential: SocialCredentialVO): SocialLoginVO {
        socialLoginCalls += credential
        socialLoginError?.let { throw it }
        return socialLoginResult
    }

    override suspend fun login(email: String, password: String): AuthTokenVO {
        loginError?.let { throw it }
        return loginResult
    }

    override suspend fun signUp(
        verifiedToken: String,
        password: String?,
        confirmPassword: String?,
        nickname: String,
        gender: Gender?,
        birth: String?,
        height: Int?,
        weight: Int?,
    ) {
        signUpCalls += SignUpCall(verifiedToken, password, confirmPassword, nickname)
        signUpError?.let { throw it }
    }

    override suspend fun requestEmailVerification(
        email: String,
        purpose: EmailVerificationPurpose,
    ) = EmailChallengeVO()

    override suspend fun confirmEmailVerification(
        authCode: String,
        emailChallengeToken: String,
    ) = EmailVerifiedVO()
}

internal class RecordingNavigationHelper : NavigationHelper {
    val pages = mutableListOf<Page>()
    val rootPages = mutableListOf<Page>()
    var backCount = 0
    var initialCount = 0

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) {
        pages += page
    }

    override fun navigateDeepLink(route: NavRoute) = Unit
    override fun navigateToBack() {
        backCount++
    }

    override fun navigateToAsRoot(page: Page) {
        rootPages += page
    }

    override fun navigateToInitial() {
        initialCount++
    }

    override fun navigateToExternalLink(url: String) = Unit
}

internal class RecordingMessageHelper : MessageHelper {
    data class DialogCall(
        val descText: String,
        val cantIgnore: Boolean,
        val onClickButton: (() -> Unit)?,
    )

    data class SnackBarCall(val iconType: IconType, val messageText: String)

    val dialogs = mutableListOf<DialogCall>()
    val snackBars = mutableListOf<SnackBarCall>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
        snackBars += SnackBarCall(iconType, messageText)
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
        dialogs += DialogCall(descText, cantIgnore, onClickButton)
    }

    override fun showTwoButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        leftButtonText: String,
        onClickLeftButton: (() -> Unit)?,
        rightButtonText: String,
        onClickRightButton: (() -> Unit)?,
    ) = Unit
}

internal class FakeResourceHelper : ResourceHelper {
    override fun getString(resource: StringResource): String = ""
}

internal class FakeTTIHelper : TTIHelper {
    override fun startTTITracking(page: TTIPage) = Unit
    override fun startTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
    override fun endTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
    override fun endTTITracking(page: TTIPage) = Unit
    override fun shotTTILogging(page: TTIPage) = Unit
    override fun addTTIMetaData(page: TTIPage, metadata: TTIMetaData, value: Any?) = Unit
}
