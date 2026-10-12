package com.dandi.nyummy.main.presentation.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

/** 탭 화면은 인자와 상관없이 path 하나에 엔트리 하나만 두는지 확인합니다. */
class BringTabToFrontTest {

    private val home = GenericNavKey("/home")
    private val settings = GenericNavKey("/settings")
    private val history = GenericNavKey("/history")
    private fun historyOpening(mealId: String) = GenericNavKey("/history", mapOf("mealId" to mealId))

    @Test
    fun `인자 없이 오면 이미 있는 탭 엔트리를 인자째 맨 위로 올린다`() {
        val stack = mutableListOf<NavKey>(home, historyOpening("1"), settings)

        stack.bringTabToFront(history)

        assertEquals(listOf(home, settings, historyOpening("1")), stack)
    }

    @Test
    fun `인자와 함께 오면 같은 탭 엔트리를 새 인자로 바꿔 하나만 둔다`() {
        val stack = mutableListOf<NavKey>(home, history, settings)

        stack.bringTabToFront(historyOpening("2"))

        assertEquals(listOf(home, settings, historyOpening("2")), stack)
    }

    @Test
    fun `맨 위가 이미 그 탭이면 인자 없이 와도 그대로 둔다`() {
        val stack = mutableListOf<NavKey>(home, historyOpening("1"))

        stack.bringTabToFront(history)

        assertEquals(listOf(home, historyOpening("1")), stack)
    }

    @Test
    fun `탭 엔트리가 없으면 맨 위에 추가한다`() {
        val stack = mutableListOf<NavKey>(home)

        stack.bringTabToFront(history)

        assertEquals(listOf(home, history), stack)
    }
}
