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

class SaveMealTimesUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()
    private var retryCount = 0

    private val useCase = SaveMealTimesUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper,
    )

    private suspend fun save(mealTimes: MealTimesVO) = useCase(mealTimes, onRetry = { retryCount++ })

    @Test
    fun `서버에 저장하고 아무것도 띄우지 않는다`() = runBlocking {
        val mealTimes = MealTimesVO(lunch = MealTimeVO(hour = 13))

        val result = save(mealTimes)

        assertTrue(result.isSuccess)
        assertEquals(listOf(mealTimes), repository.savedMealTimes)
        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `끼니끼리 시각이 겹치면 서버에 보내지 않고 안내한다`() = runBlocking {
        val result = save(MealTimesVO(breakfast = MealTimeVO(hour = 12), lunch = MealTimeVO(hour = 12)))

        assertTrue(result.isFailure)
        assertTrue(repository.savedMealTimes.isEmpty())
        val snackBar = messageHelper.snackBars.single()
        assertEquals("끼니마다 다른 시각을 골라주세요.", snackBar.messageText)
        assertNull(snackBar.callToActionText)
    }

    @Test
    fun `안 먹는 끼니는 시각이 겹쳐도 저장한다`() = runBlocking {
        val mealTimes = MealTimesVO(breakfast = MealTimeVO(hour = 12, isSkipped = true), lunch = MealTimeVO(hour = 12))

        val result = save(mealTimes)

        assertTrue(result.isSuccess)
        assertEquals(listOf(mealTimes), repository.savedMealTimes)
    }

    @Test
    fun `서버가 입력값을 거절하면 다시 시도 없이 겹치는 시각 안내를 한다`() = runBlocking {
        repository.saveMealTimesError = httpException(400, "api.common.invalidInputValue")

        val result = save(MealTimesVO.default)

        assertTrue(result.isFailure)
        val snackBar = messageHelper.snackBars.single()
        assertEquals("끼니마다 다른 시각을 골라주세요.", snackBar.messageText)
        assertNull(snackBar.callToActionText)
    }

    @Test
    fun `네트워크 오류면 저장 실패를 안내하고 다시 시도를 화면에 맡긴다`() = runBlocking {
        repository.saveMealTimesError = IOException("offline")

        val result = save(MealTimesVO.default)

        assertTrue(result.isFailure)
        val snackBar = messageHelper.snackBars.single()
        assertEquals("식사 시간을 저장하지 못했어요", snackBar.messageText)
        assertEquals("다시 시도", snackBar.callToActionText)
        snackBar.onClickCTA?.invoke()
        assertEquals(1, retryCount)
    }

    @Test
    fun `모르는 code 의 4xx 도 저장 실패와 다시 시도로 안내한다`() = runBlocking {
        repository.saveMealTimesError = httpException(409, "api.user.somethingNew")

        save(MealTimesVO.default)

        val snackBar = messageHelper.snackBars.single()
        assertEquals("식사 시간을 저장하지 못했어요", snackBar.messageText)
        assertEquals("다시 시도", snackBar.callToActionText)
        snackBar.onClickCTA?.invoke()
        assertEquals(1, retryCount)
    }

    @Test
    fun `로그인이 만료됐으면 세션 만료 다이얼로그만 띄운다`() = runBlocking {
        repository.saveMealTimesError = httpException(401, "api.auth.tokenExpired")

        val result = save(MealTimesVO.default)

        assertTrue(result.isFailure)
        assertTrue(messageHelper.snackBars.isEmpty())
        val dialog = messageHelper.dialogs.single()
        assertTrue(dialog.cantIgnore)
        dialog.onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
    }

    @Test(expected = CancellationException::class)
    fun `취소는 삼키지 않고 그대로 던진다`(): Unit = runBlocking {
        repository.saveMealTimesError = CancellationException("left screen")
        save(MealTimesVO.default)
        Unit
    }
}
