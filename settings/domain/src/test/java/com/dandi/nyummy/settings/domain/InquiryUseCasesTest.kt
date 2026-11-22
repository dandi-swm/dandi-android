package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.settings.entity.InquiryCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class InquiryUseCasesTest {

    private val repository = FakeInquiryRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `사진 없이 보내면 올리지 않고 이미지 키 없이 보낸다`() = runBlocking {
        val result = sendInquiry()(InquiryCategory.QUESTION, " 제목 ", " 내용 ")

        assertTrue(result.isSuccess)
        assertTrue(repository.uploadedImagePaths.isEmpty())
        assertEquals(
            listOf(FakeInquiryRepository.SentInquiry(InquiryCategory.QUESTION, "제목", "내용", null)),
            repository.sentInquiries,
        )
    }

    @Test
    fun `사진을 붙여 보내면 먼저 올리고 받은 이미지 키를 함께 보낸다`() = runBlocking {
        val result = sendInquiry()(InquiryCategory.BUG_REPORT, "제목", "내용", imagePath = "/cache/a.jpg")

        assertTrue(result.isSuccess)
        assertEquals(listOf("/cache/a.jpg"), repository.uploadedImagePaths)
        assertEquals(repository.uploadedImageKey, repository.sentInquiries.single().imageKey)
    }

    @Test
    fun `사진을 올리지 못하면 문의를 보내지 않고 실패를 돌려준다`() = runBlocking {
        repository.imageError = IOException("upload cut")

        val result = sendInquiry()(InquiryCategory.QUESTION, "제목", "내용", imagePath = "/cache/a.jpg")

        assertTrue(result.isFailure)
        assertTrue(repository.sentInquiries.isEmpty())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `고른 사진을 첨부용 파일로 준비해 그 경로를 돌려준다`() = runBlocking {
        val result = attachInquiryImage()("content://media/picker/1")

        assertEquals(repository.preparedImagePath, result.getOrNull())
        assertEquals(listOf("content://media/picker/1"), repository.preparedPhotoUris)
    }

    @Test
    fun `사진을 읽지 못하면 사진 불러오기 실패로 돌려준다`() = runBlocking {
        repository.imageError = IllegalArgumentException("not an image")

        val result = attachInquiryImage()("content://media/picker/1")

        assertTrue(result.exceptionOrNull() is InquiryImageLoadException)
    }

    @Test
    fun `사진을 준비하다 화면을 떠나면 취소를 그대로 전파한다`() {
        repository.imageError = CancellationException("left screen")

        val thrown = runCatching { runBlocking { attachInquiryImage()("content://media/picker/1") } }.exceptionOrNull()

        assertTrue("취소가 전파되지 않았다: $thrown", thrown is CancellationException)
    }

    @Test
    fun `네트워크가 끊겨도 문의 목록 상세 되살리기는 앱을 종료시키지 않고 실패를 돌려준다`() = runBlocking {
        repository.readError = IOException("offline")

        val list = GetInquiriesUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)()
        val detail = GetInquiryDetailUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)(1L)
        val restore = RestoreInquiryUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)(1L)

        assertTrue(list.exceptionOrNull() is IOException)
        assertTrue(detail.exceptionOrNull() is IOException)
        assertTrue(restore.exceptionOrNull() is IOException)
        // 목록과 상세는 네트워크 안내를 띄우고, 되살리기는 화면이 행을 다시 빼며 안내한다.
        assertEquals(listOf(StringResource.NETWORK_ERROR.name, StringResource.NETWORK_ERROR.name), messageHelper.snackBars.map { it.messageText })
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `응답을 해석하지 못한 예상 밖 예외도 문의 목록은 실패로 돌려준다`() = runBlocking {
        repository.readError = IllegalStateException("broken body")

        val list = GetInquiriesUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)()

        assertTrue(list.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `없는 문의 상세는 공통 404 안내 대신 없음으로 성공한다`() = runBlocking {
        repository.readError = httpException(404, InquiryErrorType.INQUIRY_NOT_FOUND.type)

        val detail = GetInquiryDetailUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)(1L)

        assertTrue(detail.isSuccess)
        assertEquals(null, detail.getOrNull())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    private fun sendInquiry() =
        SendInquiryUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)

    private fun attachInquiryImage() =
        AttachInquiryImageUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
}
