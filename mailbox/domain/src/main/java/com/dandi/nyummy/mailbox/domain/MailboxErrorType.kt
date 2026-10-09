package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

/**
 * 우편함 문의 API 도메인 에러. `type`은 서버 에러 바디의 `code`와 철자까지 같아야 한다.
 * 명세: docs/api/mailbox-inquiry.md
 */
enum class MailboxErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    /** 없는 문의이거나 다른 사용자의 문의(404). 화면은 우편함으로 돌아간다. */
    INQUIRY_NOT_FOUND(
        type = "api.inquiry.notFound",
        errorMsg = "",
    ),
}
