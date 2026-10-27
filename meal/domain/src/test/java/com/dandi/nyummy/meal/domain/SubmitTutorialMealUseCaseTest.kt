package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.MealImageUploadVO
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SubmitTutorialMealUseCaseTest {

    private val messageHelper = SubmitMealUseCaseTest.RecordingMessageHelper()
    private val tutorialHelper = FakeTutorialHelper()

    private fun useCase(create: () -> CreatedMealVO) = SubmitTutorialMealUseCase(
        repository = FakeRepository(create),
        tutorialHelper = tutorialHelper,
        resourceHelper = SubmitMealUseCaseTest.FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = SubmitMealUseCaseTest.FakeNavigationHelper(),
        ttiHelper = SubmitMealUseCaseTest.FakeTTIHelper(),
    )

    @Test
    fun `접수되면 첫 식사 튜토리얼을 마치고 뒤 안내를 시작한다`() = runBlocking {
        val meal = CreatedMealVO(mealId = 7, rewardBoxGrade = RewardBoxGrade.GOLD)

        val result = useCase { meal }()

        assertEquals(TutorialMealResult.Accepted(meal), result.getOrNull())
        assertFalse(tutorialHelper.mealRequired)
        assertTrue(tutorialHelper.started)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `이미 했다고 하면 안내 없이 첫 식사 튜토리얼만 마친다`() = runBlocking {
        val result = useCase { throw httpException(409, MealErrorType.TUTORIAL_ALREADY_COMPLETED.type) }()

        assertEquals(TutorialMealResult.AlreadyCompleted, result.getOrNull())
        assertFalse(tutorialHelper.mealRequired)
        assertFalse(tutorialHelper.started)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `기록하지 못하면 다시 시도하라는 다이얼로그를 띄우고 튜토리얼 상태는 그대로 둔다`() = runBlocking {
        val result = useCase { throw java.io.IOException("network") }()

        assertTrue(result.isFailure)
        assertEquals(RETRY_MESSAGE, messageHelper.dialogs.single())
        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(tutorialHelper.mealRequired)
        assertFalse(tutorialHelper.started)
    }

    @Test
    fun `서버 오류도 사유와 상관없이 같은 다이얼로그로 안내한다`() = runBlocking {
        useCase { throw httpException(404, "") }()
        useCase { throw httpException(500, "") }()
        useCase { throw httpException(409, MealErrorType.DAILY_COUNT_EXCEEDED.type) }()

        assertEquals(List(3) { RETRY_MESSAGE }, messageHelper.dialogs)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    private companion object {
        const val RETRY_MESSAGE = "잠시 네트워크 환경이 불안정합니다. 잠시 후 다시 시도해주세요"
    }

    private fun httpException(code: Int, errorCode: String) = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/api/v1/meals/tutorial",
        msg = "Http Request Failed ($code)",
        cause = Throwable(errorCode),
    )

    private class FakeRepository(private val create: () -> CreatedMealVO) : MealRecordRepository {
        override suspend fun readGalleryPhotoTakenAt(photoUri: String): LocalDateTime? = null
        override suspend fun importGalleryPhoto(photoUri: String) = photoUri
        override suspend fun prepareUploadImage(photoPath: String, source: MealPhotoSource) = Unit
        override suspend fun issueImageUploadUrl(photoPath: String) = MealImageUploadVO()
        override suspend fun uploadImage(uploadTarget: MealImageUploadVO, photoPath: String) = Unit
        override suspend fun createMeal(imageKey: String) = error("튜토리얼은 사진으로 기록하지 않는다")
        override suspend fun createTutorialMeal() = create()
    }

    private class FakeTutorialHelper : TutorialHelper {
        var mealRequired = true
        var started = false
        override suspend fun isPending(step: TutorialStep) = false
        override suspend fun complete(step: TutorialStep) = Unit
        override suspend fun start() {
            started = true
        }
        override suspend fun skipAll() = Unit
        override suspend fun clear() = Unit
        override suspend fun isMealTutorialRequired() = mealRequired
        override suspend fun setMealTutorialRequired(required: Boolean) {
            mealRequired = required
        }
    }
}
