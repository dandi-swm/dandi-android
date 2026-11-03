package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.shop.entity.CoinHistoryPageVO
import com.dandi.nyummy.tti.TTIHelper
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/** 코인 내역 한 묶음(최신순). 서버 오류는 공통 안내를 하고 실패를 돌려준다. */
class GetCoinHistoriesUseCase @Inject constructor(
    private val repository: ShopRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {
    suspend operator fun invoke(cursor: Long? = null, size: Int = PAGE_SIZE): Result<CoinHistoryPageVO> = try {
        Result.success(repository.getCoinHistories(cursor, size.coerceIn(1, MAX_PAGE_SIZE)))
    } catch (e: HttpResponseException) {
        handleHttpError<ShopErrorType>(e, onDomainError = {})
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        const val PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 50
    }
}
