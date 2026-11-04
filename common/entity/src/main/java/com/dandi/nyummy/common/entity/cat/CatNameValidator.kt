package com.dandi.nyummy.common.entity.cat

/** 고양이 이름 입력 검증 실패 사유. */
enum class CatNameError {
    EMPTY,
    TOO_LONG,
}

/**
 * 고양이 이름 규칙: 앞뒤 공백을 뺀 1~[MAX_LENGTH]자.
 * 서버와 같이 String 길이로 센다. 그래서 이모지는 2자 이상으로 센다(서버는 넘으면 `api.cat.nameTooLong`으로 거절한다).
 */
object CatNameValidator {
    const val MAX_LENGTH = 8

    fun validate(name: String): CatNameError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> CatNameError.EMPTY
            trimmed.length > MAX_LENGTH -> CatNameError.TOO_LONG
            else -> null
        }
    }
}
