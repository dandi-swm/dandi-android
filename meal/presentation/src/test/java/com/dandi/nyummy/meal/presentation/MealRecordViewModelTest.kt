package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.meal.domain.ImportGalleryMealPhotoUseCase
import com.dandi.nyummy.meal.domain.MealRecordRepository
import com.dandi.nyummy.meal.domain.SubmitMealUseCase
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.MealImageUploadVO
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import com.dandi.nyummy.room.domain.GetRoomBackgroundUseCase
import com.dandi.nyummy.room.domain.RoomRepository
import com.dandi.nyummy.room.entity.OwnedRoomItemPageVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.room.entity.RoomVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class MealRecordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mealRepository = FakeMealRecordRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val roomRepository = FakeRoomRepository()
    private val analytics = RecordingAnalyticsHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `기록이 접수되면 상자 등급과 분석 대기 여부가 연출 상태에 실린다`() = runTest(testDispatcher) {
        mealRepository.created = CreatedMealVO(mealId = 1, status = "ANALYZING", rewardBoxGrade = RewardBoxGrade.GOLD)
        val viewModel = createViewModel()

        submit(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.phase is MealCameraPhase.Ceremony)
        assertEquals(MealCeremonyResult(RewardBoxGrade.GOLD, isAnalysisPending = true), state.ceremonyResult)
        assertTrue(state.isSubmitSucceeded)
    }

    @Test
    fun `우편함 플래그는 원격 구성의 우편함 아이템 탭 값을 따른다`() = runTest(testDispatcher) {
        assertTrue(createViewModel(isMailGiftEnabled = true).uiState.value.isMailGiftEnabled)
        assertFalse(createViewModel(isMailGiftEnabled = false).uiState.value.isMailGiftEnabled)
    }

    @Test
    fun `분석이 끝난 응답이면 분석 대기가 아니다`() = runTest(testDispatcher) {
        mealRepository.created = CreatedMealVO(mealId = 1, status = "COMPLETED", rewardBoxGrade = RewardBoxGrade.BRONZE)
        val viewModel = createViewModel()

        submit(viewModel)

        assertFalse(viewModel.uiState.value.ceremonyResult!!.isAnalysisPending)
    }

    @Test
    fun `제출에 실패하면 연출 화면에서 실패만 알리고 다 알린 뒤 홈으로 간다`() = runTest(testDispatcher) {
        mealRepository.createError = IOException("network")
        val viewModel = createViewModel(isMailGiftEnabled = true)

        submit(viewModel)

        var state = viewModel.uiState.value
        assertTrue(state.phase is MealCameraPhase.Ceremony)
        assertTrue(state.isSubmitFailed)
        assertNull(state.ceremonyResult)

        viewModel.onIntent(MealRecordIntent.CeremonyFailureShown)

        state = viewModel.uiState.value
        assertTrue(state.isFinishing)
        assertEquals(listOf("back"), navigationHelper.calls)
    }

    @Test
    fun `실패한 뒤에는 닫기로 바로 홈으로 가고 실패 알림이 끝나도 한 번만 뒤로 간다`() = runTest(testDispatcher) {
        mealRepository.createError = IOException("network")
        val viewModel = createViewModel()
        submit(viewModel)

        viewModel.onIntent(MealRecordIntent.ClickClose)
        viewModel.onIntent(MealRecordIntent.CeremonyFailureShown)

        assertEquals(listOf("back"), navigationHelper.calls)
    }

    @Test
    fun `실패하지 않았는데 실패 동작 완료가 오면 무시한다`() = runTest(testDispatcher) {
        mealRepository.created = CreatedMealVO(mealId = 1, status = "COMPLETED")
        val viewModel = createViewModel()
        submit(viewModel)

        viewModel.onIntent(MealRecordIntent.CeremonyFailureShown)

        assertTrue(viewModel.uiState.value.phase is MealCameraPhase.Ceremony)
    }

    @Test
    fun `접수되기 전에는 닫기와 완료로 화면을 닫을 수 없다`() = runTest(testDispatcher) {
        mealRepository.blockCreate = true
        val viewModel = createViewModel()
        capture(viewModel)
        viewModel.onIntent(MealRecordIntent.ClickSubmit)
        advanceUntilIdle()

        viewModel.onIntent(MealRecordIntent.ClickClose)
        viewModel.onIntent(MealRecordIntent.ClickDone)

        assertTrue(navigationHelper.calls.isEmpty())
        assertFalse(viewModel.uiState.value.isFinishing)
    }

    @Test
    fun `완료를 여러 번 눌러도 한 번만 뒤로 간다`() = runTest(testDispatcher) {
        mealRepository.created = CreatedMealVO(mealId = 1, status = "COMPLETED")
        val viewModel = createViewModel()
        submit(viewModel)

        viewModel.onIntent(MealRecordIntent.ClickDone)
        viewModel.onIntent(MealRecordIntent.ClickDone)
        viewModel.onIntent(MealRecordIntent.ClickClose)

        assertEquals(listOf("back"), navigationHelper.calls)
    }

    @Test
    fun `연출의 고양이방은 지금 마이룸에 깔린 배경으로 그린다`() = runTest(testDispatcher) {
        roomRepository.room = RoomVO(
            placements = listOf(
                RoomPlacementVO(roomItemId = 10, category = RoomItemCategory.BED, itemId = 2),
                RoomPlacementVO(roomItemId = 11, category = RoomItemCategory.BACKGROUND, itemId = 7),
            ),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(7L, viewModel.uiState.value.roomBackgroundItemId)
    }

    @Test
    fun `마이룸을 읽지 못하면 안내 없이 기본 빈 방으로 둔다`() = runTest(testDispatcher) {
        roomRepository.error = IOException("network")
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.roomBackgroundItemId)
        assertTrue(navigationHelper.calls.isEmpty())
    }

    @Test
    fun `들어와서 찍고 내서 저장되면 퍼널을 차례로 남기고 걸린 초를 붙인다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        submit(viewModel)
        clear(viewModel)

        assertEquals(
            listOf("meal_record_open", "meal_photo_ready", "meal_submit", "meal_submit_success"),
            mealEvents().map { it.name },
        )
        assertEquals("camera", mealEvents()[1].params["source"])
        val success = mealEvents().last()
        assertEquals("camera", success.params["source"])
        assertTrue(success.numberParams.getValue("duration_sec") >= 0)
    }

    @Test
    fun `저장하지 않고 떠나면 떠난 단계를 남긴다`() = runTest(testDispatcher) {
        val atCamera = createViewModel()
        clear(atCamera)
        val atConfirm = createViewModel()
        capture(atConfirm)
        clear(atConfirm)
        mealRepository.createError = IOException("network")
        val afterFailure = createViewModel()
        submit(afterFailure)
        clear(afterFailure)

        assertEquals(
            listOf("camera", "confirm", "failed"),
            mealEvents().filter { it.name == "meal_record_exit" }.map { it.params.getValue("stage") },
        )
    }

    @Test
    fun `다시 찍기를 남긴다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        capture(viewModel)
        viewModel.onIntent(MealRecordIntent.ClickRetake)

        assertEquals(mapOf("source" to "camera"), mealEvents().single { it.name == "meal_retake" }.params)
    }

    private fun mealEvents(): List<AnalyticsEvent> = analytics.events.filter { it.name.startsWith("meal_") }

    /** 화면을 떠날 때처럼 ViewModel을 정리한다. */
    private fun clear(viewModel: MealRecordViewModel) {
        val store = ViewModelStore()
        ViewModelProvider.create(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = viewModel as T
            },
        )[MealRecordViewModel::class]
        store.clear()
    }

    private fun capture(viewModel: MealRecordViewModel) {
        viewModel.onIntent(MealRecordIntent.PhotoCaptured(PHOTO_PATH))
    }

    private fun TestScope.submit(viewModel: MealRecordViewModel) {
        capture(viewModel)
        viewModel.onIntent(MealRecordIntent.ClickSubmit)
        advanceUntilIdle()
    }

    private fun createViewModel(isMailGiftEnabled: Boolean = false): MealRecordViewModel {
        val messageHelper = SilentMessageHelper()
        return MealRecordViewModel(
            submitMeal = SubmitMealUseCase(
                repository = mealRepository,
                analyticsHelper = RecordingAnalyticsHelper(),
                resourceHelper = FakeResourceHelper(),
                messageHelper = messageHelper,
                navigationHelper = navigationHelper,
                ttiHelper = FakeTTIHelper(),
            ),
            importGalleryMealPhoto = ImportGalleryMealPhotoUseCase(
                repository = mealRepository,
                analyticsHelper = RecordingAnalyticsHelper(),
                resourceHelper = FakeResourceHelper(),
                messageHelper = messageHelper,
                navigationHelper = navigationHelper,
                ttiHelper = FakeTTIHelper(),
            ),
            navigationHelper = navigationHelper,
            messageHelper = messageHelper,
            analyticsHelper = analytics,
            remoteConfigHelper = FakeRemoteConfigHelper(isMailGiftEnabled),
            getRoomBackground = GetRoomBackgroundUseCase(roomRepository),
        )
    }

    private class FakeRoomRepository : RoomRepository {
        var room: RoomVO = RoomVO()
        var error: Exception? = null

        override suspend fun getRoom(size: Int): RoomVO = error?.let { throw it } ?: room
        override suspend fun getOwnedItems(category: RoomItemCategory, cursor: Long?, size: Int) = OwnedRoomItemPageVO()
        override suspend fun saveRoom(placements: List<RoomPlacementVO>) = Unit
    }

    private class FakeMealRecordRepository : MealRecordRepository {
        var created: CreatedMealVO = CreatedMealVO.empty
        var createError: Exception? = null
        var blockCreate = false

        override suspend fun readGalleryPhotoTakenAt(photoUri: String): LocalDateTime? = null
        override suspend fun importGalleryPhoto(photoUri: String) = photoUri
        override suspend fun prepareUploadImage(photoPath: String, source: MealPhotoSource) = Unit
        override suspend fun issueImageUploadUrl(photoPath: String): MealImageUploadVO = MealImageUploadVO()
        override suspend fun uploadImage(uploadTarget: MealImageUploadVO, photoPath: String) = Unit
        override suspend fun createMeal(imageKey: String): CreatedMealVO {
            if (blockCreate) kotlinx.coroutines.awaitCancellation()
            createError?.let { throw it }
            return created
        }
    }

    private class FakeRemoteConfigHelper(private val isMailGiftEnabled: Boolean) : RemoteConfigHelper {
        override suspend fun sync() = Unit

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: RemoteConfigKey<T>): T =
            if (key == RemoteConfigKey.MailItemTabEnabled) isMailGiftEnabled as T else key.defaultValue
    }

    private class RecordingNavigationHelper : NavigationHelper {
        val calls = mutableListOf<String>()

        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) {
            calls += "to:${page.toRoute().path}"
        }
        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() {
            calls += "back"
        }
        override fun navigateToAsRoot(page: Page) = Unit
        override fun navigateToInitial() = Unit
        override fun navigateToExternalLink(url: String) = Unit
    }

    private class FakeResourceHelper : ResourceHelper {
        override fun getString(resource: StringResource): String = resource.name
    }

    private class SilentMessageHelper : MessageHelper {
        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showSnackBar(
            iconType: IconType,
            messageText: String,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) = Unit

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
        override fun startTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITracking() = Unit
        override fun shotTTILogging() = Unit
        override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
    }

    private companion object {
        const val PHOTO_PATH = "/cache/meal_ceremony_test.jpg"
    }
}

/** 남긴 분석 기록을 모아 둔다. */
private class RecordingAnalyticsHelper : AnalyticsHelper {
    val events = mutableListOf<AnalyticsEvent>()

    override fun logScreenView(screenName: String) = Unit

    override fun logEvent(event: AnalyticsEvent) {
        events += event
    }
}
