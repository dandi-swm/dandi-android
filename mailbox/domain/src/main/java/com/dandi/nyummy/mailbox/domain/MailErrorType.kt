package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

// TODO: 백엔드가 우편 API 에러 code를 정하면 type을 실제 값으로 바꾸고 쓰지 않는 항목은 지운다.
//  지금 서버는 요청과 관계없이 성공 고정 응답만 주므로 아래 code는 임시로 정한 이름이다.
/**
 * 우편함 아이템 탭(우편 API) 도메인 에러. `type`은 서버 에러 바디의 `code`와 철자까지 같아야 한다.
 */
enum class MailErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    /** 없는 우편이거나 다른 사용자의 우편. 받기는 이미 사라진 우편으로, 삭제는 성공으로 본다. */
    MAIL_NOT_FOUND(
        type = "api.mail.notFound",
        errorMsg = "",
    ),

    /** 이미 받은 우편. 다른 기기에서 받았거나 버튼을 두 번 눌렀다. */
    MAIL_ALREADY_CLAIMED(
        type = "api.mail.alreadyClaimed",
        errorMsg = "",
    ),

    /** 보관 기간(도착 후 30일)이 지나 사라진 우편. */
    MAIL_EXPIRED(
        type = "api.mail.expired",
        errorMsg = "",
    ),
}
