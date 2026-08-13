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
     * @param birth 생년월일 (`yyyy-MM-dd` 형식)
     * @param height 키 (cm)
     * @param weight 몸무게 (kg)
     */
    suspend fun signUp(
        email: String,
        password: String,
        nickname: String,
        gender: Gender,
        birth: String,
        height: Int,
        weight: Int,
    ): Result<Unit> = try {
        repository.signUp(
            email = email,
            password = password,
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
