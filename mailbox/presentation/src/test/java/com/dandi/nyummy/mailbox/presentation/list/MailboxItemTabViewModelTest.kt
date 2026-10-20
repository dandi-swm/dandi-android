package com.dandi.nyummy.mailbox.presentation.list

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.mailbox.domain.ClaimAllMailsUseCase
import com.dandi.nyummy.mailbox.domain.ClaimMailUseCase
import com.dandi.nyummy.mailbox.domain.DeleteMailUseCase
import com.dandi.nyummy.mailbox.domain.GetMailsUseCase
import com.dandi.nyummy.mailbox.domain.MailboxRepository
import com.dandi.nyummy.mailbox.domain.RestoreMailUseCase
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.entity.InquiryPageVO
import com.dandi.nyummy.mailbox.entity.InquiryVO
import com.dandi.nyummy.mailbox.entity.MailCategory
import com.dandi.nyummy.mailbox.entity.MailPageVO
import com.dandi.nyummy.mailbox.entity.MailVO
import com.dandi.nyummy.mailbox.entity.RewardVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MailboxItemTabViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeMailboxRepository()
    private val messageHelper = RecordingMessageHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `화면이 보일 때 우편을 받는다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1), mail(2, isClaimed = true)))
        val viewModel = createViewModel()

        viewModel.onIntent(MailboxIntent.ScreenResumed)
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertEquals(listOf(1L, 2L), items.mails.map { it.mailId })
        assertTrue(items.hasUnclaimed)
    }

    @Test
    fun `행 그림은 우편 종류의 CDN 경로에 그림 번호를 붙인 주소다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(
            mails = listOf(
                mail(1).copy(category = MailCategory.REWARD_BOX, imageId = 3L),
                mail(2).copy(category = MailCategory.ITEM, imageId = 12L),
                mail(3).copy(imageId = null),
            ),
        )
        val viewModel = createViewModel()

        viewModel.onIntent(MailboxIntent.ScreenResumed)
        advanceUntilIdle()

        assertEquals(
            listOf("https://cdn.nyummy.co.kr/reward-boxes/3.png", "https://cdn.nyummy.co.kr/items/12.png", ""),
            viewModel.uiState.value.items.mails.map { it.imageUrl },
        )
    }

    @Test
    fun `남은 기간은 올림한 날 수이고 받은 우편이면 없다`() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        repository.mailPage = MailPageVO(
            mails = listOf(
                mail(1, expiresAtMillis = now + DAY_MILLIS + DAY_MILLIS / 2),
                mail(2, isClaimed = true, expiresAtMillis = now + DAY_MILLIS),
                mail(3, expiresAtMillis = null),
            ),
        )
        val viewModel = loadedViewModel()

        val mails = viewModel.uiState.value.items.mails
        assertEquals(2, mails[0].remainingDays)
        assertNull(mails[1].remainingDays)
        assertNull(mails[2].remainingDays)
    }

    @Test
    fun `받기에 성공하면 그 행만 받았어요로 바뀐다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1), mail(2)))
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertTrue(items.mails.first { it.mailId == 1L }.isClaimed)
        assertFalse(items.mails.first { it.mailId == 2L }.isClaimed)
        assertTrue(items.claimingMailIds.isEmpty())
    }

    @Test
    fun `받는 중에 다시 눌러도 한 번만 요청한다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1)))
        val gate = CompletableDeferred<RewardVO>()
        repository.claimOverride = { gate.await() }
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        runCurrent()
        assertTrue(viewModel.uiState.value.items.isClaiming(1))
        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        gate.complete(RewardVO.empty)
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.claimedMailIds)
    }

    @Test
    fun `이미 받았거나 사라진 우편이면 안내하고 목록을 다시 받는다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1)))
        val viewModel = loadedViewModel()
        val requestsBefore = repository.mailRequests.size
        repository.claimOverride = { throw httpException(409, "api.mail.alreadyClaimed") }

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        advanceUntilIdle()

        assertEquals("이미 받았거나 기한이 지난 우편이에요", messageHelper.snackBars.single().messageText)
        assertEquals(requestsBefore + 1, repository.mailRequests.size)
        assertTrue(viewModel.uiState.value.items.claimingMailIds.isEmpty())
    }

    @Test
    fun `받기에 실패하면 버튼을 다시 살리고 안내한다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1)))
        val viewModel = loadedViewModel()
        repository.claimOverride = { throw httpException(400) }

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertFalse(items.mails.single().isClaimed)
        assertTrue(items.claimingMailIds.isEmpty())
        assertEquals(IconType.ERROR, messageHelper.snackBars.single().iconType)
    }

    @Test
    fun `모두 받기에 성공하면 불러온 우편을 받았어요로 바꾸고 목록을 다시 받는다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1), mail(2)))
        val viewModel = loadedViewModel()
        val requestsBefore = repository.mailRequests.size
        // 다시 받기 전에 화면 상태를 보려고 목록 응답을 멈춰 둔다.
        val gate = CompletableDeferred<MailPageVO>()
        repository.mailsOverride = { gate.await() }

        viewModel.onIntent(MailboxIntent.ClaimAllMails)
        runCurrent()

        val items = viewModel.uiState.value.items
        assertTrue(items.mails.all { it.isClaimed })
        assertFalse(items.isClaimingAll)
        assertEquals(1, repository.claimAllCount)
        assertEquals(requestsBefore + 1, repository.mailRequests.size)
        gate.complete(MailPageVO.empty)
        advanceUntilIdle()
    }

    @Test
    fun `안 받은 우편이 없으면 모두 받기를 요청하지 않는다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1, isClaimed = true)))
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.ClaimAllMails)
        advanceUntilIdle()

        assertEquals(0, repository.claimAllCount)
    }

    @Test
    fun `안 받은 우편은 지울 수 없다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1)))
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.DeleteMail(1))
        advanceUntilIdle()

        assertTrue(repository.deletedMailIds.isEmpty())
        assertEquals(1, viewModel.uiState.value.items.mails.size)
    }

    @Test
    fun `받은 우편을 지우면 목록에서 빼고 되돌리기로 원래 자리에 되살린다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1, isClaimed = true), mail(2, isClaimed = true)))
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.DeleteMail(1))
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.uiState.value.items.mails.map { it.mailId })
        val snackBar = messageHelper.snackBars.single()
        assertEquals("되돌리기", snackBar.callToActionText)

        snackBar.onClickCTA?.invoke()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.items.mails.map { it.mailId })
        assertEquals(listOf(1L), repository.restoredMailIds)
    }

    private fun TestScope.loadedViewModel(): MailboxViewModel {
        val viewModel = createViewModel()
        viewModel.onIntent(MailboxIntent.ScreenResumed)
        advanceUntilIdle()
        return viewModel
    }

    private fun TestScope.createViewModel(): MailboxViewModel {
        val navigationHelper = RecordingNavigationHelper()
        fun <T : Any> deps(build: (MailboxRepository, ResourceHelper, MessageHelper, NavigationHelper, TTIHelper) -> T) =
            build(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
        return MailboxViewModel(
            getMails = deps(::GetMailsUseCase),
            claimMail = deps(::ClaimMailUseCase),
            claimAllMails = deps(::ClaimAllMailsUseCase),
            deleteMail = deps(::DeleteMailUseCase),
            restoreMail = deps(::RestoreMailUseCase),
            navigationHelper = navigationHelper,
            messageHelper = messageHelper,
            ioScope = this,
        )
    }

    private fun mail(id: Long, isClaimed: Boolean = false, expiresAtMillis: Long? = null) = MailVO(
        mailId = id,
        category = MailCategory.COIN,
        title = "코인 $id",
        isClaimed = isClaimed,
        expiresAtMillis = expiresAtMillis,
        createdAtMillis = System.currentTimeMillis(),
    )

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}

