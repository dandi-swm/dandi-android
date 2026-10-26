package com.dandi.nyummy.common.data.richText

import com.dandi.nyummy.common.entity.richText.RichText
import com.dandi.nyummy.common.entity.richText.RichTextVO
import kotlinx.serialization.Serializable

/**
 * SDUI 공통 객체 RichText의 조각 하나. 응답에는 이 조각의 배열로 온다.
 *
 * @property text 글자. 띄어쓰기와 줄바꿈(\n)을 포함한다. icon 조각이면 비어 있다.
 * @property typeScale 글자 토큰 이름 (displayM / titleS / labelS / numberS 등)
 * @property color 색 토큰 이름 (contentPrimary / contentSecondary / contentTertiary / contentPromo 등)
 * @property icon 아이콘 조각 이름 (coin / clock / chevronRight)
 * @property strike 취소선 여부. 없으면 false
 */
@Serializable
data class RichTextDTO(
    val text: String? = null,
    val typeScale: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val strike: Boolean? = null,
) {
    fun toVO(): RichTextVO = RichTextVO(
        text = text ?: "",
        typeScale = typeScale ?: "",
        color = color ?: "",
        icon = icon ?: "",
        strike = strike == true,
    )
}

/** 글자도 아이콘도 없는 조각은 그릴 게 없으므로 뺀다. */
fun List<RichTextDTO>?.toRichText(): RichText =
    orEmpty().map { it.toVO() }.filter { it.text.isNotEmpty() || it.isIcon }
