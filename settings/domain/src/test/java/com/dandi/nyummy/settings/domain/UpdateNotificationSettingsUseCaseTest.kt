package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class UpdateNotificationSettingsUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    private val useCase = UpdateNotificationSettingsUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper,
    )

    @Test
    fun `바꾸는 항목만 저장소에 넘긴다`() = runBlocking {
        val change = NotificationSettingsChange(isMarketingEnabled = true)

        val result = useCase(change)

        assertTrue(result.isSuccess)
        assertEquals(listOf(change), repository.notificationChanges)
    }

    @Test
    fun `로그인이 만료됐으면 세션 만료 다이얼로그를 띄운다`() = runBlocking {
        repository.updateNotificationSettingsError = httpException(401, "api.auth.tokenExpired")

        val result = useCase(NotificationSettingsChange(isServiceEnabled = false))

        assertTrue(result.isFailure)
        messageHelper.dialogs.single().onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
    }

    @Test
    fun `네트워크 오류는 안내 없이 실패만 돌려준다 다시 시도는 화면이 띄운다`() = runBlocking {
        repository.updateNotificationSettingsError = IOException("offline")

        val result = useCase(NotificationSettingsChange(isServiceEnabled = false))

        assertTrue(result.isFailure)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test(expected = CancellationException::class)
    fun `취소는 삼키지 않고 그대로 던진다`(): Unit = runBlocking {
        repository.updateNotificationSettingsError = CancellationException("left screen")
        useCase(NotificationSettingsChange(isServiceEnabled = false))
        Unit
    }
}
