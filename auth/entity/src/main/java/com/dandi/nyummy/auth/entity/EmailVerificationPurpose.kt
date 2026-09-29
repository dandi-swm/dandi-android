package com.dandi.nyummy.auth.entity

/** 이메일 인증 코드 발송 목적 — 서버 enum 값과 이름이 일치해야 한다. */
enum class EmailVerificationPurpose {
    SIGNUP,
    RESET_PASSWORD,
}
