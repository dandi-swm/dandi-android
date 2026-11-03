package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import com.dandi.nyummy.shop.entity.CoinChargePackageVO
import javax.inject.Inject

/**
 * 코인 충전 이벤트의 단계를 분석 이벤트로 남긴다. 홈 공지와 충전 화면이 같이 써서, 단계마다 이벤트 구분값과 A/B 그룹이
 * 빠지지 않고 같은 이름으로 붙게 한다. 그룹은 이벤트를 읽을 때마다 사용자 속성(coin_charge_group)으로도 남겨, 나중에
 * 어떤 그룹에 속했는지 다른 이벤트(식사 기록, 구매 등)와 함께 나눠 볼 수 있게 한다.
 */
class CoinChargeTracker @Inject constructor(
    private val analyticsHelper: AnalyticsHelper,
) {
    /** 이 기기가 받은 A/B 그룹을 사용자 속성으로 남긴다. 같은 값을 여러 번 남겨도 된다. */
    fun rememberGroup(event: CoinChargeEventVO) {
        analyticsHelper.setUserProperty(AnalyticsHelper.UserProperty.COIN_CHARGE_GROUP, event.group)
    }

    fun noticeView(event: CoinChargeEventVO) =
        log(AnalyticsEvent.CoinChargeNoticeView(event.eventId, event.group))

    fun noticeClick(event: CoinChargeEventVO) =
        log(AnalyticsEvent.CoinChargeNoticeClick(event.eventId, event.group))

    fun noticeClose(event: CoinChargeEventVO, dontShowAgain: Boolean) =
        log(AnalyticsEvent.CoinChargeNoticeClose(event.eventId, event.group, dontShowAgain))

    fun view(event: CoinChargeEventVO, source: String, claimed: Boolean) =
        log(AnalyticsEvent.CoinChargeView(event.eventId, event.group, source, claimed))

    fun packageClick(event: CoinChargeEventVO, pack: CoinChargePackageVO) =
        log(AnalyticsEvent.CoinChargePackageClick(event.eventId, event.group, pack.id, pack.price, pack.coins))

    fun sheetClose(event: CoinChargeEventVO, pack: CoinChargePackageVO) =
        log(AnalyticsEvent.CoinChargeSheetClose(event.eventId, event.group, pack.id))

    fun payClick(event: CoinChargeEventVO, pack: CoinChargePackageVO) =
        log(AnalyticsEvent.CoinChargePayClick(event.eventId, event.group, pack.id, pack.price, pack.coins))

    fun granted(event: CoinChargeEventVO, pack: CoinChargePackageVO, coins: Int) =
        log(AnalyticsEvent.CoinChargeGranted(event.eventId, event.group, pack.id, coins))

    fun grantFailed(event: CoinChargeEventVO, pack: CoinChargePackageVO, reason: String) =
        log(AnalyticsEvent.CoinChargeGrantFailed(event.eventId, event.group, pack.id, reason))

    fun repeatClick(event: CoinChargeEventVO, pack: CoinChargePackageVO) =
        log(AnalyticsEvent.CoinChargeRepeatClick(event.eventId, event.group, pack.id, pack.price))

    fun resultAction(event: CoinChargeEventVO, action: String) =
        log(AnalyticsEvent.CoinChargeResultAction(event.eventId, event.group, action))

    fun exit(event: CoinChargeEventVO, stage: String) =
        log(AnalyticsEvent.CoinChargeExit(event.eventId, event.group, stage))

    private fun log(event: AnalyticsEvent) = analyticsHelper.logEvent(event)

    /** 분석 이벤트 매개변수 값. 철자를 이 파일에서만 정한다. */
    object Values {
        const val REASON_ALREADY_CLAIMED = "already_claimed"
        const val REASON_SERVER_ERROR = "server_error"
        const val REASON_NETWORK_OR_UNKNOWN = "network_or_unknown"

        const val ACTION_SHOP = "shop"
        const val ACTION_HOME = "home"
        const val ACTION_BACK = "back"

        const val STAGE_LIST = "list"
        const val STAGE_SHEET = "sheet"
        const val STAGE_RESULT = "result"
    }
}
