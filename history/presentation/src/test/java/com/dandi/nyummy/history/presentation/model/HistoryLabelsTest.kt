package com.dandi.nyummy.history.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryLabelsTest {

    @Test
    fun `끼니 순서는 첫 끼부터 열 번째까지 우리말로 쓰고 그 뒤는 숫자를 붙여 쓴다`() {
        assertEquals("첫 끼", mealOrderLabelOf(1))
        assertEquals("두 번째 끼니", mealOrderLabelOf(2))
        assertEquals("여섯 번째 끼니", mealOrderLabelOf(6))
        assertEquals("열 번째 끼니", mealOrderLabelOf(10))
        assertEquals("11번째 끼니", mealOrderLabelOf(11))
    }

    @Test
    fun `순번이 없으면 첫 끼로 쓴다`() {
        assertEquals("첫 끼", mealOrderLabelOf(0))
    }
}
