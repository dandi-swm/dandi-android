package com.dandi.nyummy.meal.domain

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
import com.dandi.nyummy.common.entity.time.KstDateTime
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.MealImageUploadVO
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitMealUseCaseTest {

    private val messageHelper = RecordingMessageHelper()

    private fun useCase(createMealError: Exception) = SubmitMealUseCase(
        repository = FakeMealRecordRepository(createMealError),
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = FakeNavigationHelper(),
        ttiHelper = FakeTTIHelper(),
    )

    @Test
    fun `오래된 사진 code 는 사진에 맞는 문구로 안내한다`() = runBlocking {
        val result = useCase(httpException(400, MealErrorType.STALE_IMAGE.type))(PHOTO_PATH, MealPhotoSource.CAMERA)

        assertTrue(result.isFailure)
        assertEquals(MealErrorType.STALE_IMAGE.errorMsg, messageHelper.snackBars.single().messageText)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `S3 용량 초과 code 도 사진 문구로 안내한다`() = runBlocking {
        useCase(httpException(400, "api.s3.fileSizeExceeded"))(PHOTO_PATH, MealPhotoSource.CAMERA)

        assertEquals("사진 용량이 너무 커요. 다시 찍어주세요", messageHelper.snackBars.single().messageText)
    }

    @Test
    fun `모르는 code 의 400 은 기록 실패 문구로 안내한다`() = runBlocking {
        useCase(httpException(400, "api.s3.invalidKey"))(PHOTO_PATH, MealPhotoSource.CAMERA)

        assertEquals("식사를 기록하지 못했어요. 다시 시도해주세요", messageHelper.snackBars.single().messageText)
    }

    @Test
    fun `code 없는 500 은 공통 오류 다이얼로그로 안내한다`() = runBlocking {
        useCase(httpException(500))(PHOTO_PATH, MealPhotoSource.CAMERA)

        assertTrue(messageHelper.snackBars.isEmpty())
        assertEquals("잠시 후 다시 시도해 주세요.", messageHelper.dialogs.single())
    }

    /** 서버 에러 바디의 `code` 를 cause 로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
    private fun httpException(code: Int, errorCode: String? = null) = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/api/v1/meals",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )

    private class FakeMealRecordRepository(
        private val createMealError: Exception,
    ) : MealRecordRepository {
        override suspend fun readGalleryPhotoTakenAt(photoUri: String): KstDateTime? = null
        override suspend fun importGalleryPhoto(photoUri: String) = photoUri
        override suspend fun prepareUploadImage(photoPath: String, source: MealPhotoSource) = Unit
        override suspend fun issueImageUploadUrl(photoPath: String) = MealImageUploadVO()
        override suspend fun uploadImage(uploadTarget: MealImageUploadVO, photoPath: String) = Unit
        override suspend fun createMeal(imageKey: String): CreatedMealVO = throw createMealError
    }

    private class FakeNavigationHelper : NavigationHelper {
        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) = Unit
        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() = Unit
        override fun navigateToAsRoot(page: Page) = Unit
        override fun navigateToInitial() = Unit
        override fun navigateToExternalLink(url: String) = Unit
    }

    private class FakeResourceHelper : ResourceHelper {
        override fun getString(resource: StringResource): String = resource.name
    }

    private class RecordingMessageHelper : MessageHelper {
        data class SnackBarCall(val iconType: IconType, val messageText: String)

        val snackBars = mutableListOf<SnackBarCall>()
        val dialogs = mutableListOf<String>()

        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showSnackBar(
            iconType: IconType,
            messageText: String,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) {
            snackBars += SnackBarCall(iconType, messageText)
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
            dialogs += descText
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

    private class FakeTTIHelper : TTIHelper {
        override fun startTTITracking(page: TTIPage) = Unit
        override fun startTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITracking() = Unit
        override fun shotTTILogging() = Unit
        override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
    }

    private companion object {
        const val PHOTO_PATH = "/cache/meal_1.jpeg"
    }
}
