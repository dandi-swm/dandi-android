package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import com.dandi.nyummy.shop.entity.CoinChargeGrantVO
import com.dandi.nyummy.shop.entity.CoinChargeNoticeVO
import com.dandi.nyummy.shop.entity.CoinChargePackageVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinChargeUseCasesTest {

    private val pack = CoinChargePackageVO(id = "coin_300", coins = 300, price = 1000)
    private val event = CoinChargeEventVO(
        eventId = "2026-11-coin-charge",
        group = "B",
        grantCoins = 3000,
        notice = CoinChargeNoticeVO(title = "냐미 상점이 문을 열었어요!", ctaText = "코인 충전하기"),
        packages = listOf(pack),
    )

    private val repository = FakeCoinChargeRepository(event)
    private val shopRepository = FakeShopRepository()
    private val analytics = RecordingCoinChargeAnalytics()
    private val tracker = CoinChargeTracker(analytics)
    private val messageHelper = RecordingMessageHelper()

    private fun claim() = ClaimCoinChargeUseCase(
        repository, shopRepository, tracker, FakeResourceHelper, messageHelper, RecordingNavigationHelper(), FakeTTIHelper,
    )

    @Test
    fun `이벤트를 읽으면 A·B 그룹을 사용자 속성으로 남긴다`() = runBlocking {
        val offer = GetCoinChargeOfferUseCase(repository, tracker)()

        assertEquals(event, offer?.event)
        assertEquals(listOf(AnalyticsHelper.UserProperty.COIN_CHARGE_GROUP to "B"), analytics.userProperties)
    }

    @Test
    fun `이벤트가 없으면 사용자 속성을 건드리지 않는다`() = runBlocking {
        repository.current = null

        assertNull(GetCoinChargeOfferUseCase(repository, tracker)())
        assertTrue(analytics.userProperties.isEmpty())
    }

    @Test
    fun `이미 코인을 받은 사람에게는 홈 공지를 주지 않는다`() = runBlocking {
        val notice = GetCoinChargeNoticeUseCase(GetCoinChargeOfferUseCase(repository, tracker))
        assertEquals(event, notice())

        repository.claimed += event.eventId
        assertNull(notice())
    }

    @Test
    fun `받으면 기기에 받음을 남기고 잔액을 다시 읽고 받은 코인 수를 남긴다`() = runBlocking {
        val result = claim()(event, pack)

        assertEquals(CoinChargeClaimResult.Granted(CoinChargeGrantVO(grantedCoins = 3000)), result)
        assertEquals(setOf(event.eventId), repository.claimed)
        assertEquals(listOf("coin_300"), repository.claimRequests)
        assertEquals(1, shopRepository.refreshCount)
        val granted = analytics.events.single()
        assertEquals("coin_charge_granted", granted.name)
        assertEquals(mapOf("event_id" to event.eventId, "ab_group" to "B", "package_id" to "coin_300"), granted.params)
        assertEquals(mapOf("coins" to 3000L), granted.numberParams)
    }

    @Test
    fun `기기에 받음이 남아 있으면 서버에 묻지 않고 이미 받았다고 한다`() = runBlocking {
        repository.claimed += event.eventId

        assertEquals(CoinChargeClaimResult.AlreadyClaimed, claim()(event, pack))
        assertTrue(repository.claimRequests.isEmpty())
        assertEquals("already_claimed", analytics.events.single().params["reason"])
    }

    @Test
    fun `서버가 이미 받았다고 하면 기기에도 남기고 이미 받았다고 한다`() = runBlocking {
        repository.claimError = httpException(409, ShopErrorType.COIN_CHARGE_ALREADY_CLAIMED.type)

        assertEquals(CoinChargeClaimResult.AlreadyClaimed, claim()(event, pack))
        assertEquals(setOf(event.eventId), repository.claimed)
        assertEquals("already_claimed", analytics.events.single().params["reason"])
    }

    @Test
    fun `지급이 막히면 다시 눌러 달라고 알리고 까닭을 남긴다`() = runBlocking {
        repository.claimError = IllegalStateException("지급 API가 아직 없다")

        assertEquals(CoinChargeClaimResult.Failed, claim()(event, pack))
        assertEquals(listOf(ShopMessages.COIN_CHARGE_FAILED), messageHelper.toasts)
        assertTrue(repository.claimed.isEmpty())
        assertEquals("network_or_unknown", analytics.events.single().params["reason"])
    }

    @Test
    fun `서버 5xx는 공통 안내를 하고 server_error로 남긴다`() = runBlocking {
        repository.claimError = httpException(500)

        assertEquals(CoinChargeClaimResult.Failed, claim()(event, pack))
        assertEquals(1, messageHelper.dialogs.size)
        assertEquals("server_error", analytics.events.single().params["reason"])
    }

    @Test
    fun `모든 단계 이벤트에 이벤트 구분값과 그룹이 붙는다`() {
        tracker.noticeView(event)
        tracker.noticeClick(event)
        tracker.noticeClose(event, dontShowAgain = true)
        tracker.view(event, source = "notice", claimed = false)
        tracker.packageClick(event, pack)
        tracker.sheetClose(event, pack)
        tracker.payClick(event, pack)
        tracker.repeatClick(event, pack)
        tracker.resultAction(event, "home")
        tracker.exit(event, "list")

        assertEquals(
            listOf(
                "coin_charge_notice_view", "coin_charge_notice_click", "coin_charge_notice_close", "coin_charge_view",
                "coin_charge_package_click", "coin_charge_sheet_close", "coin_charge_pay_click", "coin_charge_repeat_click",
                "coin_charge_result_action", "coin_charge_exit",
            ),
            analytics.events.map { it.name },
        )
        analytics.events.forEach {
            assertEquals(event.eventId, it.params["event_id"])
            assertEquals("B", it.params["ab_group"])
            assertTrue("${it.name} 이름은 40자 안", it.name.length <= 40)
        }
        val pay = analytics.events.first { it.name == "coin_charge_pay_click" }
        assertEquals(mapOf("price" to 1000L, "coins" to 300L), pay.numberParams)
    }
}

internal class FakeCoinChargeRepository(var current: CoinChargeEventVO?) : CoinChargeRepository {
    val claimed = mutableSetOf<String>()
    val claimRequests = mutableListOf<String>()
    var claimError: Exception? = null

    override fun getEvent(): CoinChargeEventVO? = current
    override suspend fun isClaimed(eventId: String): Boolean = eventId in claimed
    override suspend fun markClaimed(eventId: String) {
        claimed += eventId
    }

    override suspend fun claim(event: CoinChargeEventVO, packageId: String): CoinChargeGrantVO {
        claimRequests += packageId
        claimError?.let { throw it }
        return CoinChargeGrantVO(grantedCoins = event.grantCoins)
    }
}

internal class RecordingCoinChargeAnalytics : AnalyticsHelper {
    val events = mutableListOf<AnalyticsEvent>()
    val userProperties = mutableListOf<Pair<String, String?>>()

    override fun logScreenView(screenName: String) = Unit
    override fun logEvent(event: AnalyticsEvent) {
        events += event
    }

    override fun setUserProperty(name: String, value: String?) {
        userProperties += name to value
    }
}
