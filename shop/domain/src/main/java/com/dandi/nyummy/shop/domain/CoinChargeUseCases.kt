package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.registeredErrorType
import com.dandi.nyummy.common.domain.helper.ImagePrefetchHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import com.dandi.nyummy.shop.entity.CoinChargeGrantVO
import com.dandi.nyummy.shop.entity.CoinChargeOfferVO
import com.dandi.nyummy.shop.entity.CoinChargePackageVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 지금 기기에서 볼 코인 충전 이벤트와 받음 여부. 이벤트가 없으면 null이다.
 * 이벤트를 읽을 때마다 이 기기의 A/B 그룹을 사용자 속성으로 남긴다.
 */
class GetCoinChargeOfferUseCase @Inject constructor(
    private val repository: CoinChargeRepository,
    private val tracker: CoinChargeTracker,
) {
    suspend operator fun invoke(): CoinChargeOfferVO? {
        val event = repository.getEvent() ?: return null
        tracker.rememberGroup(event)
        return CoinChargeOfferVO(event = event, isClaimed = repository.isClaimedOrFalse(event.eventId))
    }
}

/**
 * 홈에 띄울 코인 충전 이벤트 공지. 공지가 없는 이벤트이거나 이미 코인을 받은 사람이면 null이다.
 * "다시 보지 않기"로 숨긴 공지는 홈이 다른 공지와 같이 거른다(구분값은 [CoinChargeEventVO.eventId]).
 */
class GetCoinChargeNoticeUseCase @Inject constructor(
    private val getOffer: GetCoinChargeOfferUseCase,
) {
    suspend operator fun invoke(): CoinChargeEventVO? =
        getOffer()?.takeIf { !it.isClaimed && it.event.notice != null }?.event
}

/**
 * 홈에 띄울 코인 충전 이벤트 공지 사진을 미리 받아 둔다. 인트로와 방 준비 화면이 홈 공지 사진과 함께 부르고, 같은 상한까지 기다린다.
 * 공지가 없거나 이미 코인을 받은 회차면 받지 않는다. 받지 못한 사진은 알리지 않는다(홈이 그릴 때 다시 받는다).
 */
class PrefetchCoinChargeNoticeUseCase @Inject constructor(
    private val getNotice: GetCoinChargeNoticeUseCase,
    private val imagePrefetchHelper: ImagePrefetchHelper,
) {
    suspend operator fun invoke() {
        val imageUrl = getNotice()?.notice?.imageUrl ?: return
        imagePrefetchHelper.prefetchImages(listOf(imageUrl))
    }
}

/** 이벤트 코인 받기 결과. */
sealed interface CoinChargeClaimResult {
    /** 받았다. 상단 바 잔액도 다시 읽었다. */
    data class Granted(val grant: CoinChargeGrantVO) : CoinChargeClaimResult

    /** 이번 회차 코인을 이미 받았다(기기 기록). 서버에 묻지 않고, 화면은 안내 다이얼로그를 띄운다. */
    data object AlreadyClaimed : CoinChargeClaimResult

    /** 받지 못했다. 안내는 이 UseCase가 이미 했다. */
    data object Failed : CoinChargeClaimResult
}

/**
 * "결제하기"를 누른 뒤 이번 한 번만 선물 코인을 받는다(실제 결제는 하지 않는다, `POST /api/v1/coins/gifts`).
 * 받으면 기기에 받음을 남기고 상단 바 잔액을 다시 읽는다. 단계 결과(받음, 못 받음과 그 까닭)는 분석 이벤트로 남긴다.
 * 서버 오류(로그인 만료, 5xx)는 공통 안내를 하고, 그 밖의 실패는 다시 눌러 달라고 알린다.
 * 같은 사람이 두 번 받는 것은 서버가 막는다(다시 보내면 코인을 더 주지 않고 200으로 지금 잔액을 준다).
 */
class ClaimCoinChargeUseCase @Inject constructor(
    private val repository: CoinChargeRepository,
    private val shopRepository: ShopRepository,
    private val tracker: CoinChargeTracker,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(event: CoinChargeEventVO, pack: CoinChargePackageVO): CoinChargeClaimResult {
        if (repository.isClaimedOrFalse(event.eventId)) {
            tracker.grantFailed(event, pack, CoinChargeTracker.Values.REASON_ALREADY_CLAIMED)
            return CoinChargeClaimResult.AlreadyClaimed
        }
        return try {
            val grant = repository.claim(event, pack.coinPackageId)
            markClaimedQuietly(event.eventId)
            tracker.granted(event, pack, grant.grantedCoins)
            refreshBalanceQuietly()
            CoinChargeClaimResult.Granted(grant)
        } catch (e: HttpResponseException) {
            // 앱을 지웠다 다시 깔면 기기 받음 기록이 없어 다시 들어올 수 있다. 서버가 이미 받았다고 하면 기록을 되살려
            // 이미 받은 사람과 똑같이 안내하고, 상점 코인 탭의 Event 칩과 홈 공지도 다시 뜨지 않게 한다.
            if (e.isCoinGiftAlreadyReceived()) {
                markClaimedQuietly(event.eventId)
                tracker.grantFailed(event, pack, CoinChargeTracker.Values.REASON_ALREADY_CLAIMED_ON_SERVER)
                return CoinChargeClaimResult.AlreadyClaimed
            }
            var reason = CoinChargeTracker.Values.REASON_SERVER_ERROR
            handleHttpError<ShopErrorType>(
                e,
                onDomainError = {
                    reason = CoinChargeTracker.Values.REASON_PACKAGE_NOT_FOUND
                    messageHelper.showToast(ShopMessages.COIN_CHARGE_FAILED)
                },
                onUnknownError = {
                    reason = CoinChargeTracker.Values.REASON_INVALID_REQUEST
                    messageHelper.showToast(ShopMessages.COIN_CHARGE_FAILED)
                },
            )
            tracker.grantFailed(event, pack, reason)
            CoinChargeClaimResult.Failed
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            tracker.grantFailed(event, pack, CoinChargeTracker.Values.REASON_NETWORK_OR_UNKNOWN)
            messageHelper.showToast(ShopMessages.COIN_CHARGE_FAILED)
            CoinChargeClaimResult.Failed
        }
    }

    /** 서버가 이 회차 코인을 이미 줬다고 했다. 코드 이름이 서버와 어긋나도 받기 API의 409는 이것뿐이라 같이 본다. */
    private fun HttpResponseException.isCoinGiftAlreadyReceived(): Boolean =
        registeredErrorType<ShopErrorType>() == ShopErrorType.COIN_GIFT_ALREADY_RECEIVED || rawCode == HTTP_CONFLICT

    /** 기기에 쓰지 못해도 서버가 두 번째 지급을 막으므로 알리지 않는다. */
    private suspend fun markClaimedQuietly(eventId: String) {
        try {
            repository.markClaimed(eventId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    /** 잔액을 다시 읽지 못하면 다음에 상점이나 코인 내역이 보일 때 다시 읽는다. */
    private suspend fun refreshBalanceQuietly() {
        try {
            shopRepository.refreshSummary()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }
}

/** 기기 저장소를 읽지 못하면 받지 않은 것으로 본다(두 번째 지급은 서버가 막는다). */
internal suspend fun CoinChargeRepository.isClaimedOrFalse(eventId: String): Boolean = try {
    isClaimed(eventId)
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    false
}

private const val HTTP_CONFLICT = 409
