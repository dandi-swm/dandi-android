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
import java.io.IOException
import java.time.LocalDateTime

class SubmitTutorialMealUseCaseTest {

    private val messageHelper = SubmitMealUseCaseTest.RecordingMessageHelper()
    private val tutorialHelper = RecordingTutorialHelper()

    private fun useCase(error: Exception? = null) = SubmitTutorialMealUseCase(
        repository = FakeRepository(error),
        tutorialHelper = tutorialHelper,
        resourceHelper = SubmitMealUseCaseTest.FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = SubmitMealUseCaseTest.FakeNavigationHelper(),
        ttiHelper = SubmitMealUseCaseTest.FakeTTIHelper(),
    )

    @Test
    fun `접수되면 골드 상자로 연출하고 식사 튜토리얼을 마친 뒤 뒤 단계를 시작한다`() = runBlocking {
        val result = useCase()().getOrThrow()

        assertEquals(TutorialMealResult.Accepted(CreatedMealVO(rewardBoxGrade = RewardBoxGrade.GOLD)), result)
        assertFalse(tutorialHelper.mealRequired)
        assertTrue(tutorialHelper.started)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `이미 했다고 하면 안내 없이 식사 튜토리얼만 마친다`() = runBlocking {
        val result = useCase(httpException(409, MealErrorType.TUTORIAL_ALREADY_COMPLETED.type))().getOrThrow()

        assertEquals(TutorialMealResult.AlreadyCompleted, result)
        assertFalse(tutorialHelper.mealRequired)
        assertFalse(tutorialHelper.started)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `서버 오류면 다시 시도 안내를 다이얼로그로 띄우고 실패를 돌려준다`() = runBlocking {
        val result = useCase(httpException(500))()

        assertTrue(result.isFailure)
        assertEquals(1, messageHelper.dialogs.size)
        assertTrue(tutorialHelper.mealRequired)
        assertFalse(tutorialHelper.started)
    }

    @Test
    fun `네트워크 오류면 다시 시도 안내를 다이얼로그로 띄우고 실패를 돌려준다`() = runBlocking {
        val result = useCase(IOException("network"))()

        assertTrue(result.isFailure)
        assertEquals(1, messageHelper.dialogs.size)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    private fun httpException(code: Int, errorCode: String? = null) = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/api/v1/meals/tutorial",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )

    private class FakeRepository(private val error: Exception?) : MealRecordRepository {
        override suspend fun readGalleryPhotoTakenAt(photoUri: String): LocalDateTime? = null
        override suspend fun importGalleryPhoto(photoUri: String) = photoUri
        override suspend fun prepareUploadImage(photoPath: String, source: MealPhotoSource) = Unit
        override suspend fun issueImageUploadUrl(photoPath: String) = MealImageUploadVO()
        override suspend fun uploadImage(uploadTarget: MealImageUploadVO, photoPath: String) = Unit
        override suspend fun createMeal(imageKey: String): CreatedMealVO = CreatedMealVO.empty
        override suspend fun createTutorialMeal() {
            error?.let { throw it }
        }
    }

    private class RecordingTutorialHelper : TutorialHelper {
        var mealRequired = true
        var started = false
        override suspend fun pendingSteps(): Set<TutorialStep> = emptySet()
        override suspend fun startAfterMeal() {
            started = true
        }
        override suspend fun complete(step: TutorialStep) = Unit
        override suspend fun skipToHealthStory() = Unit
        override suspend fun finish() = Unit
        override suspend fun clear() = Unit
        override suspend fun isMealTutorialRequired(): Boolean = mealRequired
        override suspend fun setMealTutorialRequired(required: Boolean) {
            mealRequired = required
        }
    }
}
