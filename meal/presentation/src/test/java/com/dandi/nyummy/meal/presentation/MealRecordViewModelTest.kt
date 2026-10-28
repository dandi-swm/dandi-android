package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class MealRecordViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val mealRepository = FakeMealRecordRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
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
    fun `기록이 접수되면 상자 등급이 연출 상태에 실린다`() = runTest(testDispatcher) {
        mealRepository.created = CreatedMealVO(mealId = 1, status = "ANALYZING", rewardBoxGrade = RewardBoxGrade.GOLD)
        val viewModel = createViewModel()

        submit(viewModel)

        val ceremony = viewModel.ceremony()
        assertEquals(MealSubmitStatus.Accepted(RewardBoxGrade.GOLD), ceremony.submit)
        assertTrue(viewModel.uiState.value.isSubmitSucceeded)
    }

    @Test
    fun `우편함 플래그는 원격 구성의 우편함 아이템 탭 값을 따른다`() = runTest(testDispatcher) {
        assertTrue(createViewModel(isMailGiftEnabled = true).uiState.value.isMailGiftEnabled)
        assertFalse(createViewModel(isMailGiftEnabled = false).uiState.value.isMailGiftEnabled)
    }

    @Test
    fun `제출에 실패하면 연출을 멈추고 곧장 홈으로 간다`() = runTest(testDispatcher) {
        mealRepository.createError = IOException("network")
        val viewModel = createViewModel(isMailGiftEnabled = true)

        submit(viewModel)

        val state = viewModel.uiState.value
        assertEquals(MealSubmitStatus.Failed, viewModel.ceremony().submit)
        assertTrue(state.isFinishing)
        assertEquals(listOf("back"), navigationHelper.calls)
    }

    @Test
    fun `실패한 뒤 닫기를 눌러도 한 번만 뒤로 간다`() = runTest(testDispatcher) {
        mealRepository.createError = IOException("network")
        val viewModel = createViewModel()
        submit(viewModel)

        viewModel.onIntent(MealRecordIntent.ClickClose)
        viewModel.onIntent(MealRecordIntent.ClickDone)

        assertEquals(listOf("back"), navigationHelper.calls)
    }

    @Test
    fun `접수되기 전에는 닫기와 완료로 화면을 닫을 수 없다`() = runTest(testDispatcher) {
        mealRepository.createGate = CompletableDeferred()
        val viewModel = createViewModel()
        submit(viewModel)

        viewModel.onIntent(MealRecordIntent.ClickClose)
        viewModel.onIntent(MealRecordIntent.ClickDone)

        assertTrue(navigationHelper.calls.isEmpty())
        assertFalse(viewModel.uiState.value.isFinishing)
    }

    @Test
    fun `완료를 여러 번 눌러도 한 번만 뒤로 간다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        submit(viewModel)

        viewModel.onIntent(MealRecordIntent.ClickDone)
        viewModel.onIntent(MealRecordIntent.ClickDone)
        viewModel.onIntent(MealRecordIntent.ClickClose)

        assertEquals(listOf("back"), navigationHelper.calls)
    }

    @Test
    fun `사진 카드가 올라오는 동안 접수되면 그 장면이 끝난 뒤 띠욧으로 넘어간다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        submit(viewModel)
        assertEquals(MealCeremonyStage.Serving, viewModel.ceremony().stage)

        viewModel.finishStage(MealCeremonyStage.Serving)

        assertEquals(MealCeremonyStage.Reaction, viewModel.ceremony().stage)
    }

    @Test
    fun `응답을 기다리는 중에 접수되면 곧장 띠욧으로 넘어간다`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>().also { mealRepository.createGate = it }
        val viewModel = createViewModel()
        submit(viewModel)

        viewModel.finishStage(MealCeremonyStage.Serving)
        assertEquals(MealCeremonyStage.Waiting, viewModel.ceremony().stage)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(MealCeremonyStage.Reaction, viewModel.ceremony().stage)
    }

    @Test
    fun `상자를 보내지 않으면 먹었어요에서 끝 장면으로 가고 완료를 기다린다`() = runTest(testDispatcher) {
        mealRepository.created = CreatedMealVO(mealId = 1, rewardBoxGrade = RewardBoxGrade.GOLD)
        val viewModel = createViewModel(isMailGiftEnabled = false)
        submit(viewModel)

        viewModel.finishStage(MealCeremonyStage.Serving)
        viewModel.finishStage(MealCeremonyStage.Reaction)
        viewModel.finishStage(MealCeremonyStage.Ate)
        viewModel.finishStage(MealCeremonyStage.Finished)

        assertEquals(MealCeremonyStage.Finished, viewModel.ceremony().stage)
        assertTrue(navigationHelper.calls.isEmpty())
    }

    @Test
    fun `상자를 보내면 상자가 우편함에 들어간 뒤 곧장 홈으로 간다`() = runTest(testDispatcher) {
        mealRepository.created = CreatedMealVO(mealId = 1, rewardBoxGrade = RewardBoxGrade.SILVER)
        val viewModel = createViewModel(isMailGiftEnabled = true)
        submit(viewModel)

        listOf(
            MealCeremonyStage.Serving,
            MealCeremonyStage.Reaction,
            MealCeremonyStage.Ate,
            MealCeremonyStage.BoxReveal,
        ).forEach { viewModel.finishStage(it) }
        assertEquals(MealCeremonyStage.BoxSend, viewModel.ceremony().stage)
        assertTrue(navigationHelper.calls.isEmpty())

        viewModel.finishStage(MealCeremonyStage.BoxSend)

        assertEquals(listOf("back"), navigationHelper.calls)
    }

    @Test
    fun `이미 지난 장면의 끝 알림이 늦게 오면 무시한다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        submit(viewModel)
        viewModel.finishStage(MealCeremonyStage.Serving)
        viewModel.finishStage(MealCeremonyStage.Reaction)

        // 화면이 다시 만들어지며 앞 장면을 다시 재생하다 끝났다고 알려 온 경우
        viewModel.finishStage(MealCeremonyStage.Serving)

        assertEquals(MealCeremonyStage.Ate, viewModel.ceremony().stage)
    }

    @Test
    fun `띠욧에서 건너뛰면 컷신만 건너뛰고 먹었어요로 간다`() = runTest(testDispatcher) {
        val viewModel = createViewModel(isMailGiftEnabled = true)
        submit(viewModel)
        viewModel.finishStage(MealCeremonyStage.Serving)

        viewModel.onIntent(MealRecordIntent.ClickSkipCeremony)

        assertEquals(MealCeremonyStage.Ate, viewModel.ceremony().stage)
        assertTrue(navigationHelper.calls.isEmpty())
    }

    @Test
    fun `상자를 보내는 기록은 건너뛰어도 상자 연출은 그대로 보여 준다`() = runTest(testDispatcher) {
        mealRepository.created = CreatedMealVO(mealId = 1, rewardBoxGrade = RewardBoxGrade.BRONZE)
        val viewModel = createViewModel(isMailGiftEnabled = true)
        submit(viewModel)
        viewModel.finishStage(MealCeremonyStage.Serving)

        viewModel.onIntent(MealRecordIntent.ClickSkipCeremony)
        // 먹었어요와 상자 장면에서는 건너뛰기가 먹지 않는다.
        viewModel.onIntent(MealRecordIntent.ClickSkipCeremony)
        assertEquals(MealCeremonyStage.Ate, viewModel.ceremony().stage)
        viewModel.finishStage(MealCeremonyStage.Ate)
        viewModel.onIntent(MealRecordIntent.ClickSkipCeremony)

        assertEquals(MealCeremonyStage.BoxReveal, viewModel.ceremony().stage)
        assertTrue(navigationHelper.calls.isEmpty())
    }

    @Test
    fun `접수 전에는 건너뛸 수 없다`() = runTest(testDispatcher) {
        mealRepository.createGate = CompletableDeferred()
        val viewModel = createViewModel()
        submit(viewModel)
        viewModel.finishStage(MealCeremonyStage.Serving)

        viewModel.onIntent(MealRecordIntent.ClickSkipCeremony)

        assertEquals(MealCeremonyStage.Waiting, viewModel.ceremony().stage)
    }

    @Test
    fun `셔터를 누르지 않았는데 온 촬영 결과는 버리고 파일을 지운다`() = runTest(testDispatcher) {
        val stray = tempFolder.newFile("stray.jpeg")
        val viewModel = createViewModel()

        viewModel.onIntent(MealRecordIntent.PhotoCaptured(stray.absolutePath))

        assertEquals(MealCameraPhase.Preview, viewModel.uiState.value.phase)
        assertFalse(stray.exists())
    }

    @Test
    fun `한 번의 셔터에 결과가 두 번 오면 처음 것만 쓰고 나중 파일은 지운다`() = runTest(testDispatcher) {
        val first = tempFolder.newFile("first.jpeg")
        val second = tempFolder.newFile("second.jpeg")
        val viewModel = createViewModel()

        capture(viewModel, first.absolutePath)
        viewModel.onIntent(MealRecordIntent.PhotoCaptured(second.absolutePath))

        assertEquals(MealCameraPhase.Captured(first.absolutePath, MealPhotoSource.CAMERA), viewModel.uiState.value.phase)
        assertFalse(second.exists())
    }

    @Test
    fun `사진 확인 중 앱이 종료됐다 돌아오면 그 사진으로 확인 단계를 되살린다`() = runTest(testDispatcher) {
        val photo = tempFolder.newFile("meal.jpeg")
        val savedState = SavedStateHandle()
        val beforeKill = createViewModel(savedState = savedState)
        capture(beforeKill, photo.absolutePath)
        advanceUntilIdle()
        analytics.events.clear()

        val restored = createViewModel(savedState = savedState.afterProcessDeath())

        assertEquals(MealCameraPhase.Captured(photo.absolutePath, MealPhotoSource.CAMERA), restored.uiState.value.phase)
        assertTrue(mealEvents().none { it.name == "meal_record_open" })
    }

    @Test
    fun `되살릴 사진이 캐시에서 지워졌으면 다시 찍게 한다`() = runTest(testDispatcher) {
        val photo = tempFolder.newFile("meal.jpeg")
        val savedState = SavedStateHandle()
        capture(createViewModel(savedState = savedState), photo.absolutePath)
        advanceUntilIdle()
        photo.delete()

        val restored = createViewModel(savedState = savedState.afterProcessDeath())

        assertEquals(MealCameraPhase.Preview, restored.uiState.value.phase)
    }

    @Test
    fun `접수된 뒤 앱이 종료됐다 돌아오면 다시 기록하지 않고 연출 끝 장면을 보여 준다`() = runTest(testDispatcher) {
        mealRepository.created = CreatedMealVO(mealId = 1, rewardBoxGrade = RewardBoxGrade.SILVER)
        val savedState = SavedStateHandle()
        submit(createViewModel(isMailGiftEnabled = true, savedState = savedState))

        val restored = createViewModel(isMailGiftEnabled = true, savedState = savedState.afterProcessDeath())

        val ceremony = restored.ceremony()
        assertEquals(MealSubmitStatus.Accepted(RewardBoxGrade.SILVER), ceremony.submit)
        assertEquals(MealCeremonyStage.Finished, ceremony.stage)
        assertTrue(navigationHelper.calls.isEmpty())
        assertEquals(1, mealRepository.createCount)
    }

    @Test
    fun `접수 결과를 받기 전에 앱이 종료됐다 돌아오면 기록 탭에서 확인하라고 안내하고 홈으로 간다`() = runTest(testDispatcher) {
        mealRepository.createGate = CompletableDeferred()
        val photo = tempFolder.newFile("meal.jpeg")
        val savedState = SavedStateHandle()
        val beforeKill = createViewModel(savedState = savedState)
        capture(beforeKill, photo.absolutePath)
        beforeKill.onIntent(MealRecordIntent.ClickSubmit)
        advanceUntilIdle()

        createViewModel(savedState = savedState.afterProcessDeath())

        assertEquals(listOf("back"), navigationHelper.calls)
        assertEquals(listOf(R.string.meal_record_submit_interrupted), messageHelper.snackBarRes)
        assertFalse(photo.exists())
        assertEquals(1, mealRepository.createCount)
    }

    @Test
    fun `앱이 종료됐다 돌아오면 카메라 권한은 저장해 두지 않고 다시 확인한다`() = runTest(testDispatcher) {
        val photo = tempFolder.newFile("meal.jpeg")
        val savedState = SavedStateHandle()
        val beforeKill = createViewModel(savedState = savedState)
        capture(beforeKill, photo.absolutePath)
        advanceUntilIdle()
        assertEquals(MealCameraPermission.Granted, beforeKill.uiState.value.cameraPermission)

        val restored = createViewModel(savedState = savedState.afterProcessDeath())

        assertEquals(MealCameraPermission.Requesting, restored.uiState.value.cameraPermission)
    }

    @Test
    fun `연출의 고양이방은 지금 마이룸에 깔린 배경으로 그린다`() = runTest(testDispatcher) {
        roomRepository.room = RoomVO(
            placements = listOf(
                RoomPlacementVO(roomItemId = 10, category = RoomItemCategory("BED"), itemId = 2),
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

    private fun MealRecordViewModel.ceremony(): MealCameraPhase.Ceremony =
        uiState.value.phase as MealCameraPhase.Ceremony

    private fun MealRecordViewModel.finishStage(stage: MealCeremonyStage) {
        onIntent(MealRecordIntent.CeremonyStageFinished(stage))
    }

    /** 앱이 종료될 때 OS가 저장해 둔 값만 가지고 새로 만든 저장소. */
    private fun SavedStateHandle.afterProcessDeath(): SavedStateHandle =
        SavedStateHandle(keys().associateWith { get<Any?>(it) })

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

    /** 카메라를 허용하고 셔터를 눌러 한 장 찍는다. */
    private fun capture(viewModel: MealRecordViewModel, photoPath: String = PHOTO_PATH) {
        viewModel.onIntent(MealRecordIntent.PermissionResult(granted = true))
        viewModel.onIntent(MealRecordIntent.ClickShutter)
        viewModel.onIntent(MealRecordIntent.PhotoCaptured(photoPath))
    }

    private fun TestScope.submit(viewModel: MealRecordViewModel) {
        capture(viewModel)
        viewModel.onIntent(MealRecordIntent.ClickSubmit)
        advanceUntilIdle()
    }

    private fun createViewModel(
        isMailGiftEnabled: Boolean = false,
        savedState: SavedStateHandle = SavedStateHandle(),
    ): MealRecordViewModel = MealRecordViewModel(
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
        savedStateHandle = savedState,
        remoteConfigHelper = FakeRemoteConfigHelper(isMailGiftEnabled),
        getRoomBackground = GetRoomBackgroundUseCase(roomRepository),
    )

    private class FakeRoomRepository : RoomRepository {
        var room: RoomVO = RoomVO()
        var error: Exception? = null

        override suspend fun getRoom(size: Int): RoomVO = error?.let { throw it } ?: room
        override suspend fun getOwnedItems(category: RoomItemCategory, cursor: Long?, size: Int) = OwnedRoomItemPageVO()
        override suspend fun saveRoom(placements: List<RoomPlacementVO>) = Unit
    }

    private class FakeMealRecordRepository : MealRecordRepository {
        var created: CreatedMealVO = CreatedMealVO(mealId = 1, status = "WAITING")
        var createError: Exception? = null

        /** 값이 있으면 이 값이 채워질 때까지 접수 응답을 미룬다. */
        var createGate: CompletableDeferred<Unit>? = null
        var createCount = 0

        override suspend fun readGalleryPhotoTakenAt(photoUri: String): LocalDateTime? = null
        override suspend fun importGalleryPhoto(photoUri: String) = photoUri
        override suspend fun prepareUploadImage(photoPath: String, source: MealPhotoSource) = Unit
        override suspend fun issueImageUploadUrl(photoPath: String): MealImageUploadVO = MealImageUploadVO()
        override suspend fun uploadImage(uploadTarget: MealImageUploadVO, photoPath: String) = Unit
        override suspend fun createMeal(imageKey: String): CreatedMealVO {
            createCount++
            createGate?.await()
            createError?.let { throw it }
            return created
        }

        override suspend fun createTutorialMeal() = Unit
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

    /** 리소스로 띄운 스낵바만 모아 둔다. */
    private class RecordingMessageHelper : MessageHelper {
        val snackBarRes = mutableListOf<Int>()

        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showToast(messageRes: Int) = Unit
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
        ) {
            snackBarRes += messageRes
        }

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
