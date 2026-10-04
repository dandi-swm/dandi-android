package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/** 온보딩 마무리(이름을 받아 준 고양이와 함께 시작하기). 홈을 루트로 이동한다. */
class FinishOnboardingUseCase @Inject constructor(
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    operator fun invoke() {
        navigationHelper.navigateToAsRoot(HomePage)
    }
}
