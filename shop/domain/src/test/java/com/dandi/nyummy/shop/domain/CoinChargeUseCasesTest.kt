package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import com.dandi.nyummy.shop.entity.CoinChargeGrantVO
import com.dandi.nyummy.shop.entity.CoinChargeNoticeVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinChargeUseCasesTest {

    private val event = CoinChargeEventVO(
        eventId = "2026-11-coin-charge",
        group = "B",
        notice = CoinChargeNoticeVO(title = "냐미 상점이 문을 열었어요!", ctaText = "코인 충전하기"),
        packages = CoinChargePackages.of("B"),
    )
    private val pack = event.packages.first()

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

        assertEquals(CoinChargeClaimResult.Granted(CoinChargeGrantVO(grantedCoins = 3000, balance = 3860)), result)
        assertEquals(setOf(event.eventId), repository.claimed)
        assertEquals(listOf(1L), repository.claimRequests)
        assertEquals(1, shopRepository.refreshCount)
        val granted = analytics.events.single()
        assertEquals("coin_charge_granted", granted.name)
        assertEquals(mapOf("event_id" to event.eventId, "ab_group" to "B", "package_id" to "1"), granted.params)
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
    fun `다시 설치해 기기 기록이 없는데 서버가 이미 받았다고 하면 기록을 되살리고 이미 받았다고 한다`() = runBlocking {
        repository.claimError = httpException(409, ShopErrorType.COIN_GIFT_ALREADY_CLAIMED.type)
        val notice = GetCoinChargeNoticeUseCase(GetCoinChargeOfferUseCase(repository, tracker))
        assertEquals(event, notice())

        assertEquals(CoinChargeClaimResult.AlreadyClaimed, claim()(event, pack))

        assertEquals(setOf(event.eventId), repository.claimed)
        assertTrue(messageHelper.toasts.isEmpty())
        assertTrue(messageHelper.dialogs.isEmpty())
        assertEquals(0, shopRepository.refreshCount)
        assertEquals("already_claimed_server", analytics.events.last().params["reason"])
        // 되살린 기록으로 다음부터는 서버에 묻지 않고, 홈 공지도 다시 뜨지 않는다.
        assertEquals(CoinChargeClaimResult.AlreadyClaimed, claim()(event, pack))
        assertEquals(1, repository.claimRequests.size)
        assertNull(notice())
    }

    @Test
    fun `코드가 빠져도 409면 이미 받은 것으로 본다`() = runBlocking {
        repository.claimError = httpException(409)

        assertEquals(CoinChargeClaimResult.AlreadyClaimed, claim()(event, pack))
        assertEquals(setOf(event.eventId), repository.claimed)
        assertTrue(messageHelper.toasts.isEmpty())
    }

    @Test
    fun `묶음 번호가 없다는 404면 다시 눌러 달라고 알리고 package_not_found로 남긴다`() = runBlocking {
        repository.claimError = httpException(404, ShopErrorType.COIN_PACKAGE_NOT_FOUND.type)

        assertEquals(CoinChargeClaimResult.Failed, claim()(event, pack))
        assertEquals(listOf(StringResource.SHOP_COIN_CHARGE_FAILED.name), messageHelper.toasts)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertTrue(repository.claimed.isEmpty())
        assertEquals("package_not_found", analytics.events.single().params["reason"])
    }

    @Test
    fun `그룹 헤더가 틀렸다는 400이면 다시 눌러 달라고 알리고 invalid_request로 남긴다`() = runBlocking {
        repository.claimError = httpException(400, "api.common.invalidInputValue")

        assertEquals(CoinChargeClaimResult.Failed, claim()(event, pack))
        assertEquals(listOf(StringResource.SHOP_COIN_CHARGE_FAILED.name), messageHelper.toasts)
        assertEquals("invalid_request", analytics.events.single().params["reason"])
    }

    @Test
    fun `응답을 받지 못하면 다시 눌러 달라고 알리고 까닭을 남긴다`() = runBlocking {
        repository.claimError = java.io.IOException("offline")

        assertEquals(CoinChargeClaimResult.Failed, claim()(event, pack))
        assertEquals(listOf(StringResource.SHOP_COIN_CHARGE_FAILED.name), messageHelper.toasts)
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
        tracker.tabClick(event, claimed = true)
        tracker.view(event, source = "shop_tab", claimed = false)
        tracker.packageClick(event, pack)
        tracker.sheetClose(event, pack)
        tracker.payClick(event, pack)
        tracker.repeatClick(event, pack)
        tracker.resultAction(event, "home")
        tracker.exit(event, "list")

        assertEquals(
            listOf(
                "coin_charge_notice_view", "coin_charge_notice_click", "coin_charge_notice_close", "coin_charge_tab_click", "coin_charge_view",
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
        assertEquals("1", pay.params["package_id"])
        assertEquals(mapOf("price" to 1500L, "coins" to 300L), pay.numberParams)
    }
}

internal class FakeCoinChargeRepository(var current: CoinChargeEventVO?) : CoinChargeRepository {
    val claimed = mutableSetOf<String>()
    val claimRequests = mutableListOf<Long>()
    var claimError: Exception? = null

    override fun getEvent(): CoinChargeEventVO? = current
    override suspend fun isClaimed(eventId: String): Boolean = eventId in claimed
    override suspend fun markClaimed(eventId: String) {
        claimed += eventId
    }

    override suspend fun claim(event: CoinChargeEventVO, coinPackageId: Long): CoinChargeGrantVO {
        claimRequests += coinPackageId
        claimError?.let { throw it }
        return CoinChargeGrantVO(grantedCoins = 3000, balance = 3860)
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

class CoinChargePackagesTest {

    @Test
    fun `서버와 같은 묶음표 - 묶음 번호 1~3에 300·1,500·3,000코인, 가격은 그룹마다 다르다`() {
        assertEquals(listOf(1000, 5000, 10000), CoinChargePackages.of("A").map { it.price })
        assertEquals(listOf(1500, 7500, 15000), CoinChargePackages.of("B").map { it.price })
        assertEquals(listOf(1L, 2L, 3L), CoinChargePackages.of("B").map { it.coinPackageId })
        assertTrue(CoinChargePackages.of("C").isEmpty())
    }

    @Test
    fun `그룹은 A·B만 받고 공백과 대소문자는 맞춘다`() {
        assertEquals(listOf("A", "B", null, null), listOf("A", " b", "C", null).map(CoinChargePackages::groupOf))
    }
}
