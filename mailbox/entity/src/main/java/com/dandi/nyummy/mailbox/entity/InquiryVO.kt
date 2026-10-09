package com.dandi.nyummy.mailbox.entity

/** 문의 유형. 화면 라벨은 presentation 문자열 리소스가 정한다. */
enum class InquiryCategory {
    BUG_REPORT,
    QUESTION,
    SUGGESTION,
}

/**
 * 내가 보낸 문의 하나.
 *
 * @property createdAtMillis 보낸 시각(epoch millis). 화면은 KST로 바꿔 보여 준다.
 * @property answer 운영팀 답장. 아직 답장이 없으면 [InquiryAnswerVO.empty].
 */
data class InquiryVO(
    val inquiryId: Long = 0L,
    val category: InquiryCategory = InquiryCategory.QUESTION,
    val content: String = "",
    val createdAtMillis: Long = 0L,
    val answer: InquiryAnswerVO = InquiryAnswerVO.empty,
) {
    val isAnswered: Boolean
        get() = answer.content.isNotBlank()
}

/** 문의에 온 답장. */
data class InquiryAnswerVO(
    val content: String = "",
    val answeredAtMillis: Long = 0L,
) {
    companion object {
        val empty = InquiryAnswerVO()
    }
}
