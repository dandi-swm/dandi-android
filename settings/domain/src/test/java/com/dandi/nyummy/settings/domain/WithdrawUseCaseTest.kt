package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class WithdrawUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    /** 푸시 토큰을 지운 시점에 첫 화면으로 몇 번 갔는지 기록한다. 지우기가 이동보다 먼저인지 보려는 것이다. */
    private val pushTokenHelper = object : PushTokenHelper {
        val initialCountsAtDelete = mutableListOf<Int>()
        override fun register(token: String?) = Unit
        override suspend fun deleteToken() {
            initialCountsAtDelete += navigationHelper.initialCount
        }
    }

    /** 튜토리얼 진행 상태를 지운 시점에 첫 화면으로 몇 번 갔는지 기록한다. */
    private val tutorialHelper = object : TutorialHelper {
        val initialCountsAtClear = mutableListOf<Int>()
        override suspend fun isPending(step: TutorialStep) = false
        override suspend fun complete(step: TutorialStep) = Unit
        override suspend fun start() = Unit
        override suspend fun skipAll() = Unit
        override suspend fun clear() {
            initialCountsAtClear += navigationHelper.initialCount
        }
    }

    private val useCase = WithdrawUseCase(
        repository = repository,
        pushTokenHelper = pushTokenHelper,
        tutorialHelper = tutorialHelper,
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper,
    )

    @Test
    fun `탈퇴하면 푸시 토큰을 지운 뒤 인사하고 첫 화면으로 간다`() = runBlocking {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, repository.withdrawCount)
        assertEquals(listOf(0), pushTokenHelper.initialCountsAtDelete)
        assertEquals(listOf(0), tutorialHelper.initialCountsAtClear)
        assertEquals(1, navigationHelper.initialCount)
        val snackBar = messageHelper.snackBars.single()
        assertEquals(IconType.SUCCESS, snackBar.iconType)
        assertEquals(WithdrawUseCase.WITHDRAWN_MESSAGE, snackBar.messageText)
    }

    @Test
    fun `네트워크 오류면 회원은 그대로라 토큰을 두고 머무르며 실패를 알린다`() = runBlocking {
        repository.withdrawError = IOException("offline")

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(pushTokenHelper.initialCountsAtDelete.isEmpty())
        assertTrue(tutorialHelper.initialCountsAtClear.isEmpty())
        assertEquals(0, navigationHelper.initialCount)
        val snackBar = messageHelper.snackBars.single()
        assertEquals(IconType.ERROR, snackBar.iconType)
        assertEquals(WithdrawUseCase.WITHDRAW_FAILED_MESSAGE, snackBar.messageText)
    }

    @Test
    fun `회원 정보가 없다는 응답이면 그 안내를 띄운다`() = runBlocking {
        repository.withdrawError = httpException(404, "api.user.notFound")

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(SettingsErrorType.USER_NOT_FOUND.errorMsg, messageHelper.snackBars.single().messageText)
        assertTrue(pushTokenHelper.initialCountsAtDelete.isEmpty())
    }

    @Test
    fun `로그인이 만료됐으면 탈퇴 실패 대신 세션 만료 다이얼로그를 띄운다`() = runBlocking {
        repository.withdrawError = httpException(401, "api.auth.tokenExpired")

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(messageHelper.snackBars.isEmpty())
        messageHelper.dialogs.single().onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
        assertTrue(pushTokenHelper.initialCountsAtDelete.isEmpty())
    }

    @Test(expected = CancellationException::class)
    fun `취소는 삼키지 않고 그대로 던진다`(): Unit = runBlocking {
        repository.withdrawError = CancellationException("left")
        useCase()
    }
}
