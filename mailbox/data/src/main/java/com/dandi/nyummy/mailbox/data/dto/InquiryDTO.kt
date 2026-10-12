package com.dandi.nyummy.mailbox.data.dto

import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.mailbox.entity.InquiryAnswerVO
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.entity.InquiryVO
import kotlinx.serialization.Serializable

/**
 * GET /api/v1/inquiries 응답(내가 보낸 문의 목록). 서버 명세 전 임시 형태다.
 *
 * @property inquiries 최근에 보낸 문의가 앞에 온다.
 */
@Serializable
data class InquiryListResponseDTO(
    val inquiries: List<InquiryDTO>? = null,
) {
    fun toVO(): List<InquiryVO> = inquiries.orEmpty().map { it.toVO() }
}

/**
 * 문의 하나. GET /api/v1/inquiries/{inquiryId}, POST /api/v1/inquiries 응답과 같은 형태로 둔다.
 *
 * @property category BUG_REPORT / QUESTION / SUGGESTION. 모르는 값은 QUESTION으로 본다.
 * @property createdAt 보낸 시각(ISO date-time). data 레이어가 epoch millis로 바꾼다.
 * @property answer 운영팀 답장. 아직 없으면 null.
 */
@Serializable
data class InquiryDTO(
    val inquiryId: Long? = null,
    val category: String? = null,
    val content: String? = null,
    val createdAt: String? = null,
    val answer: InquiryAnswerDTO? = null,
) {
    fun toVO(): InquiryVO = InquiryVO(
        inquiryId = inquiryId ?: 0L,
        category = category.toInquiryCategory(),
        content = content?.trim().orEmpty(),
        createdAtMillis = KstTime.parseIsoToEpochMillisOrNull(createdAt) ?: 0L,
        answer = answer?.toVO() ?: InquiryAnswerVO.empty,
    )
}

/**
 * 문의 답장.
 *
 * @property answeredAt 답장한 시각(ISO date-time).
 */
@Serializable
data class InquiryAnswerDTO(
    val content: String? = null,
    val answeredAt: String? = null,
) {
    fun toVO(): InquiryAnswerVO = InquiryAnswerVO(
        content = content?.trim().orEmpty(),
        answeredAtMillis = KstTime.parseIsoToEpochMillisOrNull(answeredAt) ?: 0L,
    )
}

/** POST /api/v1/inquiries 요청. */
@Serializable
data class SendInquiryRequestDTO(
    val category: String? = null,
    val content: String? = null,
)

private fun String?.toInquiryCategory(): InquiryCategory =
    InquiryCategory.entries.firstOrNull { it.name == this } ?: InquiryCategory.QUESTION
