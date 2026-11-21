package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.CommonErrorPolicy
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.WidgetHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.onboarding.domain.OnboardingPage
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val socialSignUpSession: SocialSignUpSession,
    private val analyticsHelper: AnalyticsHelper,
    private val widgetHelper: WidgetHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    /**
     * 이메일 회원가입. 성공 시 로그인 상태가 되어 고양이 이름을 짓는 온보딩으로 이동한다.
     *
     * 발급 토큰 저장은 data 레이어에서 담당한다. 가입이 되면 기기 홈 화면 위젯에 남은 앞 계정의 기록을 지운다.
     *
     * @param emailVerifiedToken 이메일 인증 완료 토큰
     * @param birth 생년월일 (`yyyy-MM-dd` 형식, 선택)
     * @param height 키 (cm, 선택)
     * @param weight 몸무게 (kg, 선택)
     */
    suspend fun signUp(
        emailVerifiedToken: String,
        password: String,
        confirmPassword: String,
        nickname: String,
        gender: Gender? = null,
        birth: String? = null,
        height: Int? = null,
        weight: Int? = null,
    ): Result<Unit> = request(
        AuthErrorType.entries,
        commonErrors = CommonErrorPolicy.SIGNED_OUT,
        onBusinessError = { type, cause ->
            showError(resourceHelper.getString(type.errorMsg))
            // 인증 완료 토큰이 만료됐거나 무효하면 이메일 인증을 처음부터 다시 해야 한다.
            Result.failure(if (type in VERIFICATION_RESTART_ERRORS) EmailVerificationRestartRequiredException() else cause)
        },
        onNetworkError = { showError(resourceHelper.getString(StringResource.NETWORK_ERROR)) },
        onUnknownError = { cause ->
            val message = when {
                cause !is HttpResponseException -> StringResource.TEMPORARY_ERROR
                cause.rawCode == HTTP_CONFLICT -> StringResource.AUTH_EMAIL_ALREADY_EXISTS
                else -> StringResource.AUTH_SIGN_UP_FAILED
            }
            showError(resourceHelper.getString(message))
        },
    ) {
        repository.signUp(
            verifiedToken = emailVerifiedToken,
            password = password,
            confirmPassword = confirmPassword,
            nickname = nickname,
            gender = gender,
            birth = birth,
            height = height,
            weight = weight,
        )
        repository.setOnboardingIncomplete(incomplete = true)
        widgetHelper.clear()
        sessionExpiryHandler?.onSignedIn()
        analyticsHelper.logEvent(AnalyticsEvent.SignUp(method = "email"))
        navigationHelper.navigateToAsRoot(OnboardingPage)
    }

    /**
     * 소셜 로그인 신규 회원의 회원가입. [SocialSignUpSession] 에 보관된 검증 완료 토큰을 쓰며,
     * 비밀번호는 보내지 않는다. 성공 시 온보딩으로 이동한다.
     */
    suspend fun signUpWithSocial(
        nickname: String,
        gender: Gender? = null,
        birth: String? = null,
        height: Int? = null,
        weight: Int? = null,
    ): Result<Unit> {
        val verifiedToken = socialSignUpSession.pendingToken
        if (verifiedToken.isNullOrBlank()) {
            leaveSocialSignUpWithoutSession()
            return Result.failure(IllegalStateException("No pending social sign-up"))
        }
        // 가입 실패의 서버 code 처리는 아래 [handleSocialSignUpBusinessError], code 없는 실패는 [handleSocialSignUpUnknownError].
        // 로그인 전이라 code 없는 401은 로그인 만료가 아니다. 가입 대기 토큰을 비우고 다시 로그인하게 한다.
        return request(
            AuthErrorType.entries,
            commonErrors = CommonErrorPolicy.SIGNED_OUT,
            onBusinessError = { type, cause ->
                handleSocialSignUpBusinessError(type)
                Result.failure(cause)
            },
            onNetworkError = { showError(resourceHelper.getString(StringResource.NETWORK_ERROR)) },
            onUnknownError = ::handleSocialSignUpUnknownError,
        ) {
            repository.signUp(
                verifiedToken = verifiedToken,
                password = null,
                confirmPassword = null,
                nickname = nickname,
                gender = gender,
                birth = birth,
                height = height,
                weight = weight,
            )
            repository.setOnboardingIncomplete(incomplete = true)
            socialSignUpSession.clear()
            widgetHelper.clear()
            sessionExpiryHandler?.onSignedIn()
            analyticsHelper.logEvent(AnalyticsEvent.SignUp(method = "social"))
            navigationHelper.navigateToAsRoot(OnboardingPage)
        }
    }

    /** 소셜 가입을 진행할 토큰이 있는지. 프로세스 종료 뒤 복원되거나 외부 링크로 들어오면 없다. */
    fun hasPendingSocialSignUp(): Boolean = !socialSignUpSession.pendingToken.isNullOrBlank()

    /** 가입 대기 토큰 없이 소셜 가입 화면에 들어왔을 때 안내하고 이전 화면으로 돌려보낸다. */
    fun leaveSocialSignUpWithoutSession() {
        messageHelper.showOneButtonDialog(
            descText = resourceHelper.getString(StringResource.AUTH_SOCIAL_SESSION_MISSING),
            cantIgnore = true,
            onClickButton = { navigationHelper.navigateToBack() },
        )
    }

    /** 소셜 가입 화면을 떠날 때 가입 대기 토큰을 버린다. */
    fun abandonSocialSignUp() {
        socialSignUpSession.clear()
    }

    /**
     * 소셜 가입의 서버 code 실패. 인증이 만료됐거나 이미 가입된 계정이면 다시 로그인해야 하므로
     * 토큰을 버리고 초기(로그인) 화면으로 보낸다. 그 외에는 토큰을 남겨 다시 제출할 수 있게 한다.
     */
    private fun handleSocialSignUpBusinessError(type: AuthErrorType) {
        when (type) {
            in VERIFICATION_RESTART_ERRORS -> restartSocialLogin(resourceHelper.getString(StringResource.AUTH_SOCIAL_SIGN_UP_EXPIRED))
            AuthErrorType.OAUTH_ACCOUNT_ALREADY_EXISTS, AuthErrorType.EMAIL_ALREADY_EXISTS ->
                restartSocialLogin(resourceHelper.getString(StringResource.AUTH_ACCOUNT_ALREADY_EXISTS))
            else -> showError(resourceHelper.getString(type.errorMsg))
        }
    }

    /** 소셜 가입의 code 없는 실패. 상태 코드로 구분한다. 404와 5xx는 공통 안내가 이미 했다. */
    private fun handleSocialSignUpUnknownError(cause: Exception) {
        when ((cause as? HttpResponseException)?.rawCode) {
            HTTP_UNAUTHORIZED -> restartSocialLogin(resourceHelper.getString(StringResource.AUTH_SOCIAL_SIGN_UP_EXPIRED))
            HTTP_CONFLICT -> restartSocialLogin(resourceHelper.getString(StringResource.AUTH_ACCOUNT_ALREADY_EXISTS))
            HTTP_BAD_REQUEST -> showError(resourceHelper.getString(StringResource.AUTH_INVALID_PROFILE))
            else -> showError(resourceHelper.getString(StringResource.TEMPORARY_ERROR))
        }
    }

    private fun restartSocialLogin(message: String) {
        socialSignUpSession.clear()
        messageHelper.showOneButtonDialog(
            descText = message,
            cantIgnore = true,
            onClickButton = { navigationHelper.navigateToInitial() },
        )
    }

    private fun showError(message: String) {
        messageHelper.showOneButtonDialog(descText = message)
    }

    private companion object {
        /** 인증 완료 토큰을 다시 받아야 하는 오류. 만료(`verificationExpired`)와 무효(`invalidVerifiedToken`). */
        val VERIFICATION_RESTART_ERRORS = setOf(AuthErrorType.VERIFICATION_EXPIRED, AuthErrorType.INVALID_VERIFIED_TOKEN)

        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_CONFLICT = 409
    }
}

/** 가입에 쓴 이메일 인증이 만료됐거나 무효하다. 안내는 UseCase가 이미 했고, 화면은 인증 단계로 돌아간다. */
class EmailVerificationRestartRequiredException : Exception("Email verification must be restarted")
