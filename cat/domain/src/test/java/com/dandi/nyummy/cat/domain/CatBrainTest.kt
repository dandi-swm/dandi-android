package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatLineVO
import com.dandi.nyummy.cat.entity.CatState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import kotlin.random.Random

class CatBrainTest {

    private val brain = CatBrain(Random(seed = 7))
    private val noon = LocalDateTime.of(2026, 10, 28, 12, 0)

    @Test
    fun `밤 11시부터 아침 6시 전까지는 밤이고, 낮에 오래 건드리지 않으면 졸린다`() {
        assertEquals(CatMood.NIGHT, brain.moodOf(noon.withHour(23)))
        assertEquals(CatMood.NIGHT, brain.moodOf(noon.withHour(5)))
        assertEquals(CatMood.DAY, brain.moodOf(noon.withHour(6)))
        assertEquals(CatMood.DROWSY, brain.moodOf(noon, idleMillis = 60_000))
    }

    @Test
    fun `분위기 상황이 비어 있으면 낮 동작으로 내려가고, 직전 동작은 연달아 고르지 않는다`() {
        val set = catAnimationSet(CatState.HOME_DAY)

        var previous: String? = null
        repeat(50) {
            val pick = brain.nextMotion(CatMood.NIGHT, set)!!
            assertEquals(CatState.HOME_DAY, pick.state)
            assertNotEquals(previous, pick.motion.name)
            previous = pick.motion.name
        }
    }

    @Test
    fun `반응 상황이 다 비어 있으면 반응 동작은 없다`() {
        assertNull(brain.reaction(CatReaction.GREET, catAnimationSet(CatState.HOME_DAY)))
        assertEquals(CatState.HOME_DAY, brain.reaction(CatReaction.TAP, catAnimationSet(CatState.HOME_DAY))?.state)
    }

    @Test
    fun `들어올 때 인사는 처음이거나 날짜가 바뀌면 인사, 하루 넘으면 반가움, 10분 안이면 없다`() {
        assertEquals(CatReaction.GREET, brain.greetingOf(null, noon))
        assertNull(brain.greetingOf(noon.minusMinutes(5), noon))
        assertNull(brain.greetingOf(noon.minusHours(2), noon))
        assertEquals(CatReaction.GREET, brain.greetingOf(noon.minusHours(13), noon))
        assertEquals(CatReaction.WELCOME_BACK, brain.greetingOf(noon.minusDays(2), noon))
    }

    @Test
    fun `반응 대사는 동작에 맞춘 대사, 반응 대사, 일상 대사 순서로 고른다`() {
        val lines = listOf(
            CatLineVO("콕", setOf("tap:poke")),
            CatLineVO("왜", setOf("tap")),
            CatLineVO("낮이다", setOf("day")),
        )
        val poke = catMotion("poke")

        assertEquals("콕", brain.reactionLine(CatReaction.TAP, noon, lines, poke))
        assertEquals("왜", brain.reactionLine(CatReaction.TAP, noon, lines, catMotion("other")))
        assertEquals("낮이다", brain.reactionLine(CatReaction.GREET, noon, lines))
    }
}
