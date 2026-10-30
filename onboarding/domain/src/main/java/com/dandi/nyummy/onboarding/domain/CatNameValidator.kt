package com.dandi.nyummy.onboarding.domain

/** 고양이 이름 입력 검증 실패 사유. */
enum class CatNameError {
    EMPTY,
    TOO_LONG,
}

/** 고양이 이름 규칙: 앞뒤 공백을 뺀 1~[MAX_LENGTH]자. 서버도 같은 길이로 막는다. */
object CatNameValidator {
    const val MAX_LENGTH = 8

    fun validate(name: String): CatNameError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> CatNameError.EMPTY
            trimmed.codePointCount(0, trimmed.length) > MAX_LENGTH -> CatNameError.TOO_LONG
            else -> null
        }
    }
}
