package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.CommonErrorPolicy
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopCategory
import com.dandi.nyummy.shop.entity.ShopItemPageVO
import com.dandi.nyummy.shop.entity.ShopPromotionPageVO
import com.dandi.nyummy.shop.entity.ShopRecommendPageVO
import com.dandi.nyummy.shop.entity.ShopSummaryVO
import com.dandi.nyummy.shop.entity.ShopTab
import com.dandi.nyummy.tti.TTIHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** 상단 바 값(잔액·장바구니 종류 수)을 구독한다. 담기·장바구니·구매가 바로 반영된다. */
class ObserveShopSummaryUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    operator fun invoke(): StateFlow<ShopSummaryVO> = repository.summary
}

/** 구매가 끝날 때마다 한 번 받는다. 상품 목록은 산 상품이 바로 "n개 보유"로 보이게 처음부터 다시 읽는다. */
class ObservePurchaseCompletedUseCase @Inject constructor(
    private val repository: ShopRepository,
) {
    operator fun invoke(): Flow<Unit> = repository.purchaseCompleted
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
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(): Result<ShopSummaryVO> =
        request(commonErrors = CommonErrorPolicy.SESSION_ONLY) { repository.refreshSummary() }
}

/**
 * 추천 탭 SDUI 칸 목록. 원격 구성의 배치 그룹(A/B 실험)을 서버에 보내 그 그룹의 칸 순서로 받고, 그룹을 사용자 속성으로 남긴다.
 * 실패하면 공통 오류를 안내하고 실패를 돌려준다(화면은 다시 시도 안내).
 */
class GetShopRecommendPageUseCase @Inject constructor(
    private val repository: ShopRepository,
    private val remoteConfigHelper: RemoteConfigHelper,
    private val tracker: ShopTracker,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(): Result<ShopRecommendPageVO> = request(ShopErrorType.entries) {
        val layoutVariant = ShopLayoutVariant.of(remoteConfigHelper.get(RemoteConfigKey.ShopLayoutVariant))
        tracker.rememberLayoutGroup(layoutVariant)
        repository.getRecommendPage(layoutVariant)
    }
}

/** 인테리어·방 배경 탭 상품 한 묶음. 방 배경 탭이면 [category]를 무시한다(서버가 400을 준다). */
class GetShopProductsUseCase @Inject constructor(
    private val repository: ShopRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(
        tab: ShopTab,
        category: ShopCategory?,
        sort: ProductSort,
        cursor: Long? = null,
        size: Int = PAGE_SIZE,
    ): Result<ShopItemPageVO> = request(ShopErrorType.entries) {
        val safeCategory = if (tab == ShopTab.INTERIOR) category else null
        repository.getProducts(tab, safeCategory, sort, cursor, size.coerceIn(1, MAX_PAGE_SIZE))
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
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(
        promotionId: Long,
        cursor: Long? = null,
        size: Int = GetShopProductsUseCase.PAGE_SIZE,
    ): Result<ShopPromotionPageVO?> = request(
        ShopErrorType.entries,
        onBusinessError = { type, cause -> if (type == ShopErrorType.PROMOTION_NOT_FOUND) Result.success(null) else Result.failure(cause) },
    ) {
        repository.getPromotionProducts(promotionId, cursor, size.coerceIn(1, GetShopProductsUseCase.MAX_PAGE_SIZE))
    }
}
