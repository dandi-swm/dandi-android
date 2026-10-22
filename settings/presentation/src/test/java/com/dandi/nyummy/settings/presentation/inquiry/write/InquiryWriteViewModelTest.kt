package com.dandi.nyummy.settings.presentation.inquiry.write

import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.settings.domain.AttachInquiryImageUseCase
import com.dandi.nyummy.settings.domain.InquiryRepository
import com.dandi.nyummy.settings.domain.SendInquiryUseCase
import com.dandi.nyummy.settings.entity.InquiryCategory
import com.dandi.nyummy.settings.entity.InquiryPageVO
import com.dandi.nyummy.settings.entity.InquiryVO
import com.dandi.nyummy.settings.presentation.R
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

@OptIn(ExperimentalCoroutinesApi::class)
class InquiryWriteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeInquiryRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `사진을 고르면 준비하는 동안 보내기를 막고, 끝나면 썸네일 경로를 둔다`() = runTest(testDispatcher) {
        val prepared = CompletableDeferred<String>()
        repository.prepare = { prepared.await() }
        val viewModel = createViewModel()

        viewModel.onIntent(InquiryWriteIntent.ImagePicked("content://picker/1"))
        runCurrent()
        assertTrue(viewModel.uiState.value.isImageLoading)

        prepared.complete("/cache/inquiry_image_1.jpg")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isImageLoading)
        assertEquals("/cache/inquiry_image_1.jpg", viewModel.uiState.value.imagePath)
    }

    @Test
    fun `선택기를 그냥 닫으면 아무것도 바뀌지 않는다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(InquiryWriteIntent.ImagePicked(null))
        advanceUntilIdle()

        assertTrue(repository.preparedPhotoUris.isEmpty())
        assertEquals(InquiryWriteUIState.empty, viewModel.uiState.value)
    }

    @Test
    fun `사진을 읽지 못하면 토스트로 알리고 칸을 비워 둔다`() = runTest(testDispatcher) {
        repository.prepare = { throw IllegalArgumentException("not an image") }
        val viewModel = createViewModel()

        viewModel.onIntent(InquiryWriteIntent.ImagePicked("content://picker/1"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isImageLoading)
        assertNull(viewModel.uiState.value.imagePath)
        assertEquals(listOf(R.string.inquiry_write_image_load_failed), messageHelper.toastRes)
    }

    @Test
    fun `사진을 빼면 사진 없이 보낸다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(InquiryWriteIntent.ImagePicked("content://picker/1"))
        advanceUntilIdle()

        viewModel.onIntent(InquiryWriteIntent.ClickRemoveImage)
        fillAndSend(viewModel)
        advanceUntilIdle()

        assertTrue(repository.uploadedImagePaths.isEmpty())
        assertNull(repository.sentImageKeys.single())
    }

    @Test
    fun `사진을 붙여 보내면 그 사진을 올리고 받은 키로 보낸 뒤 돌아간다`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(InquiryWriteIntent.ImagePicked("content://picker/1"))
        advanceUntilIdle()

        fillAndSend(viewModel)
        advanceUntilIdle()

        assertEquals(listOf("/cache/inquiry_image_1.jpg"), repository.uploadedImagePaths)
        assertEquals(listOf<String?>("inquiries/1/a.jpg"), repository.sentImageKeys)
        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `사진을 준비하는 동안에는 보내지 않는다`() = runTest(testDispatcher) {
        repository.prepare = { CompletableDeferred<String>().await() }
        val viewModel = createViewModel()
        viewModel.onIntent(InquiryWriteIntent.ImagePicked("content://picker/1"))
        runCurrent()

        fillAndSend(viewModel)
        runCurrent()

        assertTrue(repository.sentImageKeys.isEmpty())
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun `사진 업로드가 끊기면 보내기 실패를 알리고 다시 보낼 수 있게 둔다`() = runTest(testDispatcher) {
        repository.uploadError = IOException("upload cut")
        val viewModel = createViewModel()
        viewModel.onIntent(InquiryWriteIntent.ImagePicked("content://picker/1"))
        advanceUntilIdle()

        fillAndSend(viewModel)
        advanceUntilIdle()

        assertTrue(repository.sentImageKeys.isEmpty())
        assertFalse(viewModel.uiState.value.isSending)
        assertEquals("/cache/inquiry_image_1.jpg", viewModel.uiState.value.imagePath)
        assertEquals(listOf(R.string.inquiry_write_send_failed), messageHelper.snackBarRes)
        assertEquals(0, navigationHelper.backCount)
    }

    private fun fillAndSend(viewModel: InquiryWriteViewModel) {
        viewModel.onIntent(InquiryWriteIntent.ChangeTitle("사진이 안 보여요"))
        viewModel.onIntent(InquiryWriteIntent.ChangeContent("기록 화면에서 사진이 비어 있어요"))
        viewModel.onIntent(InquiryWriteIntent.ClickSend)
    }

    private fun createViewModel() = InquiryWriteViewModel(
        sendInquiry = SendInquiryUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        attachInquiryImage = AttachInquiryImageUseCase(
            repository,
            FakeResourceHelper,
            messageHelper,
            navigationHelper,
            FakeTTIHelper,
        ),
        navigationHelper = navigationHelper,
        messageHelper = messageHelper,
    )
}

/** 문의 사진 준비, 업로드, 보내기만 흉내 내는 저장소. 사진 준비를 멈추거나 실패시키려면 [prepare]를 바꾼다. */
private class FakeInquiryRepository : InquiryRepository {
    var prepare: suspend (String) -> String = { "/cache/inquiry_image_1.jpg" }
    var uploadError: Exception? = null
    val preparedPhotoUris = mutableListOf<String>()
    val uploadedImagePaths = mutableListOf<String>()
    val sentImageKeys = mutableListOf<String?>()

    override suspend fun prepareInquiryImage(photoUri: String): String {
        preparedPhotoUris += photoUri
        return prepare(photoUri)
    }

    override suspend fun uploadInquiryImage(imagePath: String): String {
        uploadedImagePaths += imagePath
        uploadError?.let { throw it }
        return "inquiries/1/a.jpg"
    }

    override suspend fun sendInquiry(category: InquiryCategory, title: String, content: String, imageKey: String?) {
        sentImageKeys += imageKey
    }

    override suspend fun getInquiries(cursor: Long?, size: Int): InquiryPageVO = InquiryPageVO.empty
    override suspend fun getInquiry(inquiryId: Long): InquiryVO? = null
    override suspend fun deleteInquiry(inquiryId: Long) = Unit
    override suspend fun restoreInquiry(inquiryId: Long) = Unit
}

private class RecordingMessageHelper : MessageHelper {
    val toastRes = mutableListOf<Int>()
    val snackBarRes = mutableListOf<Int>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showToast(messageRes: Int) {
        toastRes += messageRes
    }

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

private class RecordingNavigationHelper : NavigationHelper {
    var backCount = 0

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) = Unit
    override fun navigateDeepLink(route: NavRoute) = Unit
    override fun navigateToBack() {
        backCount++
    }

    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() = Unit
    override fun navigateToExternalLink(url: String) = Unit
}

private object FakeResourceHelper : ResourceHelper {
    override fun getString(resource: StringResource): String = ""
}

private object FakeTTIHelper : TTIHelper {
    override fun startTTITracking(page: TTIPage) = Unit
    override fun startTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITracking() = Unit
    override fun shotTTILogging() = Unit
    override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
}