/** 우편 API를 흉내 내는 저장소. 응답을 멈추거나 실패시키려면 override를 바꾼다. */
private class FakeMailboxRepository : MailboxRepository {
    var mailPage: MailPageVO = MailPageVO.empty
    var mailsOverride: suspend () -> MailPageVO = { mailPage }
    var claimOverride: suspend (Long) -> RewardVO = { RewardVO.empty }
    val mailRequests = mutableListOf<Pair<Long?, Int>>()
    val claimedMailIds = mutableListOf<Long>()
    var claimAllCount = 0
    val deletedMailIds = mutableListOf<Long>()
    val restoredMailIds = mutableListOf<Long>()

    override suspend fun getMails(cursor: Long?, size: Int): MailPageVO {
        mailRequests += cursor to size
        return mailsOverride()
    }

    override suspend fun claimMail(mailId: Long): RewardVO {
        claimedMailIds += mailId
        return claimOverride(mailId)
    }

    override suspend fun claimAllMails(): RewardVO {
        claimAllCount++
        return RewardVO.empty
    }

    override suspend fun deleteMail(mailId: Long) {
        deletedMailIds += mailId
    }

    override suspend fun restoreMail(mailId: Long) {
        restoredMailIds += mailId
    }

    override suspend fun getInquiries(cursor: Long?, size: Int): InquiryPageVO = InquiryPageVO.empty
    override suspend fun getInquiry(inquiryId: Long): InquiryVO? = null
    override suspend fun sendInquiry(category: InquiryCategory, title: String, content: String) = Unit
    override suspend fun deleteInquiry(inquiryId: Long) = Unit
    override suspend fun restoreInquiry(inquiryId: Long) = Unit
}

private fun httpException(code: Int, errorCode: String? = null): HttpResponseException = HttpResponseException(
    status = HttpResponseStatus.create(code),
    rawCode = code,
    errorRequestUrl = "https://test/api/v1/mails",
    msg = "Http Request Failed ($code)",
    cause = errorCode?.let(::Throwable),
)

private class RecordingMessageHelper : MessageHelper {
    data class SnackBarCall(
        val iconType: IconType,
        val messageText: String,
        val callToActionText: String?,
        val onClickCTA: (() -> Unit)?,
    )

    data class DialogCall(val descText: String, val cantIgnore: Boolean, val onClickButton: (() -> Unit)?)

    val snackBars = mutableListOf<SnackBarCall>()
    val dialogs = mutableListOf<DialogCall>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
        snackBars += SnackBarCall(iconType, messageText, callToActionText, onClickCTA)
    }

    override fun showSnackBar(
        iconType: IconType,
        messageRes: Int,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) = Unit

    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) {
        dialogs += DialogCall(descText, cantIgnore, onClickButton)
    }

    override fun showTwoButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        leftButtonText: String,
        onClickLeftButton: (() -> Unit)?,
        rightButtonText: String,
        onClickRightButton: (() -> Unit)?,
    ) = Unit
}

private class RecordingNavigationHelper : NavigationHelper {
    var initialCount = 0
    var backCount = 0

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) = Unit
    override fun navigateDeepLink(route: NavRoute) = Unit
    override fun navigateToBack() {
        backCount++
    }

    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() {
        initialCount++
    }

    override fun navigateToExternalLink(url: String) = Unit
}

private object FakeResourceHelper : ResourceHelper {
    override fun getString(resource: StringResource): String = ""
}

private object FakeTTIHelper : TTIHelper {
    override fun startTTITracking(page: TTIPage) = Unit
    override fun startTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITracking() = Unit
    override fun shotTTILogging() = Unit
    override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
}
