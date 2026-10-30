package com.dandi.nyummy.common.entity.cat

/**
 * 사용자가 온보딩에서 지어 준 고양이 이름을 문장에 넣는다.
 *
 * 이름 자리는 문자열 리소스면 `%1$s`, 대사 은행처럼 리소스가 아닌 문장이면 [NAME_TOKEN]으로 쓴다.
 * 이름 뒤 조사는 받침이 있을 때 쓰는 글자를 괄호에 넣어 `%1$s(이)가`처럼 쓰고, [resolveParticles]가
 * 이름의 마지막 글자를 보고 `달봉이`·`냐미가`처럼 고른다.
 */
object CatName {

    /** 이름을 아직 모를 때 쓰는 기본 이름. */
    const val DEFAULT = "냐미"

    /** 리소스가 아닌 문장의 이름 자리. */
    const val NAME_TOKEN = "{name}"

    /** [template]의 [NAME_TOKEN]을 [name]으로 바꾸고 조사를 고른다. */
    fun fill(template: String, name: String): String =
        resolveParticles(template.replace(NAME_TOKEN, name))

    /** `(이)가` `(을)를` `(은)는` `(과)와` `(이)야` `(이)랑` `(으)로`를 바로 앞 글자의 받침에 맞춰 고른다. */
    fun resolveParticles(text: String): String = PARTICLE_PATTERN.replace(text) { match ->
        val (withFinal, withoutFinal) = PARTICLES.getValue(match.value)
        when (finalConsonantBefore(text, match.range.first)) {
            Final.NONE -> withoutFinal
            Final.RIEUL -> if (match.value == RO) withoutFinal else withFinal
            Final.OTHER -> withFinal
        }
    }

    private enum class Final { NONE, RIEUL, OTHER }

    /** [index] 바로 앞 글자를 읽을 때의 받침. 한글은 글자 그대로, 숫자와 영문은 읽는 소리로 본다. */
    private fun finalConsonantBefore(text: String, index: Int): Final {
        val c = text.getOrNull(index - 1) ?: return Final.NONE
        return when (c) {
            in '가'..'힣' -> when ((c - '가') % JONGSEONG_COUNT) {
                0 -> Final.NONE
                JONGSEONG_RIEUL -> Final.RIEUL
                else -> Final.OTHER
            }
            // 영 일 이 삼 사 오 육 칠 팔 구
            in '0'..'9' -> DIGIT_FINALS[c - '0']
            in 'a'..'z', in 'A'..'Z' -> latinFinal(c.lowercaseChar(), text.getOrNull(index - 2)?.lowercaseChar())
            else -> Final.NONE
        }
    }

    /** 영문 이름은 끝소리로 어림한다(Tom 톰, Bill 빌, King 킹은 받침, Coco 코코, Max 맥스는 받침 없음). */
    private fun latinFinal(last: Char, beforeLast: Char?): Final = when {
        last == 'l' -> Final.RIEUL
        last in "mnkpt" -> Final.OTHER
        last == 'g' && beforeLast == 'n' -> Final.OTHER
        else -> Final.NONE
    }

    private const val JONGSEONG_COUNT = 28
    private const val JONGSEONG_RIEUL = 8
    private const val RO = "(으)로"

    private val DIGIT_FINALS = listOf(
        Final.OTHER, Final.RIEUL, Final.NONE, Final.OTHER, Final.NONE,
        Final.NONE, Final.OTHER, Final.RIEUL, Final.RIEUL, Final.NONE,
    )

    /** 표기 → (받침 있을 때, 받침 없을 때) */
    private val PARTICLES = mapOf(
        "(이)가" to ("이" to "가"),
        "(을)를" to ("을" to "를"),
        "(은)는" to ("은" to "는"),
        "(과)와" to ("과" to "와"),
        "(이)야" to ("이야" to "야"),
        "(이)랑" to ("이랑" to "랑"),
        RO to ("으로" to "로"),
    )

    private val PARTICLE_PATTERN = Regex(PARTICLES.keys.joinToString("|") { Regex.escape(it) })
}
