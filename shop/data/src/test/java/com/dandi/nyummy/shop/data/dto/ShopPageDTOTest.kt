package com.dandi.nyummy.shop.data.dto

import com.dandi.nyummy.common.entity.tag.TagTone
import com.dandi.nyummy.common.entity.tag.TagVO
import com.dandi.nyummy.shop.entity.BannerCornerStyle
import com.dandi.nyummy.shop.entity.SduiComponentVO
import com.dandi.nyummy.shop.entity.ShopActionVO
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopPageDTOTest {

    /** 앱 NetworkModule과 같은 설정. */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }
    private val drops = mutableListOf<String>()

    private fun decode(components: String) =
        json.decodeFromString<ShopPageDTO>("""{"pageKey":"recommend","sduiComponents":[$components]}""")
            .toVO(json) { drops += it }
            .components

    private fun item(productId: Long = 1, name: String? = "발바닥 방석", extra: String = "") = """
        {"productId":$productId,"itemId":${productId + 100},${name?.let { "\"name\":\"$it\"," } ?: ""}
         "category":"RUG","categoryName":"러그","price":[{"text":"210","typeScale":"numberS"}],
         "isLimited":false,"ownedCount":0$extra}
    """

    private val banner = """
        {"bannerId":7,"imageUrl":"https://cdn/banner.png","cornerStyle":"ROUNDED",
         "title":[{"text":"가을 한정 모음"}],"description":[{"text":"단풍 가구"}],
         "price":[{"text":"30%","color":"contentDanger"}],
         "action":{"actionType":"navigate","navigateLink":"https://link.nyummy.co.kr/shop/promotions/7"}}
    """

    private fun header(title: String) = """
        {"viewType":"SectionHeader","content":{"title":[{"text":"$title"}],
         "trailing":{"content":[{"text":"전체보기"},{"icon":"chevronRight"}],
                     "action":{"actionType":"navigate","navigateLink":"https://link.nyummy.co.kr/shop/interior?sort=POPULAR"}}}}
    """

    @Test
    fun `서버 A 구성은 순서대로 모든 칸을 그린다`() {
        val components = decode(
            """
            {"viewType":"HeroBanner","content":{"banners":[$banner]}},
            ${header("인기 순위")},
            {"viewType":"Ranking","content":{"items":[${item(1)},${item(2, "플로어 조명")}]}},
            ${header("새로 들어왔어요")},
            {"viewType":"ItemList","content":{"items":[${item(3, "민트 체크 방")}]}},
            {"viewType":"ItemGrid","content":{"items":[${item(4, "단풍 러그")}]}}
            """,
        )

        assertEquals(
            listOf(
                SduiComponentVO.HeroBanner::class,
                SduiComponentVO.SectionHeader::class,
                SduiComponentVO.Ranking::class,
                SduiComponentVO.SectionHeader::class,
                SduiComponentVO.ItemList::class,
                SduiComponentVO.ItemGrid::class,
            ),
            components.map { it::class },
        )
        val hero = components[0] as SduiComponentVO.HeroBanner
        assertEquals(ShopActionVO.Navigate("https://link.nyummy.co.kr/shop/promotions/7"), hero.banners.single().action)
        val header = components[1] as SduiComponentVO.SectionHeader
        assertEquals(listOf("전체보기", ""), header.trailing.map { it.text })
        assertEquals(
            ShopActionVO.Navigate("https://link.nyummy.co.kr/shop/interior?sort=POPULAR"),
            header.trailingAction,
        )
        assertEquals(listOf("발바닥 방석", "플로어 조명"), (components[2] as SduiComponentVO.Ranking).items.map { it.name })
        assertTrue(drops.isEmpty())
    }

    @Test
    fun `모르는 칸은 그 칸만 버리고 로그를 남긴다`() {
        val components = decode(
            """
            {"viewType":"CountdownTimer","content":{"endsAt":"2026-11-03T00:00:00Z"}},
            ${header("인기 순위")},
            "not an object"
            """,
        )

        assertEquals(listOf(SduiComponentVO.SectionHeader::class), components.map { it::class })
        assertEquals(2, drops.size)
    }

    @Test
    fun `배너 하나라도 그림이 없으면 배너 칸을 버린다`() {
        val noImage = banner.replace(""""imageUrl":"https://cdn/banner.png",""", "")

        val components = decode("""{"viewType":"HeroBanner","content":{"banners":[$banner,$noImage]}}""")

        assertTrue(components.isEmpty())
        assertEquals(1, drops.size)
    }

    @Test
    fun `상품 하나라도 필수 값이 없으면 순위가 밀리지 않게 칸을 버린다`() {
        val components = decode(
            """
            {"viewType":"Ranking","content":{"items":[${item(1)},${item(2, name = null)}]}},
            {"viewType":"ItemList","content":{"items":[${item(3)}]}}
            """,
        )

        assertEquals(listOf(SduiComponentVO.ItemList::class), components.map { it::class })
    }

    @Test
    fun `제목 없는 헤더와 모양이 틀린 칸은 버린다`() {
        val components = decode(
            """
            {"viewType":"SectionHeader","content":{"title":[]}},
            {"viewType":"ItemGrid","content":{"items":"oops"}},
            {"viewType":"ItemList"}
            """,
        )

        assertTrue(components.isEmpty())
        assertEquals(3, drops.size)
    }

    @Test
    fun `가격이 없는 배너는 가격표 없이 그린다`() {
        val noPrice = banner.replace(""""price":[{"text":"30%","color":"contentDanger"}],""", "")
            .replace(""""cornerStyle":"ROUNDED"""", """"cornerStyle":"HEXAGON"""")

        val vo = json.decodeFromString<ShopBannerDTO>(noPrice).toVOOrNull()

        assertTrue(vo!!.price.isEmpty())
        assertEquals(BannerCornerStyle.ROUNDED, vo.cornerStyle)
    }

    @Test
    fun `상품은 서버 분류와 태그를 그대로 담고 모르는 태그 색은 프로모 색으로 그린다`() {
        val vo = json.decodeFromString<ShopItemDTO>(
            item(extra = ""","tag":{"text":"한정","tone":"GOLD"}""").replace("\"ownedCount\":0", "\"ownedCount\":-2"),
        ).toVOOrNull()!!

        assertEquals("RUG", vo.category)
        assertEquals("러그", vo.categoryName)
        assertEquals(TagVO("한정", TagTone.PROMO), vo.tag)
        assertEquals(0, vo.ownedCount)
    }

    @Test
    fun `가격이 없는 상품은 담을 수 없으므로 버린다`() {
        val noPrice = item().replace(""""price":[{"text":"210","typeScale":"numberS"}],""", "")

        assertNull(json.decodeFromString<ShopItemDTO>(noPrice).toVOOrNull())
    }

    @Test
    fun `상품 목록은 필수 값이 빠진 상품만 빼고 마지막 묶음이면 커서를 버린다`() {
        val page = json.decodeFromString<ShopItemsDTO>(
            """{"items":[${item(1)},${item(2, name = null)}],"nextCursor":99,"hasNext":false}""",
        ).toVO()

        assertEquals(listOf(1L), page.items.map { it.productId })
        assertNull(page.nextCursor)
    }

    @Test
    fun `모르는 동작이나 빈 링크는 누를 수 없다`() {
        assertEquals(ShopActionVO.ShowToast("곧 열려요"), ShopActionDTO("showToast", message = "곧 열려요").toVOOrNull())
        assertNull(ShopActionDTO("navigate", navigateLink = " ").toVOOrNull())
        assertNull(ShopActionDTO("openSheet", navigateLink = "https://link.nyummy.co.kr/shop").toVOOrNull())
    }
}
