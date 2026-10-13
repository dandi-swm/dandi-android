package com.dandi.nyummy.mailbox.data.dto

import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.mailbox.entity.InquiryAnswerVO
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.entity.InquiryPageVO
import com.dandi.nyummy.mailbox.entity.InquiryVO
import kotlinx.serialization.Serializable

/**
 * GET /api/v1/inquiries 응답(내가 보낸 문의 목록 한 묶음, 최신순).
 *
 * @property nextCursor 다음 묶음을 읽을 때 cursor로 넘길 값. 마지막 묶음이면 null.
 * @property hasNext 다음 묶음이 있다. false면 [nextCursor]는 null이다.
 */
@Serializable
data class InquiryListResponseDTO(
    val inquiries: List<InquirySummaryDTO>? = null,
    val nextCursor: Long? = null,
    val hasNext: Boolean? = null,
) {
    fun toVO(): InquiryPageVO = InquiryPageVO(
        inquiries = inquiries.orEmpty().map { it.toVO() },
        nextCursor = nextCursor.takeIf { hasNext == true },
    )
}

/**
 * 목록의 문의 하나. 내용과 답장은 상세 조회에서만 온다.
 *
 * @property category BUG_REPORT / QUESTION / SUGGESTION. 모르는 값은 QUESTION으로 본다.
 * @property questionTitle 문의 제목.
 * @property createdAt 보낸 시각(ISO date-time). data 레이어가 epoch millis로 바꾼다.
 * @property hasUnreadAnswer 답장이 달렸는데 아직 상세를 열어 보지 않았다.
 */
@Serializable
data class InquirySummaryDTO(
    val inquiryId: Long? = null,
    val category: String? = null,
    val questionTitle: String? = null,
    val createdAt: String? = null,
    val isAnswered: Boolean? = null,
    val hasUnreadAnswer: Boolean? = null,
) {
    fun toVO(): InquiryVO {
        val answered = isAnswered == true
        return InquiryVO(
            inquiryId = inquiryId ?: 0L,
            category = category.toInquiryCategory(),
            title = questionTitle?.trim().orEmpty(),
            createdAtMillis = DeviceTime.parseIsoToEpochMillisOrNull(createdAt) ?: 0L,
            isAnswered = answered,
            // 답장이 달린 문의만 안 읽음으로 본다.
            hasUnreadAnswer = answered && hasUnreadAnswer == true,
        )
    }
}

/**
 * GET /api/v1/inquiries/{inquiryId} 응답(문의 하나와 답장).
 *
 * @property questionContent 문의 내용.
 * @property imageUrl 첨부 이미지 조회 URL(일정 시간만 유효). 앱은 아직 첨부를 쓰지 않는다.
 * @property createdAt 보낸 시각(ISO date-time).
 * @property isAnswered 답장이 달렸다. 답장 필드는 이 값이 true일 때만 채워진다.
 * @property answerTitle 답장 제목. 앱은 아직 보여 주지 않는다.
 * @property answeredAt 답장한 시각(ISO date-time).
 */
@Serializable
data class InquiryDTO(
    val inquiryId: Long? = null,
    val category: String? = null,
    val questionTitle: String? = null,
    val questionContent: String? = null,
    val imageUrl: String? = null,
    val createdAt: String? = null,
    val isAnswered: Boolean? = null,
    val answerTitle: String? = null,
    val answerContent: String? = null,
    val answeredAt: String? = null,
) {
    fun toVO(): InquiryVO {
        val answered = isAnswered == true
        return InquiryVO(
            inquiryId = inquiryId ?: 0L,
            category = category.toInquiryCategory(),
            title = questionTitle?.trim().orEmpty(),
            content = questionContent?.trim().orEmpty(),
            createdAtMillis = DeviceTime.parseIsoToEpochMillisOrNull(createdAt) ?: 0L,
            isAnswered = answered,
            answer = if (answered) {
                InquiryAnswerVO(
                    content = answerContent?.trim().orEmpty(),
                    answeredAtMillis = DeviceTime.parseIsoToEpochMillisOrNull(answeredAt) ?: 0L,
                )
            } else {
                InquiryAnswerVO.empty
            },
        )
    }
}

/**
 * POST /api/v1/inquiries 요청. 성공은 본문 없는 201이다.
 *
 * @property questionTitle 제목(서버 필수, 100자까지).
 * @property imageKey 이미지 업로드 URL 발급(purpose=INQUIRY)으로 받은 키. 앱은 아직 첨부를 쓰지 않는다.
 */
@Serializable
data class CreateInquiryRequestDTO(
    val category: String? = null,
    val questionTitle: String? = null,
    val questionContent: String? = null,
    val imageKey: String? = null,
)

private fun String?.toInquiryCategory(): InquiryCategory =
    InquiryCategory.entries.firstOrNull { it.name == this } ?: InquiryCategory.QUESTION
