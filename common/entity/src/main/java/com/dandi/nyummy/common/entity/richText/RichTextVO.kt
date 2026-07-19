package com.dandi.nyummy.common.entity.richText

import kotlinx.serialization.Serializable

@Serializable
data class RichTextVO(
    val text: String = "",
    val typeScale: String = "",
    val color: String = "",
    val link: String = "",
) {
    val hasLink: Boolean get() = link.isNotEmpty()

    companion object {
        val empty: List<RichTextVO> = emptyList()
    }
}

typealias RichText = List<RichTextVO>
