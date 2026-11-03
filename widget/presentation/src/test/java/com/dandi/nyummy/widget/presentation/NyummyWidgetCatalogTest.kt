package com.dandi.nyummy.widget.presentation

import com.dandi.nyummy.widget.entity.WidgetCardVO
import com.dandi.nyummy.widget.entity.WidgetPlanVO
import com.dandi.nyummy.widget.entity.WidgetStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.awt.Font
import java.awt.font.FontRenderContext
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 위젯 시안 목록이 피그마 `Widget · 20 사용 시안 · 상태별`과 문구 규칙을 지키는지 본다.
 * 문구는 리소스 파일(strings.xml)에서 읽고, 까투리 글자 판정은 앱의 `AccentGlyphCoverage`와 같은 규칙으로 JVM 글꼴로 한다.
 */
class NyummyWidgetCatalogTest {

    @Test
    fun `모든 상태와 일 단위 날수에 시안이 하나 이상 있다`() {
        allPlans.forEach { plan ->
            val cards = NyummyWidgetCatalog.cardsFor(plan)
            assertTrue("$plan", cards.isNotEmpty())
            assertEquals(cards.size, NyummyWidgetCatalog.variantCount(plan))
        }
    }

    @Test
    fun `시안 수는 보드와 같은 39장이고 그림이 겹치지 않는다`() {
        val counts = WidgetStage.entries.filterNot { it == WidgetStage.AWAY }
            .associateWith { NyummyWidgetCatalog.variantCount(WidgetPlanVO(stage = it)) }

        assertEquals(
            mapOf(
                WidgetStage.NIGHT to 3,
                WidgetStage.WAITING to 4,
                WidgetStage.FULL to 4,
                WidgetStage.MISSED_ONE to 6,
                WidgetStage.MISSED_TWO to 5,
                WidgetStage.MISSED_ALL to 4,
                WidgetStage.REUNION to 1,
                WidgetStage.LOGGED_OUT to 2,
            ),
            counts,
        )
        assertEquals(listOf(1, 1, 1, 1, 2, 3, 1), (1..7).map { NyummyWidgetCatalog.variantCount(away(it)) })
        assertEquals(39, allCards.size)
        assertEquals(39, allCards.map { it.image }.toSet().size)
    }

    @Test
    fun `7일 이상은 모두 망부석이다`() {
        val stone = NyummyWidgetCatalog.cardsFor(away(7))

        (8..30).forEach { days -> assertEquals(stone, NyummyWidgetCatalog.cardsFor(away(days))) }
    }

    @Test
    fun `톤 확인이 필요했던 컵과 난장판 시안은 없다`() {
        val texts = allCards.flatMap(::rawTexts) + strings.values

        assertFalse(texts.any { "떨어뜨린다" in it })
        assertFalse(texts.any { "엎었어" in it })
    }

    @Test
    fun `배빵빵 제목에 끼니 수 같은 숫자가 없다`() {
        NyummyWidgetCatalog.cardsFor(WidgetPlanVO(stage = WidgetStage.FULL)).forEach { card ->
            assertFalse(text(card.title), text(card.title).any(Char::isDigit))
        }
    }

    @Test
    fun `재회 문구는 지난 날수를 말하지 않는다`() {
        val reunion = NyummyWidgetCatalog.cardsFor(WidgetPlanVO(stage = WidgetStage.REUNION)).single()

        assertEquals("왔다!!!", text(reunion.title))
        assertEquals("집사다, 집사!", text(reunion.sub))
        assertFalse(reunion.showsAwayDays)
        rawTexts(reunion).forEach { copy ->
            assertFalse(copy, copy.any(Char::isDigit) || "%" in copy || "만에" in copy || "만이" in copy)
        }
    }

    @Test
    fun `끼니 이름은 밤·기다림·한 끼 놓침 문구에만 들어간다`() {
        val withMeal = setOf(WidgetStage.NIGHT, WidgetStage.WAITING, WidgetStage.MISSED_ONE)

        allPlans.filterNot { it.stage in withMeal }.forEach { plan ->
            NyummyWidgetCatalog.cardsFor(plan).flatMap(::rawTexts).forEach { copy ->
                assertFalse("${plan.stage}: $copy", "%1\$s" in copy)
            }
        }
    }

    @Test
    fun `일 단위 설명에는 날수가 없고 날수 접두는 따로 붙인다`() {
        assertEquals("%1\$d일째 %2\$s", strings.getValue("widget_away_days_prefix"))
        (1..7).flatMap { NyummyWidgetCatalog.cardsFor(away(it)) }.forEach { card ->
            val rest = text(card.sub)
            assertFalse(rest, "일째" in rest || "%" in rest)
        }
    }

    @Test
    fun `끼니 시각이 지나면 바꿔 쓰는 문구는 손목시계 기다림에만 있다`() {
        val switching = allCards.filter { it.subAfterMealTime != 0 }

        assertEquals(1, switching.size)
        assertEquals("%1\$s까지 1시간", text(switching.single().sub))
        assertEquals("%1\$s 시간이에요", text(switching.single().subAfterMealTime))
    }

