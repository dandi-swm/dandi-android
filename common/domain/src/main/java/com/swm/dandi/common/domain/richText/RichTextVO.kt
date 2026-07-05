package com.swm.dandi.common.domain.richText

import kotlinx.serialization.Serializable

@Serializable
data class RichTextVO(
    val text: String,
    val typeScale: String,
    val color: String,
    val link: String?,
) {
    val hasLink: Boolean get() = link != null

    companion object {
        val empty: List<RichTextVO> = emptyList()
    }
}

typealias RichText = List<RichTextVO>
