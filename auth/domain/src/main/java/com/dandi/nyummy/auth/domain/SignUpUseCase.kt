package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.Gender
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

class SignUpUseCase @Inject constructor(
    private val repository: AuthRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /**
     * 회원가입. 성공 시 로그인 상태가 되어 홈으로 이동한다.
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
            emailVerifiedToken = emailVerifiedToken,
            password = password,
            confirmPassword = confirmPassword,
            nickname = nickname,
            gender = gender,
            birth = birth,
            height = height,
            weight = weight,
        )
        navigationHelper.navigateToAsRoot(HomePage)
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        handleSignUpError(e)
        Result.failure(e)
    }

    private fun handleSignUpError(e: HttpResponseException) {
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
