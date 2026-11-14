package com.dandi.nyummy.common.presentation.image

import com.dandi.nyummy.common.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NyummyFoodIconsTest {

    @Test
    fun `서버 iconId와 같은 번호의 리소스를 고른다`() {
        assertEquals(R.drawable.nyummy_food_icon_1, NyummyFoodIcons.drawableOf("1"))
        assertEquals(R.drawable.nyummy_food_icon_12, NyummyFoodIcons.drawableOf("12"))
        assertEquals(R.drawable.nyummy_food_icon_210, NyummyFoodIcons.drawableOf("210"))
    }

    @Test
    fun `1부터 최대 번호까지 빈 번호 없이 서로 다른 리소스가 있다`() {
        val drawables = (1..NyummyFoodIcons.MaxIconId).map { NyummyFoodIcons.drawableOf(it.toString()) }
        assertEquals(NyummyFoodIcons.MaxIconId, drawables.filterNotNull().toSet().size)
    }

    @Test
    fun `앱에 없는 번호나 숫자가 아닌 값은 리소스가 없다`() {
        assertNull(NyummyFoodIcons.drawableOf(""))
        assertNull(NyummyFoodIcons.drawableOf("0"))
        assertNull(NyummyFoodIcons.drawableOf("-1"))
        assertNull(NyummyFoodIcons.drawableOf("${NyummyFoodIcons.MaxIconId + 1}"))
        assertNull(NyummyFoodIcons.drawableOf("rice"))
    }
}
