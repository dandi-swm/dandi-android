package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isSessionExpired
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 고양이 조회로 내 냐미 체형(과 이름)을 새로 읽어 기억한다. 그리는 체형이 바뀌었으면 true다.
 *
 * 체형은 화면을 꾸미는 정보라서, 받지 못하면 기억해 둔 체형을 그대로 쓴다. 그래서 실패해도 다이얼로그나 화면 이동 없이 false만 돌려준다.
 * 로그인 만료([isSessionExpired])만 공통 처리로 넘긴다.
 */
class RefreshCatWeightUseCase @Inject constructor(
    private val repository: CatRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(): Boolean = try {
        repository.refreshWeight()
    } catch (e: HttpResponseException) {
        if (e.isSessionExpired()) executeCommonErrorHanding(e)
        false
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }
}
