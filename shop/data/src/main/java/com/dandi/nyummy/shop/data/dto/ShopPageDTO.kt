package com.dandi.nyummy.shop.data.dto

import com.dandi.nyummy.common.data.richText.RichTextDTO
import com.dandi.nyummy.common.data.richText.toRichText
import com.dandi.nyummy.shop.entity.SduiComponentVO
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.entity.ShopRecommendPageVO
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * GET /api/v1/shop/pages/recommend 응답(추천 탭 SDUI 한 장).
 *
 * 칸(`{viewType, content}`)은 [JsonElement]로 받아 [toVO]가 하나씩 따로 해석한다. 모르는 칸 하나가 목록 전체
 * 디코딩을 깨지 않게 하려는 것이다.
 */
@Serializable
data class ShopPageDTO(
    val pageKey: String? = null,
    val sduiComponents: List<JsonElement>? = null,
) {
    /**
     * 칸을 서버 순서대로 바꾼다. 모르는 `viewType`, 디코딩 실패, 필수 값(배너 그림·제목, 상품 id·이름·가격,
     * 헤더 제목) 누락은 그 칸만 버리고 [onDrop]에 이유를 남긴다.
     */
    fun toVO(json: Json, onDrop: (String) -> Unit): ShopRecommendPageVO = ShopRecommendPageVO(
        components = sduiComponents.orEmpty().mapIndexedNotNull { index, element ->
            val component = element as? JsonObject
            val viewType = component?.viewType()
            if (component == null || viewType !in VIEW_TYPES) {
                onDrop("SDUI #$index dropped: unknown viewType $viewType")
                return@mapIndexedNotNull null
            }
            val vo = runCatching { component.toComponentOrNull(viewType, json) }.getOrElse {
                onDrop("SDUI #$index ($viewType) dropped: decode failed ${it.message}")
                return@mapIndexedNotNull null
            }
            if (vo == null) onDrop("SDUI #$index ($viewType) dropped: missing required value")
            vo
        },
    )

    private fun JsonObject.toComponentOrNull(viewType: String?, json: Json): SduiComponentVO? {
        val content = this[KEY_CONTENT] ?: return null
        fun <T> decode(serializer: KSerializer<T>): T = json.decodeFromJsonElement(serializer, content)
        return when (viewType) {
            VIEW_HERO_BANNER -> decode(HeroBannerContentDTO.serializer()).toVOOrNull()
            VIEW_SECTION_HEADER -> decode(SectionHeaderContentDTO.serializer()).toVOOrNull()
            VIEW_ITEM_GRID -> decode(ShopItemsContentDTO.serializer()).itemsOrNull()?.let { SduiComponentVO.ItemGrid(it) }
            VIEW_RANKING -> decode(ShopItemsContentDTO.serializer()).itemsOrNull()?.let { SduiComponentVO.Ranking(it) }
            VIEW_ITEM_LIST -> decode(ShopItemsContentDTO.serializer()).itemsOrNull()?.let { SduiComponentVO.ItemList(it) }
            else -> null
        }
    }

    private fun JsonObject.viewType(): String? = runCatching { this[KEY_VIEW_TYPE]?.jsonPrimitive?.contentOrNull }.getOrNull()

    private companion object {
        const val KEY_VIEW_TYPE = "viewType"
        const val KEY_CONTENT = "content"
        const val VIEW_HERO_BANNER = "HeroBanner"
        const val VIEW_SECTION_HEADER = "SectionHeader"
        const val VIEW_ITEM_GRID = "ItemGrid"
        const val VIEW_RANKING = "Ranking"
        const val VIEW_ITEM_LIST = "ItemList"
        val VIEW_TYPES = setOf(VIEW_HERO_BANNER, VIEW_SECTION_HEADER, VIEW_ITEM_GRID, VIEW_RANKING, VIEW_ITEM_LIST)
    }
}

/** HeroBanner 칸 내용. 배너가 하나라도 그릴 수 없으면 칸을 버린다. */
@Serializable
data class HeroBannerContentDTO(
    val banners: List<ShopBannerDTO>? = null,
) {
    fun toVOOrNull(): SduiComponentVO.HeroBanner? {
        val banners = banners.orEmpty().takeIf { it.isNotEmpty() } ?: return null
        return SduiComponentVO.HeroBanner(banners.map { it.toVOOrNull() ?: return null })
    }
}

/**
 * SectionHeader 칸 내용.
 *
 * @property trailing 오른쪽 영역("전체보기 >", "9시간 후 바뀌어요")과 그 누름 동작. 없으면 null
 */
@Serializable
data class SectionHeaderContentDTO(
    val title: List<RichTextDTO>? = null,
    val trailing: SectionHeaderTrailingDTO? = null,
) {
    fun toVOOrNull(): SduiComponentVO.SectionHeader? {
        val title = title.toRichText().takeIf { it.isNotEmpty() } ?: return null
        return SduiComponentVO.SectionHeader(
            title = title,
            trailing = trailing?.content.toRichText(),
            trailingAction = trailing?.action?.toVOOrNull(),
        )
    }
}

@Serializable
data class SectionHeaderTrailingDTO(
    val content: List<RichTextDTO>? = null,
    val action: ShopActionDTO? = null,
)

/** ItemGrid·Ranking·ItemList 칸 내용. 상품 하나라도 필수 값이 빠지면(순위가 밀리므로) 칸을 버린다. */
@Serializable
data class ShopItemsContentDTO(
    val items: List<ShopItemDTO>? = null,
) {
    fun itemsOrNull(): List<ShopItemVO>? {
        val items = items.orEmpty().takeIf { it.isNotEmpty() } ?: return null
        return items.map { it.toVOOrNull() ?: return null }
    }
}
