package com.dandi.nyummy.mailbox.data

import android.content.Context
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.mailbox.data.dto.InquiryDTO
import com.dandi.nyummy.mailbox.data.dto.InquiryListResponseDTO
import com.dandi.nyummy.mailbox.data.dto.SendInquiryRequestDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * 서버 API가 생기기 전까지 쓰는 우편함 데이터 소스.
 *
 * assets의 happy case JSON([HAPPY_CASE_ASSET])을 실제와 같은 [Json] 설정으로 파싱해 DTO → VO 경로를 그대로 탄다.
 * 보낸 문의는 앱이 살아 있는 동안 메모리에만 남는다(맨 앞에 추가).
 *
 * TODO(server): API가 생기면 `BaseRemoteDataSource` + `MailboxApiService`로 바꾸고 JSON 파일을 지운다.
 */
class MailboxDataSource(
    private val context: Context,
    private val json: Json,
    private val latencyMillis: Long = MOCK_LATENCY_MILLIS,
) {

    private val mutex = Mutex()
    private var inquiries: List<InquiryDTO>? = null

    suspend fun getInquiries(): InquiryListResponseDTO {
        delay(latencyMillis)
        return InquiryListResponseDTO(inquiries = loadedInquiries())
    }

    suspend fun getInquiry(inquiryId: Long): InquiryDTO? {
        delay(latencyMillis)
        return loadedInquiries().firstOrNull { it.inquiryId == inquiryId }
    }

    suspend fun sendInquiry(request: SendInquiryRequestDTO): InquiryDTO {
        delay(latencyMillis)
        return mutex.withLock {
            val current = inquiries ?: readHappyCase()
            val created = InquiryDTO(
                inquiryId = (current.maxOfOrNull { it.inquiryId ?: 0L } ?: 0L) + 1,
                category = request.category,
                content = request.content,
                createdAt = nowIso(),
                answer = null,
            )
            inquiries = listOf(created) + current
            created
        }
    }

    private suspend fun loadedInquiries(): List<InquiryDTO> = mutex.withLock {
        inquiries ?: readHappyCase().also { inquiries = it }
    }

    private suspend fun readHappyCase(): List<InquiryDTO> = withContext(Dispatchers.IO) {
        val raw = context.assets.open(HAPPY_CASE_ASSET).bufferedReader().use { it.readText() }
        json.decodeFromString<InquiryListResponseDTO>(raw).inquiries.orEmpty()
    }

    /** 서버가 줄 형태(KST 오프셋이 붙은 ISO)로 지금 시각을 만든다. */
    private fun nowIso(): String = with(KstTime.now()) {
        String.format(Locale.US, "%sT%02d:%02d:%02d+09:00", isoDate, hour, minute, second)
    }

    companion object {
        const val HAPPY_CASE_ASSET = "mailbox_inquiries_happy_case.json"
        private const val MOCK_LATENCY_MILLIS = 400L
    }
}
