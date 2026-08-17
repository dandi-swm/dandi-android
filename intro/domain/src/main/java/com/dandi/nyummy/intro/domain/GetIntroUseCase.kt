package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.auth.domain.LoginPage
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.home.domain.HomePage
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
     * 앱 시작 분기. 저장된 리프레시 토큰이 있으면 홈, 없으면 로그인을 루트로 이동한다.
     *
     * 토큰 유효성은 여기서 검증하지 않는다 — 만료된 토큰은 이후 요청에서
     * Authenticator 의 재발급 실패 → 공통 401 처리(세션 만료)로 걸러진다.
     */
    suspend operator fun invoke() {
        if (repository.hasRefreshToken()) {
            navigationHelper.navigateToAsRoot(HomePage)
        } else {
            navigationHelper.navigateToAsRoot(LoginPage)
        }
    }
}
