package com.dandi.nyummy.cat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CatMotionPickerTest {

    private val picker = CatMotionPicker(Random(seed = 7))

    @Test
    fun `동작 묶음이 둘 이상이면 바로 전 묶음은 다시 고르지 않는다`() {
        var previous = 0
        repeat(200) {
            val next = picker.nextGroup(count = 3, previous = previous)
            assertNotEquals(previous, next)
            assertTrue(next in 0..2)
            previous = next
        }
    }

    @Test
    fun `동작 묶음이 하나면 그 묶음을 다시 고른다`() {
        assertEquals(0, picker.nextGroup(count = 1, previous = 0))
    }

    @Test
    fun `동작 묶음이 없으면 0을 돌려준다`() {
        assertEquals(0, picker.nextGroup(count = 0, previous = null))
    }

    @Test
    fun `여러 번 고르면 모든 묶음이 한 번씩은 나온다`() {
        val picked = (1..200).map { picker.nextGroup(count = 3, previous = null) }.toSet()
        assertEquals(setOf(0, 1, 2), picked)
    }

    @Test
    fun `대사가 둘 이상이면 바로 전 대사는 다시 고르지 않는다`() {
        val lines = listOf("냠냠", "골골", "맛있다")
        var previous: String? = null
        repeat(200) {
            val next = picker.nextLine(lines, previous)
            assertNotEquals(previous, next)
            previous = next
        }
    }

    @Test
    fun `대사가 하나면 그 대사를 다시 고르고, 없으면 null이다`() {
        assertEquals("냠냠", picker.nextLine(listOf("냠냠"), previous = "냠냠"))
        assertNull(picker.nextLine(emptyList(), previous = null))
    }

    @Test
    fun `같은 대사가 여러 번 들어 있어도 바로 전 대사는 피하고, 모두 같으면 그 대사를 고른다`() {
        repeat(50) {
            assertEquals("골골", picker.nextLine(listOf("냠냠", "냠냠", "골골"), previous = "냠냠"))
        }
        assertEquals("냠냠", picker.nextLine(listOf("냠냠", "냠냠"), previous = "냠냠"))
    }

    @Test
    fun `쉬는 시간은 6초와 12초를 모두 포함한다`() {
        val lowest = CatMotionPicker(EdgeRandom(pickLowest = true))
        val highest = CatMotionPicker(EdgeRandom(pickLowest = false))

        assertEquals(CatMotionPicker.MIN_REST_MILLIS, lowest.restMillis())
        assertEquals(CatMotionPicker.MAX_REST_MILLIS, highest.restMillis())
    }

    @Test
    fun `쉬는 시간은 6초에서 12초 사이다`() {
        repeat(200) {
            assertTrue(picker.restMillis() in CatMotionPicker.MIN_REST_MILLIS..CatMotionPicker.MAX_REST_MILLIS)
        }
    }

    /** 범위의 가장 작은 값이나 가장 큰 값만 돌려주는 난수. */
    private class EdgeRandom(private val pickLowest: Boolean) : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextLong(from: Long, until: Long): Long = if (pickLowest) from else until - 1
    }
}
