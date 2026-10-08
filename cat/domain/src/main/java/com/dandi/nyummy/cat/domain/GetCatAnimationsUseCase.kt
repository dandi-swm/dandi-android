package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 내 고양이의 상태별 애니메이션 조회.
 *
 * 냐미 애니메이션은 화면을 꾸미는 요소라서, 받지 못해도 화면은 정지 포즈로 계속 쓸 수 있다.
 * 그래서 고양이가 없거나(404) 서버나 네트워크가 실패해도 다이얼로그나 화면 이동 없이 [Result.failure]만 돌려준다.
 * 공통 404 처리는 "뒤로"를 보내므로 여기서는 쓰지 않는다. 세션 만료(401)만 공통 처리로 넘긴다.
 */
class GetCatAnimationsUseCase @Inject constructor(
    private val repository: CatRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(): Result<CatAnimationSetVO> = try {
        Result.success(repository.getAnimations())
    } catch (e: HttpResponseException) {
        if (e.rawCode == UNAUTHORIZED) executeCommonErrorHanding(e)
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        const val UNAUTHORIZED = 401
    }
}
