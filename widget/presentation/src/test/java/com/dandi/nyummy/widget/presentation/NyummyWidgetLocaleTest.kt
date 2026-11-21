package com.dandi.nyummy.widget.presentation

import com.dandi.nyummy.widget.entity.WidgetCardVO
import com.dandi.nyummy.widget.entity.WidgetPlanVO
import com.dandi.nyummy.widget.entity.WidgetStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 한국어가 아닌 언어에서 위젯이 한국어 그림을 보이지 않는지 본다.
 * - 그림 속 글자가 앱 언어가 아니면(widget_art_text_localized = false, 지금은 한국어·일본어 말고) 글자가 든 시안을 고르지 않는다.
 * - 미리보기 레이아웃은 언어마다 그림과 글자 중 하나만 보인다(values·values-ko·values-ja의 integers.xml).
 */
class NyummyWidgetLocaleTest {

    @Test
    fun `그림에 글자가 든 시안은 6일째 현수막 하나다`() {
        val withText = allCards(imageTextLocalized = true).filter { it.imageHasText }

        assertEquals(listOf(R.drawable.widget_nyummy_away6_b), withText.map { it.image })
    }

    @Test
    fun `그림 글자가 앱 언어가 아니면 글자가 든 시안을 빼고, 모든 상태에 시안이 남는다`() {
        allPlans.forEach { plan ->
            val cards = NyummyWidgetCatalog.cardsFor(plan, imageTextLocalized = false)

            assertTrue("$plan", cards.isNotEmpty())
            assertTrue("$plan", cards.none { it.imageHasText })
            assertEquals(cards.size, NyummyWidgetCatalog.variantCount(plan, imageTextLocalized = false))
        }
        assertEquals(
            listOf(R.drawable.widget_nyummy_away6_a, R.drawable.widget_nyummy_away6_c),
            NyummyWidgetCatalog.cardsFor(away(6), imageTextLocalized = false).map { it.image },
        )
    }

    @Test
    fun `그림 글자가 앱 언어면 시안 목록이 그대로다`() {
        allPlans.forEach { plan ->
            assertEquals(NyummyWidgetCatalog.cardsFor(plan), NyummyWidgetCatalog.cardsFor(plan, imageTextLocalized = true))
        }
        assertEquals(3, NyummyWidgetCatalog.variantCount(away(6), imageTextLocalized = true))
    }

    @Test
    fun `한국어로 고른 현수막 시안 번호는 다른 언어에서 남은 시안으로 맞춘다`() {
        val card = WidgetCardVO(away(6), variant = 1)

        assertEquals(R.drawable.widget_nyummy_away6_b, NyummyWidgetCatalog.cardFor(card, imageTextLocalized = true).image)
        assertEquals(R.drawable.widget_nyummy_away6_c, NyummyWidgetCatalog.cardFor(card, imageTextLocalized = false).image)
    }

    @Test
    fun `글자가 든 그림 스위치는 그 언어 그림이 있는 한국어·일본어만 켜져 있다`() {
        assertEquals("false", resourceValue("values/bools.xml", "bool", "widget_art_text_localized"))
        assertEquals("true", resourceValue("values-ko/bools.xml", "bool", "widget_art_text_localized"))
        assertEquals("true", resourceValue("values-ja/bools.xml", "bool", "widget_art_text_localized"))
        assertTrue(projectFile("$RES/drawable-ja-nodpi/widget_nyummy_away6_b.webp").isFile)
    }

    @Test
    fun `미리보기 보임 값은 visibility 열거 순번이고 그림과 글자 중 하나만 보인다`() {
        listOf("values", "values-ko", "values-ja").forEach { dir ->
            val image = resourceValue("$dir/integers.xml", "integer", "widget_preview_image_visibility").toInt()
            val text = resourceValue("$dir/integers.xml", "integer", "widget_preview_text_visibility").toInt()

            // android:visibility는 0 visible, 1 invisible, 2 gone이다. View.GONE(8)을 쓰면 런처가 레이아웃을 읽다 죽는다.
            assertTrue("$dir image=$image", image in VISIBILITY_ORDINALS)
            assertTrue("$dir text=$text", text in VISIBILITY_ORDINALS)
            assertEquals("$dir: 하나만 보여야 한다", setOf(VISIBLE, GONE), setOf(image, text))
        }
        // 그림은 한국어·일본어판만 있다. 기본(영어)은 글자로 그린다.
        assertEquals("0", resourceValue("values-ko/integers.xml", "integer", "widget_preview_image_visibility"))
        assertEquals("0", resourceValue("values-ja/integers.xml", "integer", "widget_preview_image_visibility"))
        assertEquals("0", resourceValue("values/integers.xml", "integer", "widget_preview_text_visibility"))
        listOf("widget_preview_copy.png", "widget_preview_streak.png").forEach { name ->
            assertTrue(name, projectFile("$RES/drawable-ja-xxhdpi/$name").isFile)
        }
    }

    @Test
    fun `위젯 정보는 미리보기 레이아웃을 쓴다`() {
        val info = projectFile("widget/presentation/src/main/res/xml/nyummy_widget_info.xml").readText()

        assertTrue(info.contains("android:initialLayout=\"@layout/widget_nyummy_preview\""))
        assertTrue(info.contains("android:previewLayout=\"@layout/widget_nyummy_preview\""))
        assertFalse(
            "실제 위젯 레이아웃은 한국어 미리보기 그림을 참조하지 않는다",
            projectFile("widget/presentation/src/main/res/layout/widget_nyummy.xml").readText().contains("widget_preview_"),
        )
    }

    private fun away(days: Int) = WidgetPlanVO(stage = WidgetStage.AWAY, awayDays = days)

    private val allPlans: List<WidgetPlanVO> = WidgetStage.entries.flatMap { stage ->
        if (stage == WidgetStage.AWAY) (1..10).map(::away) else listOf(WidgetPlanVO(stage = stage))
    }

    private fun allCards(imageTextLocalized: Boolean): List<NyummyWidgetCard> =
        WidgetStage.entries.filterNot { it == WidgetStage.AWAY }
            .flatMap { NyummyWidgetCatalog.cardsFor(WidgetPlanVO(stage = it), imageTextLocalized) } +
            (1..7).flatMap { NyummyWidgetCatalog.cardsFor(away(it), imageTextLocalized) }

    private companion object {
        const val RES = "widget/presentation/src/main/res"
        const val VISIBLE = 0
        const val GONE = 2
        val VISIBILITY_ORDINALS = 0..2

        /** 위젯 모듈 res 폴더의 [path] 파일에서 [tag] 이름이 [name]인 값. */
        fun resourceValue(path: String, tag: String, name: String): String {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(projectFile("$RES/$path"))
            val nodes = document.getElementsByTagName(tag)
            return (0 until nodes.length).map { nodes.item(it) as Element }
                .single { it.getAttribute("name") == name }
                .textContent.trim()
        }

        /** 저장소 루트(settings.gradle.kts가 있는 곳) 기준 경로. 테스트 작업 디렉터리는 모듈 폴더다. */
        fun projectFile(path: String): File {
            var dir: File? = File(System.getProperty("user.dir")).absoluteFile
            while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
            return File(requireNotNull(dir) { "저장소 루트를 찾지 못했다" }, path)
        }
    }
}
