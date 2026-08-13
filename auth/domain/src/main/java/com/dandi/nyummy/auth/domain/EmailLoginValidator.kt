package com.dandi.nyummy.auth.domain

/**
 * 이메일 로그인 입력값 검증
 */
object EmailLoginValidator {

    /** 로컬 파트 atom — RFC 5322 dot-atom 허용 특수문자 포함 (따옴표 로컬 파트는 미지원). */
    private const val LOCAL_PART_ATOM = "[a-z0-9!#\$%&'*+/=?^_`{|}~-]"

    /** 도메인 라벨 — 영숫자 시작·끝, 내부에만 하이픈 허용, 최대 63자. 빈 라벨(`@.d.d`) 불가. */
    private const val DOMAIN_LABEL = "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"

    /**
     * 전체 형식: `atom(.atom)*@(label.)+TLD`
     * - 로컬 파트: 점으로 구분된 atom — 선행/후행/연속 점 불가
     * - 도메인: 점으로 구분된 라벨 1개 이상 + 영문 2자 이상 TLD
     */
    private val EMAIL_REGEX = Regex(
        "$LOCAL_PART_ATOM+(?:\\.$LOCAL_PART_ATOM+)*@(?:$DOMAIN_LABEL\\.)+[a-z]{2,}",
        RegexOption.IGNORE_CASE,
    )

    private const val MAX_EMAIL_LENGTH = 254
    private const val MAX_LOCAL_PART_LENGTH = 64

    fun validateEmail(email: String): EmailLoginFieldError? {
        val isValid = email.length <= MAX_EMAIL_LENGTH &&
            email.substringBefore('@').length <= MAX_LOCAL_PART_LENGTH &&
            EMAIL_REGEX.matches(email)
        return if (isValid) null else EmailLoginFieldError.EMAIL_FORMAT
    }
}
