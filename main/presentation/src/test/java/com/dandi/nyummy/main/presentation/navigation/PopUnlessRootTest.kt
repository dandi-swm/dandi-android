package com.dandi.nyummy.main.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class PopUnlessRootTest {

    @Test
    fun `화면이 둘 이상이면 맨 위 하나를 닫는다`() {
        val backStack = mutableListOf("home", "meal/record")

        backStack.popUnlessRoot()

        assertEquals(listOf("home"), backStack)
    }

    @Test
    fun `루트 하나만 남았으면 뒤로 신호가 와도 지우지 않는다`() {
        // 홈에서 공통 404 다이얼로그가 뒤로를 보내도 빈 백스택이 되면 안 된다.
        val backStack = mutableListOf("home")

        backStack.popUnlessRoot()

        assertEquals(listOf("home"), backStack)
    }

    @Test
    fun `빈 백스택에서도 예외 없이 그대로 둔다`() {
        val backStack = mutableListOf<String>()

        backStack.popUnlessRoot()

        assertEquals(emptyList<String>(), backStack)
    }
}
