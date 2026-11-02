package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isSessionExpired
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopCategory
import com.dandi.nyummy.shop.entity.ShopItemPageVO
import com.dandi.nyummy.shop.entity.ShopPromotionPageVO
import com.dandi.nyummy.shop.entity.ShopRecommendPageVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import com.dandi.nyummy.shop.entity.ShopTab
import com.dandi.nyummy.tti.TTIHelper
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** 상단 바 값(잔액·장바구니 종류 수)을 구독한다. 담기·장바구니·구매가 바로 반영된다. */
class ObserveShopSummaryUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    operator fun invoke(): StateFlow<ShopSummaryVO> = repository.summary
}

/**
 * 상단 바 값을 서버에서 다시 읽는다. 화면이 보일 때마다 부르므로 실패해도 안내하지 않고 이전 값을 둔다.
 * 로그인 만료만 공통 안내로 넘긴다.
 */
class RefreshShopSummaryUseCase @Inject constructor(
    private val repository: ShopRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(): Result<ShopSummaryVO> = try {
        Result.success(repository.refreshSummary())
    } catch (e: HttpResponseException) {
        if (e.isSessionExpired()) executeCommonErrorHanding(e)
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/** 추천 탭 SDUI 칸 목록. 실패하면 공통 오류를 안내하고 실패를 돌려준다(화면은 다시 시도 안내). */
class GetShopRecommendPageUseCase @Inject constructor(
    private val repository: ShopRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(): Result<ShopRecommendPageVO> = try {
        Result.success(repository.getRecommendPage())
    } catch (e: HttpResponseException) {
        handleHttpError<ShopErrorType>(e, onDomainError = {})
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/** 인테리어·방 배경 탭 상품 한 묶음. 방 배경 탭이면 [category]를 무시한다(서버가 400을 준다). */
class GetShopProductsUseCase @Inject constructor(
    private val repository: ShopRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(
        tab: ShopTab,
        category: ShopCategory?,
        sort: ProductSort,
        cursor: Long? = null,
        size: Int = PAGE_SIZE,
    ): Result<ShopItemPageVO> = try {
        val safeCategory = if (tab == ShopTab.INTERIOR) category else null
        Result.success(repository.getProducts(tab, safeCategory, sort, cursor, size.coerceIn(1, MAX_PAGE_SIZE)))
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

/**
 * 프로모션 상품 모음 한 묶음. 끝났거나 볼 수 없는 프로모션(`promotionNotFound`)이면 `Result.success(null)`이고
 * 화면은 끝난 프로모션 안내를 보인다. 그 밖의 실패는 공통 오류만 안내하고 실패를 돌려준다.
 */
class GetShopPromotionUseCase @Inject constructor(
    private val repository: ShopRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(
        promotionId: Long,
        cursor: Long? = null,
        size: Int = GetShopProductsUseCase.PAGE_SIZE,
    ): Result<ShopPromotionPageVO?> = try {
        Result.success(
            repository.getPromotionProducts(promotionId, cursor, size.coerceIn(1, GetShopProductsUseCase.MAX_PAGE_SIZE)),
        )
    } catch (e: HttpResponseException) {
        var ended = false
        handleHttpError<ShopErrorType>(e, onDomainError = { if (it == ShopErrorType.PROMOTION_NOT_FOUND) ended = true })
        if (ended) Result.success(null) else Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
