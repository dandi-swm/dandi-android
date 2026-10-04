package com.dandi.nyummy.meal.domain

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
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.MealImageUploadVO
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

class ImportGalleryMealPhotoUseCaseTest {

    private val messageHelper = RecordingMessageHelper()

    /** 테스트 기준 "지금": 2026-09-30 13:00 KST. */
    private val nowMillis = KstTime.epochMillisOf(2026, 9, 30, 13, 0)

    @Test
    fun `오늘 찍은 사진이면 캐시로 가져온 파일 경로를 돌려준다`() = runBlocking {
        val repository = FakeMealRecordRepository(takenAt = KstDateTime(2026, 9, 30, 0, 0, 1))

        val result = useCase(repository)(PHOTO_URI, nowMillis)

        assertEquals(Result.success(IMPORTED_PATH), result)
        assertEquals(listOf(PHOTO_URI), repository.importedUris)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `어제 찍은 사진이면 안내 스낵바를 띄우고 가져오지 않는다`() = runBlocking {
        val repository = FakeMealRecordRepository(takenAt = KstDateTime(2026, 9, 29, 23, 59, 59))

        val result = useCase(repository)(PHOTO_URI, nowMillis)

        assertTrue(result.isFailure)
        assertTrue(repository.importedUris.isEmpty())
        val snackBar = messageHelper.snackBars.single()
        assertEquals(IconType.ERROR, snackBar.iconType)
        assertEquals(StringResource.MEAL_GALLERY_NOT_TAKEN_TODAY.name, snackBar.messageText)
    }

    @Test
    fun `촬영 시각이 미래 날짜여도 오늘이 아니면 거절한다`() = runBlocking {
        val repository = FakeMealRecordRepository(takenAt = KstDateTime(2026, 10, 1, 0, 0, 0))

        val result = useCase(repository)(PHOTO_URI, nowMillis)

        assertTrue(result.isFailure)
        assertTrue(repository.importedUris.isEmpty())
        assertEquals(
            StringResource.MEAL_GALLERY_NOT_TAKEN_TODAY.name,
            messageHelper.snackBars.single().messageText,
        )
    }

    @Test
    fun `촬영 시각 정보가 없으면 확인 불가 안내를 띄우고 가져오지 않는다`() = runBlocking {
        val repository = FakeMealRecordRepository(takenAt = null)

        val result = useCase(repository)(PHOTO_URI, nowMillis)

        assertTrue(result.isFailure)
        assertTrue(repository.importedUris.isEmpty())
        assertEquals(
            StringResource.MEAL_GALLERY_UNKNOWN_TAKEN_DATE.name,
            messageHelper.snackBars.single().messageText,
        )
    }

    @Test
    fun `사진을 불러오지 못하면 다시 선택 안내를 띄운다`() = runBlocking {
        val repository = FakeMealRecordRepository(
            takenAt = KstDateTime(2026, 9, 30, 12, 0, 0),
            importFailure = MealGalleryPhotoLoadException(),
        )

        val result = useCase(repository)(PHOTO_URI, nowMillis)

        assertTrue(result.isFailure)
        assertEquals(
            StringResource.MEAL_GALLERY_LOAD_FAILED.name,
            messageHelper.snackBars.single().messageText,
        )
    }

    @Test
    fun `예상치 못한 오류로 실패하면 다시 시도 안내를 띄운다`() = runBlocking {
        val repository = FakeMealRecordRepository(
            takenAt = KstDateTime(2026, 9, 30, 12, 0, 0),
            importFailure = IllegalArgumentException(),
        )

        val result = useCase(repository)(PHOTO_URI, nowMillis)

        assertTrue(result.isFailure)
        assertEquals(
            StringResource.MEAL_GALLERY_IMPORT_FAILED.name,
            messageHelper.snackBars.single().messageText,
        )
    }

    private fun useCase(repository: MealRecordRepository) = ImportGalleryMealPhotoUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = FakeNavigationHelper(),
        ttiHelper = FakeTTIHelper(),
    )

    private class FakeMealRecordRepository(
        private val takenAt: KstDateTime?,
        private val importFailure: Exception? = null,
    ) : MealRecordRepository {
        val importedUris = mutableListOf<String>()

        override suspend fun readGalleryPhotoTakenAt(photoUri: String) = takenAt
        override suspend fun importGalleryPhoto(photoUri: String): String {
            importFailure?.let { throw it }
            importedUris += photoUri
            return IMPORTED_PATH
        }

        override suspend fun prepareUploadImage(photoPath: String) = Unit
        override suspend fun issueImageUploadUrl(photoPath: String) = MealImageUploadVO()
        override suspend fun uploadImage(uploadTarget: MealImageUploadVO, photoPath: String) = Unit
        override suspend fun createMeal(imageKey: String) = CreatedMealVO()
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
        ) = Unit

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
        override fun startTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) = Unit
        override fun endTTITracking(page: TTIPage) = Unit
        override fun shotTTILogging(page: TTIPage) = Unit
        override fun addTTIMetaData(page: TTIPage, metadata: TTIMetaData, value: Any?) = Unit
    }

    private companion object {
        const val PHOTO_URI = "content://media/picker/0/com.android.providers.media.photopicker/media/1"
        const val IMPORTED_PATH = "/cache/meal_gallery_1.jpeg"
    }
}
