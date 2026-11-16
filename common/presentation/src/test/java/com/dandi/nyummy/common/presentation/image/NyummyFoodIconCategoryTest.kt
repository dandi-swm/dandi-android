package com.dandi.nyummy.common.presentation.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** 음식 아이콘 분류 표가 앱에 든 아이콘을 빠짐없이, 겹치지 않게 나누는지 확인합니다. */
class NyummyFoodIconCategoryTest {

    @Test
    fun `앱에 든 모든 아이콘이 정확히 한 분류에 속한다`() {
        val all = NyummyFoodIconCategory.allIconIds
        val byCategory = NyummyFoodIconCategory.entries.flatMap { NyummyFoodIconCategory.iconIdsOf(it) }

        assertEquals(NyummyFoodIcons.MaxIconId, all.size)
        assertEquals(all.size, byCategory.size)
        assertEquals(all.toSet(), byCategory.toSet())
    }

    @Test
    fun `분류별 아이콘 수는 정해 둔 표와 같다`() {
        val counts = NyummyFoodIconCategory.entries.associateWith { NyummyFoodIconCategory.iconIdsOf(it).size }

        assertEquals(
            mapOf(
                NyummyFoodIconCategory.MAIN to 61,
                NyummyFoodIconCategory.SIDE to 77,
                NyummyFoodIconCategory.FRUIT to 12,
                NyummyFoodIconCategory.SNACK to 35,
                NyummyFoodIconCategory.DRINK to 25,
            ),
            counts,
        )
    }

    @Test
    fun `분류에 든 아이콘은 모두 앱에 리소스가 있다`() {
        NyummyFoodIconCategory.entries.forEach { category ->
            NyummyFoodIconCategory.iconIdsOf(category).forEach { iconId ->
                assertNotNull("$category $iconId", NyummyFoodIcons.drawableOf(iconId))
            }
        }
    }

    @Test
    fun `대표적인 아이콘의 분류`() {
        assertEquals(NyummyFoodIconCategory.MAIN, NyummyFoodIconCategory.of("5")) // 흰쌀밥
        assertEquals(NyummyFoodIconCategory.SIDE, NyummyFoodIconCategory.of("15")) // 된장찌개
        assertEquals(NyummyFoodIconCategory.FRUIT, NyummyFoodIconCategory.of("25")) // 사과
        assertEquals(NyummyFoodIconCategory.SNACK, NyummyFoodIconCategory.of("9")) // 케이크
        assertEquals(NyummyFoodIconCategory.DRINK, NyummyFoodIconCategory.of("6")) // 아이스 아메리카노
    }

    @Test
    fun `모르는 번호는 반찬으로 본다`() {
        assertEquals(NyummyFoodIconCategory.SIDE, NyummyFoodIconCategory.of("9999"))
        assertEquals(NyummyFoodIconCategory.SIDE, NyummyFoodIconCategory.of(""))
        assertEquals(NyummyFoodIconCategory.SIDE, NyummyFoodIconCategory.of("rice"))
    }
}