    @Test
    fun `범위를 벗어난 시안 번호는 끝 시안으로 맞춘다`() {
        val plan = WidgetPlanVO(stage = WidgetStage.NIGHT)
        val cards = NyummyWidgetCatalog.cardsFor(plan)

        assertEquals(cards.last(), NyummyWidgetCatalog.cardFor(WidgetCardVO(plan, variant = 99)))
        assertEquals(cards.first(), NyummyWidgetCatalog.cardFor(WidgetCardVO(plan, variant = -1)))
    }

    @Test
    fun `글자 판정은 까투리에 없는 글자를 찾아낸다`() {
        assertTrue(KaturiCoverage.covers("배 빵빵!"))
        assertFalse(KaturiCoverage.covers("똠"))
        assertFalse(KaturiCoverage.covers("집사 ☆ Ω"))
    }

    @Test
    fun `모든 위젯 문구를 까투리로 그릴 수 있다`() {
        val missing = renderedTexts().filterNot(KaturiCoverage::covers)

        assertTrue("까투리에 없는 글자가 든 문구: $missing", missing.isEmpty())
    }

    /** 위젯에 실제로 그려질 수 있는 문구 전부. 끼니 이름과 "N일째 "를 끼워 넣은 꼴이다. */
    private fun renderedTexts(): List<String> {
        val meals = listOf("widget_meal_breakfast", "widget_meal_lunch", "widget_meal_dinner").map(strings::getValue)
        val prefix = strings.getValue("widget_away_days_prefix")
        val streak = strings.getValue("widget_streak_days")
        return allCards.flatMap { card ->
            val copies = rawTexts(card).flatMap { raw -> meals.map { meal -> raw.replace("%1\$s", meal) } }
            val withDays = if (card.showsAwayDays) {
                (1..6).map { days -> prefix.replace("%1\$d", "$days").replace("%2\$s", text(card.sub)) }
            } else {
                emptyList()
            }
            copies + withDays
        } + (0..400).map { days -> streak.replace("%1\$d", "$days") } +
            listOf(strings.getValue("widget_label"), strings.getValue("widget_description"))
    }

    private fun rawTexts(card: NyummyWidgetCard): List<String> =
        listOfNotNull(card.title, card.sub, card.subAfterMealTime.takeIf { it != 0 }).map(::text)

    private fun text(res: Int): String = strings.getValue(stringNames.getValue(res))

    private fun away(days: Int) = WidgetPlanVO(stage = WidgetStage.AWAY, awayDays = days)

    private val allPlans: List<WidgetPlanVO> = WidgetStage.entries.flatMap { stage ->
        if (stage == WidgetStage.AWAY) (1..10).map(::away) else listOf(WidgetPlanVO(stage = stage))
    }

    private val allCards: List<NyummyWidgetCard> =
        WidgetStage.entries.filterNot { it == WidgetStage.AWAY }
            .flatMap { NyummyWidgetCatalog.cardsFor(WidgetPlanVO(stage = it)) } +
            (1..7).flatMap { NyummyWidgetCatalog.cardsFor(away(it)) }

    /**
     * 앱의 `AccentGlyphCoverage`와 같은 규칙: 글자만 보고, 영문 대소문자는 있다고 보고, 한글은 모양이 실제로
     * 찍히는지로 판정하며, 그 밖의 글자는 없다고 본다. 이 글꼴은 한글 11,172자를 문자표에 올려 두고
     * 2,350자에만 모양을 넣어서 문자표만 보면 늘 "있다"고 나온다.
     */
    private object KaturiCoverage {
        private val font: Font by lazy {
            System.setProperty("java.awt.headless", "true")
            Font.createFont(Font.TRUETYPE_FONT, projectFile("common/presentation/src/main/res/font/andong_katuri_regular.ttf"))
                .deriveFont(48f)
        }
        private val context = FontRenderContext(null, true, true)

        fun covers(text: String): Boolean = text.codePoints().allMatch { codePoint ->
            !Character.isLetter(codePoint) || hasGlyph(codePoint)
        }

        private fun hasGlyph(codePoint: Int): Boolean = when (codePoint) {
            in 'A'.code..'Z'.code, in 'a'.code..'z'.code -> true
            in 0xAC00..0xD7A3, in 0x3131..0x318E ->
                !font.createGlyphVector(context, String(Character.toChars(codePoint))).outline.bounds2D.isEmpty
            else -> false
        }
    }

    private companion object {
        /** 이 모듈의 strings.xml. 이름 → 문구(`\n` 등은 풀어 둔다). */
        val strings: Map<String, String> by lazy {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(projectFile("widget/presentation/src/main/res/values/strings.xml"))
            val nodes = document.getElementsByTagName("string")
            (0 until nodes.length).map { nodes.item(it) as Element }.associate { element ->
                element.getAttribute("name") to element.textContent
                    .replace("\\n", "\n")
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
            }
        }

        /** 리소스 id → 이름. */
        val stringNames: Map<Int, String> by lazy {
            R.string::class.java.fields.associate { it.getInt(null) to it.name }
        }

        /** 저장소 루트(settings.gradle.kts가 있는 곳) 기준 경로. 테스트 작업 디렉터리는 모듈 폴더다. */
        fun projectFile(path: String): File {
            var dir: File? = File(System.getProperty("user.dir")).absoluteFile
            while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
            return File(requireNotNull(dir) { "저장소 루트를 찾지 못했다" }, path)
        }
    }
}
