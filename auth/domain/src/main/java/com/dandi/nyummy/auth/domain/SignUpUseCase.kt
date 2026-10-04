package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.onboarding.domain.OnboardingPage
import com.dandi.nyummy.tti.TTIHelper
import java.io.IOException
import java.util.concurrent.CancellationException
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val socialSignUpSession: SocialSignUpSession,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /**
     * 이메일 회원가입. 성공 시 로그인 상태가 되어 고양이 이름을 짓는 온보딩으로 이동한다.
     *
     * 발급 토큰 저장은 data 레이어에서 담당한다.
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
    ): Result<Unit> = try {
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
        navigationHelper.navigateToAsRoot(OnboardingPage)
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        handleSignUpError(e)
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        showError(e.toUserMessage())
        Result.failure(e)
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
        return try {
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
            socialSignUpSession.clear()
            navigationHelper.navigateToAsRoot(OnboardingPage)
            Result.success(Unit)
        } catch (e: HttpResponseException) {
            handleSocialSignUpError(e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e.toUserMessage())
            Result.failure(e)
        }
    }

    /** 소셜 가입을 진행할 토큰이 있는지. 프로세스 종료 뒤 복원되거나 외부 링크로 들어오면 없다. */
    fun hasPendingSocialSignUp(): Boolean = !socialSignUpSession.pendingToken.isNullOrBlank()

    /** 가입 대기 토큰 없이 소셜 가입 화면에 들어왔을 때 안내하고 이전 화면으로 돌려보낸다. */
    fun leaveSocialSignUpWithoutSession() {
        messageHelper.showOneButtonDialog(
            descText = SOCIAL_SESSION_MISSING_MESSAGE,
            cantIgnore = true,
            buttonText = CONFIRM_BUTTON_TEXT,
            onClickButton = { navigationHelper.navigateToBack() },
        )
    }

    /** 소셜 가입 화면을 떠날 때 가입 대기 토큰을 버린다. */
    fun abandonSocialSignUp() {
        socialSignUpSession.clear()
    }

    private fun handleSignUpError(e: HttpResponseException) {
        val errorType = e.handlingErrorOnUseCase<AuthErrorType>()
        when {
            errorType != null -> messageHelper.showOneButtonDialog(descText = errorType.errorMsg)
            e.rawCode == HTTP_CONFLICT -> showError(ALREADY_REGISTERED_EMAIL_MESSAGE)
            e.isCommonErrorHandling() -> executeCommonErrorHanding(e)
            else -> showError(SIGN_UP_FAILED_MESSAGE)
        }
    }

    /**
     * 소셜 가입 실패. 토큰이 만료됐거나(401) 이미 가입된 계정이면(409) 다시 로그인해야 하므로
     * 토큰을 버리고 초기(로그인) 화면으로 보낸다. 그 외에는 토큰을 남겨 다시 제출할 수 있게 한다.
     */
    private fun handleSocialSignUpError(e: HttpResponseException) {
        when (e.rawCode) {
            HTTP_UNAUTHORIZED -> restartSocialLogin(SOCIAL_SIGN_UP_EXPIRED_MESSAGE)
            HTTP_CONFLICT -> restartSocialLogin(ALREADY_REGISTERED_ACCOUNT_MESSAGE)
            else -> showError(
                e.handlingErrorOnUseCase<AuthErrorType>()?.errorMsg
                    ?: if (e.rawCode == HTTP_BAD_REQUEST) INVALID_PROFILE_MESSAGE else TEMPORARY_ERROR_MESSAGE,
            )
        }
    }

    private fun restartSocialLogin(message: String) {
        socialSignUpSession.clear()
        messageHelper.showOneButtonDialog(
            descText = message,
            cantIgnore = true,
            buttonText = CONFIRM_BUTTON_TEXT,
            onClickButton = { navigationHelper.navigateToInitial() },
        )
    }

    private fun showError(message: String) {
        messageHelper.showOneButtonDialog(descText = message, buttonText = CONFIRM_BUTTON_TEXT)
    }

    private fun Exception.toUserMessage(): String =
        if (this is IOException) NETWORK_ERROR_MESSAGE else TEMPORARY_ERROR_MESSAGE

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_CONFLICT = 409

        const val SOCIAL_SESSION_MISSING_MESSAGE = "가입 정보가 만료됐어요. 다시 로그인해주세요."
        const val SOCIAL_SIGN_UP_EXPIRED_MESSAGE = "가입 유효 시간이 지났어요. 다시 로그인해주세요."
        const val ALREADY_REGISTERED_ACCOUNT_MESSAGE = "이미 가입된 계정이에요. 다시 로그인해주세요."
        const val ALREADY_REGISTERED_EMAIL_MESSAGE = "이미 가입된 이메일이에요. 로그인해주세요."
        const val INVALID_PROFILE_MESSAGE = "입력한 정보를 확인한 뒤 다시 시도해주세요."
        const val SIGN_UP_FAILED_MESSAGE = "회원가입에 실패했어요. 다시 시도해주세요."
        const val NETWORK_ERROR_MESSAGE = "네트워크 연결을 확인한 뒤 다시 시도해주세요."
        const val TEMPORARY_ERROR_MESSAGE = "일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요."
        const val CONFIRM_BUTTON_TEXT = "확인"
    }
}
