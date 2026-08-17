package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.auth.domain.LoginPage
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.intro.entity.IntroVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

class GetIntroUseCase @Inject constructor(
    private val repository: IntroRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /**
     * 앱 시작 게이트 + 분기.
     *
     * 1. (예정) GET /intro 로 버전 게이트 통과 여부 확인 — API 준비 전이라 주석.
     * 2. 저장된 리프레시 토큰이 있으면 홈, 없으면 로그인을 루트로 이동한다.
     *
     * 토큰 유효성은 여기서 검증하지 않는다 — 만료된 토큰은 이후 요청에서
     * Authenticator 의 재발급 실패 → 공통 401 처리(세션 만료)로 걸러진다.
     */
    suspend operator fun invoke(): Result<IntroVO> = try {
//        val intro = repository.getIntro() // TODO: GET /intro 준비 시 주석 해제
        if (repository.hasRefreshToken()) {
            navigationHelper.navigateToAsRoot(HomePage)
        } else {
            navigationHelper.navigateToAsRoot(LoginPage)
        }
        Result.success(IntroVO.empty)
    } catch (e: HttpResponseException) {
        handleIntroError(e)
        Result.failure(e)
    }

    private fun handleIntroError(e: HttpResponseException) {
        if (e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
            return
        }
        val errorType = e.handlingErrorOnUseCase<IntroErrorType>() ?: return
        when (errorType) {
            IntroErrorType.REQUIRED_FORCE_UPDATE -> {
                messageHelper.showOneButtonDialog(
                    cantIgnore = true,
                    descText = errorType.errorMsg,
                    buttonText = "스토어로 이동",
                    // TODO: Play Store 딥링크 연결 전까지 임시로 뒤로가기(앱 종료)
                    onClickButton = { navigationHelper.navigateToBack() },
                )
            }
        }
    }
}
