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
 * 목록 조회에서는 [content]와 [answer]가 비어 있고, 상세 조회에서 채워진다.
 *
 * @property title 문의 제목.
 * @property createdAtMillis 보낸 시각(epoch millis). 화면은 기기 타임존으로 바꿔 보여 준다.
 * @property isAnswered 운영팀 답장이 달렸다.
 * @property hasUnreadAnswer 답장이 달렸는데 아직 상세를 열어 보지 않았다. 목록 조회에서만 채워진다.
 * @property answer 운영팀 답장. 아직 답장이 없거나 목록 조회면 [InquiryAnswerVO.empty].
 */
data class InquiryVO(
    val inquiryId: Long = 0L,
    val category: InquiryCategory = InquiryCategory.QUESTION,
    val title: String = "",
    val content: String = "",
    val createdAtMillis: Long = 0L,
    val isAnswered: Boolean = false,
    val hasUnreadAnswer: Boolean = false,
    val answer: InquiryAnswerVO = InquiryAnswerVO.empty,
)

/** 문의에 온 답장. */
data class InquiryAnswerVO(
    val content: String = "",
    val answeredAtMillis: Long = 0L,
) {
    companion object {
        val empty = InquiryAnswerVO()
    }
}

/**
 * 문의 목록 한 묶음(최신순).
 *
 * @property nextCursor 다음 묶음을 읽을 때 넘길 값. 마지막 묶음이면 null.
 */
data class InquiryPageVO(
    val inquiries: List<InquiryVO> = emptyList(),
    val nextCursor: Long? = null,
) {
    val hasNext: Boolean
        get() = nextCursor != null

    companion object {
        val empty = InquiryPageVO()
    }
}
