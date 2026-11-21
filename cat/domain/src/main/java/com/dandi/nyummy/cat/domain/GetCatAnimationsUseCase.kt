package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.runSuspendCatching
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 내 고양이 체형의 상황별 애니메이션. 앱 에셋에서 읽으므로 네트워크를 기다리지 않는다.
 * 체형은 기억해 둔 값(없으면 NORMAL)을 쓰고, 새로 읽는 일은 [RefreshCatWeightUseCase]가 한다.
 *
 * 냐미 애니메이션은 화면을 꾸미는 요소라서, 읽지 못해도 화면은 기본 냐미로 계속 쓸 수 있다.
 * 그래서 실패해도 다이얼로그나 화면 이동 없이 [Result.failure]만 돌려준다.
 */
class GetCatAnimationsUseCase @Inject constructor(
    private val repository: CatRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(): Result<CatAnimationSetVO> = runSuspendCatching {
        repository.getAnimations().also { check(it.animations.isNotEmpty()) { "앱 에셋에 냐미 애니메이션이 없다" } }
    }
}
