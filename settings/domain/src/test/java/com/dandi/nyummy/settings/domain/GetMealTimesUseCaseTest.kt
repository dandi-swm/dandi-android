package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class GetMealTimesUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()
    private var retryCount = 0

    private val useCase = GetMealTimesUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper,
    )

    private suspend fun load() = useCase(onRetry = { retryCount++ })

    @Test
    fun `서버에 저장된 식사 시각을 돌려준다`() = runBlocking {
        val mealTimes = MealTimesVO(dinner = MealTimeVO(hour = 19))
        repository.mealTimes = mealTimes

        assertEquals(Result.success(mealTimes), load())
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `네트워크 오류면 불러오기 실패를 안내하고 다시 시도를 화면에 맡긴다`() = runBlocking {
        repository.getMealTimesError = IOException("offline")

        val result = load()

        assertTrue(result.isFailure)
        val snackBar = messageHelper.snackBars.single()
        assertEquals("식사 시간을 불러오지 못했어요", snackBar.messageText)
        assertEquals("다시 시도", snackBar.callToActionText)
        snackBar.onClickCTA?.invoke()
        assertEquals(1, retryCount)
    }

    @Test
    fun `회원이 없으면 공통 404 안내 대신 회원 정보 안내를 한다`() = runBlocking {
        repository.getMealTimesError = httpException(404, "api.user.notFound")

        val result = load()

        assertTrue(result.isFailure)
        val snackBar = messageHelper.snackBars.single()
        assertEquals("회원 정보를 찾지 못했어요. 다시 로그인해 주세요.", snackBar.messageText)
        assertNull(snackBar.callToActionText)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertEquals(0, navigationHelper.backCount)
    }

    @Test
    fun `로그인이 만료됐으면 세션 만료 다이얼로그만 띄운다`() = runBlocking {
        repository.getMealTimesError = httpException(401)

        load()

        assertTrue(messageHelper.snackBars.isEmpty())
        messageHelper.dialogs.single().onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
    }

    @Test(expected = CancellationException::class)
    fun `취소는 삼키지 않고 그대로 던진다`(): Unit = runBlocking {
        repository.getMealTimesError = CancellationException("left screen")
        load()
        Unit
    }
}
