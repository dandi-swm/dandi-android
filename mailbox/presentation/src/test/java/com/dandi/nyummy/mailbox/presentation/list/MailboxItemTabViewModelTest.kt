package com.dandi.nyummy.mailbox.presentation.list

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialLesson
import com.dandi.nyummy.mailbox.domain.ClaimAllMailsUseCase
import com.dandi.nyummy.mailbox.domain.ClaimMailUseCase
import com.dandi.nyummy.mailbox.domain.DeleteMailUseCase
import com.dandi.nyummy.mailbox.domain.GetMailsUseCase
import com.dandi.nyummy.mailbox.domain.MailboxRepository
import com.dandi.nyummy.mailbox.domain.RestoreMailUseCase
import com.dandi.nyummy.mailbox.entity.MailCategory
import com.dandi.nyummy.mailbox.entity.MailPageVO
import com.dandi.nyummy.mailbox.entity.MailVO
import com.dandi.nyummy.mailbox.entity.RewardBoxVO
import com.dandi.nyummy.mailbox.entity.RewardCoinVO
import com.dandi.nyummy.mailbox.entity.RewardItemVO
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
    private val navigationHelper = RecordingNavigationHelper()
    private val tutorialHelper = FakeTutorialHelper()
    private var isMyRoomEnabled = true

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
            listOf("https://cdn.nyummy.co.kr/reward-boxes/closed/3.png", "https://cdn.nyummy.co.kr/items/12.png", ""),
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
    fun `모두 받기를 누르면 받을 상자 중 가장 높은 등급으로 연출을 띄우고 받은 보상의 합계로 연다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(
            mails = listOf(box(1, imageId = 1L), box(2, imageId = 3L).copy(isClaimed = true), box(3, imageId = 2L), mail(4)),
        )
        val claimGate = CompletableDeferred<RewardVO>()
        repository.claimAllOverride = { claimGate.await() }
        val viewModel = loadedViewModel()
        val requestsBefore = repository.mailRequests.size

        viewModel.onIntent(MailboxIntent.ClaimAllMails)
        runCurrent()

        // 이미 받은 골드 상자는 빼고, 받을 상자 중 가장 높은 실버 상자다.
        val waiting = viewModel.uiState.value.boxOpening!!
        assertEquals(BoxOpeningPhase.Waiting, waiting.phase)
        assertEquals("https://cdn.nyummy.co.kr/reward-boxes/closed/2.png", waiting.closedImageUrl)
        assertEquals("https://cdn.nyummy.co.kr/reward-boxes/opened/2.png", waiting.openedImageUrl)
        assertTrue(viewModel.uiState.value.items.isClaimingAll)

        // 다시 받기 전에 화면 상태를 보려고 목록 응답을 멈춰 둔다.
        val mailsGate = CompletableDeferred<MailPageVO>()
        repository.mailsOverride = { mailsGate.await() }
        claimGate.complete(
            RewardVO(
                coin = RewardCoinVO(quantity = 300, imageId = 1L),
                items = listOf(RewardItemVO(imageId = 5L, name = "가을 담요", quantity = 2)),
            ),
        )
        runCurrent()

        val opening = viewModel.uiState.value.boxOpening!!
        assertEquals(BoxOpeningPhase.Opening, opening.phase)
        assertEquals(
            listOf(
                BoxRewardUiModel.Coin(imageUrl = "https://cdn.nyummy.co.kr/coins/1.png", quantity = 300),
                BoxRewardUiModel.Item(imageUrl = "https://cdn.nyummy.co.kr/items/5.png", name = "가을 담요", quantity = 2),
            ),
            opening.rewards,
        )
        val items = viewModel.uiState.value.items
        assertTrue(items.mails.all { it.isClaimed })
        assertFalse(items.isClaimingAll)
        assertEquals(1, repository.claimAllCount)
        assertEquals(requestsBefore + 1, repository.mailRequests.size)
        assertTrue(messageHelper.snackBars.isEmpty())
        mailsGate.complete(MailPageVO.empty)
        advanceUntilIdle()
    }

    @Test
    fun `상자 우편 없이 모두 받으면 상자 그림 없이 연다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1)))
        repository.claimAllOverride = { RewardVO(coin = RewardCoinVO(quantity = 100)) }
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.ClaimAllMails)
        advanceUntilIdle()

        val opening = viewModel.uiState.value.boxOpening!!
        assertEquals(BoxOpeningPhase.Opening, opening.phase)
        assertEquals("", opening.closedImageUrl)
        assertEquals("", opening.openedImageUrl)
        assertEquals(listOf(BoxRewardUiModel.Coin(imageUrl = "", quantity = 100)), opening.rewards)
    }

    @Test
    fun `모두 받기로 받은 보상이 없으면 무대가 걷힌 뒤 안내하고 목록을 다시 받는다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1)))
        val viewModel = loadedViewModel()
        val requestsBefore = repository.mailRequests.size

        viewModel.onIntent(MailboxIntent.ClaimAllMails)
        advanceUntilIdle()

        assertEquals(BoxOpeningFailure.AlreadyGone, viewModel.uiState.value.boxOpening?.failure)
        assertFalse(viewModel.uiState.value.items.mails.single().isClaimed)
        assertTrue(messageHelper.snackBars.isEmpty())
        assertEquals(requestsBefore, repository.mailRequests.size)

        viewModel.onIntent(MailboxIntent.BoxOpeningFailureShown)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.boxOpening)
        assertEquals("이미 받았거나 기한이 지난 우편이에요", messageHelper.snackBars.single().messageText)
        assertEquals(requestsBefore + 1, repository.mailRequests.size)
    }

    @Test
    fun `모두 받기에 실패하면 무대가 걷힌 뒤 오류를 안내한다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1)))
        repository.claimAllOverride = { throw httpException(400) }
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.ClaimAllMails)
        advanceUntilIdle()

        assertEquals(BoxOpeningFailure.Error, viewModel.uiState.value.boxOpening?.failure)
        val items = viewModel.uiState.value.items
        assertFalse(items.isClaimingAll)
        assertFalse(items.mails.single().isClaimed)

        viewModel.onIntent(MailboxIntent.BoxOpeningFailureShown)

        assertNull(viewModel.uiState.value.boxOpening)
        assertEquals(IconType.ERROR, messageHelper.snackBars.single().iconType)
    }

    @Test
    fun `상자 연출 중에는 모두 받기를 하지 않는다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(box(1), mail(2)))
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        advanceUntilIdle()
        viewModel.onIntent(MailboxIntent.ClaimAllMails)
        advanceUntilIdle()

        assertEquals(0, repository.claimAllCount)
        assertEquals(1L, viewModel.uiState.value.boxOpening?.mailId)
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

    @Test
    fun `보상 상자를 열면 바로 그 상자로 연출을 시작하고 응답이 오면 받은 보상으로 열린다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(box(1, imageId = 2L)))
        val gate = CompletableDeferred<RewardVO>()
        repository.claimOverride = { gate.await() }
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        runCurrent()

        val waiting = viewModel.uiState.value.boxOpening!!
        assertEquals(BoxOpeningPhase.Waiting, waiting.phase)
        assertEquals("https://cdn.nyummy.co.kr/reward-boxes/closed/2.png", waiting.closedImageUrl)
        assertEquals("https://cdn.nyummy.co.kr/reward-boxes/opened/2.png", waiting.openedImageUrl)
        assertEquals("상자 1", waiting.mailTitle)

        gate.complete(
            RewardVO(
                coin = RewardCoinVO(quantity = 100, imageId = 1L),
                items = listOf(RewardItemVO(imageId = 5L, name = "가을 담요", quantity = 2)),
                rewardBox = RewardBoxVO(imageId = 3L),
            ),
        )
        advanceUntilIdle()

        val opening = viewModel.uiState.value.boxOpening!!
        assertEquals(BoxOpeningPhase.Opening, opening.phase)
        // 응답의 상자 번호가 목록의 번호보다 우선이다.
        assertEquals("https://cdn.nyummy.co.kr/reward-boxes/closed/3.png", opening.closedImageUrl)
        assertEquals("https://cdn.nyummy.co.kr/reward-boxes/opened/3.png", opening.openedImageUrl)
        assertEquals(
            listOf(
                BoxRewardUiModel.Coin(imageUrl = "https://cdn.nyummy.co.kr/coins/1.png", quantity = 100),
                BoxRewardUiModel.Item(imageUrl = "https://cdn.nyummy.co.kr/items/5.png", name = "가을 담요", quantity = 2),
            ),
            opening.rewards,
        )
        val row = viewModel.uiState.value.items.mails.single()
        assertTrue(row.isClaimed)
        assertEquals("https://cdn.nyummy.co.kr/reward-boxes/opened/2.png", row.imageUrl)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `열리는 연출이 끝나거나 건너뛰면 보상을 보여 주고 계속하기로 닫는다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(box(1), box(2)))
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        advanceUntilIdle()
        viewModel.onIntent(MailboxIntent.CloseBoxOpening)
        assertEquals(BoxOpeningPhase.Opening, viewModel.uiState.value.boxOpening?.phase)

        viewModel.onIntent(MailboxIntent.BoxOpeningBurstFinished)
        assertEquals(BoxOpeningPhase.Revealed, viewModel.uiState.value.boxOpening?.phase)
        viewModel.onIntent(MailboxIntent.CloseBoxOpening)
        assertNull(viewModel.uiState.value.boxOpening)

        viewModel.onIntent(MailboxIntent.ClaimMail(2))
        advanceUntilIdle()
        viewModel.onIntent(MailboxIntent.SkipBoxOpening)
        assertEquals(BoxOpeningPhase.Revealed, viewModel.uiState.value.boxOpening?.phase)
    }

    @Test
    fun `연출 중에는 다른 상자를 열지 않는다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(box(1), box(2)))
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        advanceUntilIdle()
        viewModel.onIntent(MailboxIntent.ClaimMail(2))
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.claimedMailIds)
        assertEquals(1L, viewModel.uiState.value.boxOpening?.mailId)
    }

    @Test
    fun `이미 받았거나 사라진 상자면 무대가 걷힌 뒤 안내하고 목록을 다시 받는다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(box(1)))
        val viewModel = loadedViewModel()
        val requestsBefore = repository.mailRequests.size
        repository.claimOverride = { throw httpException(409, "api.mail.alreadyClaimed") }

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        advanceUntilIdle()

        val failed = viewModel.uiState.value.boxOpening!!
        assertEquals(BoxOpeningPhase.Failed, failed.phase)
        assertEquals(BoxOpeningFailure.AlreadyGone, failed.failure)
        assertTrue(messageHelper.snackBars.isEmpty())
        assertEquals(requestsBefore, repository.mailRequests.size)

        viewModel.onIntent(MailboxIntent.BoxOpeningFailureShown)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.boxOpening)
        assertEquals("이미 받았거나 기한이 지난 우편이에요", messageHelper.snackBars.single().messageText)
        assertEquals(requestsBefore + 1, repository.mailRequests.size)
        assertFalse(viewModel.uiState.value.items.mails.single().isClaimed)
    }

    @Test
    fun `상자를 받지 못하면 무대가 걷힌 뒤 오류를 안내한다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(box(1)))
        val viewModel = loadedViewModel()
        repository.claimOverride = { throw httpException(400) }

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        advanceUntilIdle()
        assertEquals(BoxOpeningFailure.Error, viewModel.uiState.value.boxOpening?.failure)

        viewModel.onIntent(MailboxIntent.BoxOpeningFailureShown)

        assertNull(viewModel.uiState.value.boxOpening)
        assertEquals(IconType.ERROR, messageHelper.snackBars.single().iconType)
        assertTrue(viewModel.uiState.value.items.claimingMailIds.isEmpty())
    }

    @Test
    fun `코인과 아이템 우편은 상자 연출 없이 받는다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(mail(1)))
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.boxOpening)
        assertTrue(viewModel.uiState.value.items.mails.single().isClaimed)
    }

    // region 첫 사용자 튜토리얼

    @Test
    fun `우편함 단계가 없으면 안내하지 않는다`() = runTest(testDispatcher) {
        repository.mailPage = MailPageVO(mails = listOf(box(1)))

        val viewModel = loadedViewModel()

        assertNull(viewModel.uiState.value.tutorial)
    }

    @Test
    fun `받지 않은 상자가 있으면 목록에서 가장 위의 받지 않은 상자의 열기를 가리킨다`() = runTest(testDispatcher) {
        tutorialHelper.pending += TutorialStep.MAILBOX
        repository.mailPage = MailPageVO(mails = listOf(mail(1), box(2).copy(isClaimed = true), box(3), box(4)))

        val viewModel = loadedViewModel()

        assertEquals(MailboxTutorialUiState(MailboxTutorialStage.OpenBox, boxMailId = 3), viewModel.uiState.value.tutorial)
    }

    @Test
    fun `받을 상자가 없으면 우편함을 소개하고 넘기면 우편함 단계를 마치고 뒤로가기를 가리킨다`() = runTest(testDispatcher) {
        tutorialHelper.pending += TutorialStep.MAILBOX
        repository.mailPage = MailPageVO(mails = listOf(mail(1)))
        val viewModel = loadedViewModel()
        assertEquals(MailboxTutorialStage.Intro, viewModel.uiState.value.tutorial?.stage)

        viewModel.onIntent(MailboxIntent.TutorialAdvance)
        advanceUntilIdle()

        assertEquals(MailboxTutorialStage.GoBack, viewModel.uiState.value.tutorial?.stage)
        assertFalse(TutorialStep.MAILBOX in tutorialHelper.pending)
    }

    @Test
    fun `안내대로 상자를 열고 연출을 닫으면 우편함 단계를 마치고 뒤로가기를 가리킨다`() = runTest(testDispatcher) {
        tutorialHelper.pending += TutorialStep.MAILBOX
        repository.mailPage = MailPageVO(mails = listOf(box(1)))
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        viewModel.onIntent(MailboxIntent.TutorialTargetClicked)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.tutorial!!.isWaitingForBox)
        assertTrue(TutorialStep.MAILBOX in tutorialHelper.pending)

        viewModel.onIntent(MailboxIntent.BoxOpeningBurstFinished)
        viewModel.onIntent(MailboxIntent.CloseBoxOpening)
        advanceUntilIdle()

        assertEquals(MailboxTutorialUiState(MailboxTutorialStage.GoBack, isRoomNext = true), viewModel.uiState.value.tutorial)
        assertFalse(TutorialStep.MAILBOX in tutorialHelper.pending)
    }

    @Test
    fun `마이룸 기능이 꺼져 있으면 뒤로가기 안내가 홈으로 돌아가자고 한다`() = runTest(testDispatcher) {
        isMyRoomEnabled = false
        tutorialHelper.pending += TutorialStep.MAILBOX
        repository.mailPage = MailPageVO(mails = listOf(mail(1)))
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.TutorialAdvance)

        assertFalse(viewModel.uiState.value.tutorial!!.isRoomNext)
    }

    @Test
    fun `안내한 상자를 받지 못하면 실패 안내 뒤 같은 상자를 다시 가리킨다`() = runTest(testDispatcher) {
        tutorialHelper.pending += TutorialStep.MAILBOX
        repository.mailPage = MailPageVO(mails = listOf(box(1)))
        val viewModel = loadedViewModel()
        repository.claimOverride = { throw httpException(400) }

        viewModel.onIntent(MailboxIntent.ClaimMail(1))
        viewModel.onIntent(MailboxIntent.TutorialTargetClicked)
        advanceUntilIdle()
        viewModel.onIntent(MailboxIntent.BoxOpeningFailureShown)

        assertEquals(MailboxTutorialUiState(MailboxTutorialStage.OpenBox, boxMailId = 1), viewModel.uiState.value.tutorial)
        assertTrue(TutorialStep.MAILBOX in tutorialHelper.pending)
    }

    @Test
    fun `건너뛰기를 누르면 남은 안내와 함께 확인을 띄우고 계속 배우면 닫는다`() = runTest(testDispatcher) {
        tutorialHelper.pending += TutorialStep.MAILBOX
        repository.mailPage = MailPageVO(mails = listOf(box(1)))
        val viewModel = loadedViewModel()

        viewModel.onIntent(MailboxIntent.TutorialSkip)

        assertEquals(
            listOf(NyummyTutorialLesson.OpenBox, NyummyTutorialLesson.DecorateRoom, NyummyTutorialLesson.CatHealth),
            viewModel.uiState.value.tutorial?.skipConfirm,
        )

        viewModel.onIntent(MailboxIntent.TutorialSkipCancelled)

        assertNull(viewModel.uiState.value.tutorial?.skipConfirm)
        assertEquals(MailboxTutorialStage.OpenBox, viewModel.uiState.value.tutorial?.stage)
    }

    @Test
    fun `건너뛰기를 확정하면 냐미 건강 이야기만 남기고 홈으로 돌아간다`() = runTest(testDispatcher) {
        tutorialHelper.pending += TutorialStep.entries
        repository.mailPage = MailPageVO(mails = listOf(box(1)))
        val viewModel = loadedViewModel()
        viewModel.onIntent(MailboxIntent.TutorialSkip)

        viewModel.onIntent(MailboxIntent.TutorialSkipConfirmed)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.tutorial)
        assertEquals(setOf(TutorialStep.HEALTH_STORY), tutorialHelper.pending)
        assertEquals(1, navigationHelper.backCount)
    }

    // endregion

    private fun TestScope.loadedViewModel(): MailboxViewModel {
        val viewModel = createViewModel()
        viewModel.onIntent(MailboxIntent.ScreenResumed)
        advanceUntilIdle()
        return viewModel
    }

    private fun TestScope.createViewModel(): MailboxViewModel {
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
            tutorialHelper = tutorialHelper,
            remoteConfigHelper = object : RemoteConfigHelper {
                override suspend fun sync() = Unit

                @Suppress("UNCHECKED_CAST")
                override fun <T> get(key: RemoteConfigKey<T>): T =
                    if (key == RemoteConfigKey.MyRoomEnabled) isMyRoomEnabled as T else key.defaultValue
            },
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

    private fun box(id: Long, imageId: Long = 1L) = mail(id).copy(
        category = MailCategory.REWARD_BOX,
        title = "상자 $id",
        imageId = imageId,
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
    var claimAllOverride: suspend () -> RewardVO = { RewardVO.empty }
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
        return claimAllOverride()
    }

    override suspend fun deleteMail(mailId: Long) {
        deletedMailIds += mailId
    }

    override suspend fun restoreMail(mailId: Long) {
        restoredMailIds += mailId
    }
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
    override fun showToast(messageRes: Int) = Unit
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

/** 메모리로 동작하는 튜토리얼 진행 기록. */
private class FakeTutorialHelper : TutorialHelper {
    val pending = mutableSetOf<TutorialStep>()

    override suspend fun pendingSteps(): Set<TutorialStep> = pending.toSet()
    override suspend fun startAfterMeal() {
        pending.clear()
        pending += TutorialStep.entries
    }

    override suspend fun complete(step: TutorialStep) {
        pending -= step
    }

    override suspend fun skipToHealthStory() {
        pending.clear()
        pending += TutorialStep.HEALTH_STORY
    }

    override suspend fun finish() = pending.clear()
    override suspend fun clear() = pending.clear()
    override suspend fun isMealTutorialRequired() = false
    override suspend fun setMealTutorialRequired(required: Boolean) = Unit
}
