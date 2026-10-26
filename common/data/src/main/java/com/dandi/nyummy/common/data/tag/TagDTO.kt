package com.dandi.nyummy.common.data.tag

import com.dandi.nyummy.common.entity.tag.TagTone
import com.dandi.nyummy.common.entity.tag.TagVO
import kotlinx.serialization.Serializable

/**
 * SDUI 공통 객체 Tag. 상품 태그와 배너 배지가 같이 쓴다.
 *
 * @property text 표시 글자 ("한정", "신규", "새 집사 전용")
 * @property tone 색 (PROMO / SUCCESS / NEUTRAL)
 */
@Serializable
data class TagDTO(
    val text: String? = null,
    val tone: String? = null,
) {
    /**
     * 글자가 없으면 태그가 없는 것으로 보고 null.
     * 모르는 tone은 자리마다 정한 [defaultTone]으로 그린다(상품 태그는 PROMO, 배너 배지는 NEUTRAL).
     */
    fun toVO(defaultTone: TagTone): TagVO? {
        val label = text?.takeIf { it.isNotBlank() } ?: return null
        return TagVO(text = label, tone = TagTone.entries.find { it.name == tone } ?: defaultTone)
    }
}
