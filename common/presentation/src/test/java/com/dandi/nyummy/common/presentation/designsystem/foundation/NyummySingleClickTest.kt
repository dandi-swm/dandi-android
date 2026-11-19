package com.dandi.nyummy.common.presentation.designsystem.foundation

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NyummySingleClickTest {

    /** (값, 들어온 시각ms) 쌍을 차례로 흘리고, 가짜 시계가 그 시각을 돌려준다. 실제로 기다리지 않는다. */
    private fun throttled(vararg presses: Pair<String, Long>): List<String> = runBlocking {
        var clock = 0L
        flow {
            presses.forEach { (value, at) ->
                clock = at
                emit(value)
            }
        }.throttleFirst(NyummySingleClickPeriodMillis, now = { clock }).toList()
    }

    @Test
    fun `첫 값은 바로 나온다`() {
        // 시각 0: 기준 시각을 null 대신 0으로 잘못 시작하면 0 - 0 < 400이라 버려져서 잡힌다.
        assertEquals(listOf("a"), throttled("a" to 0L))
    }

    @Test
    fun `399ms 뒤에 들어온 값은 버려진다`() {
        assertEquals(listOf("a"), throttled("a" to 0L, "b" to 399L))
    }

    @Test
    fun `정확히 400ms 뒤에 들어온 값은 나온다`() {
        assertEquals(listOf("a", "b"), throttled("a" to 0L, "b" to 400L))
    }

    @Test
    fun `버려진 값은 기준 시각을 바꾸지 않는다`() {
        assertEquals(listOf("a", "c"), throttled("a" to 0L, "b" to 300L, "c" to 500L))
    }

    @Test
    fun `받는 쪽이 꺼내 가기 전에 두 번 넣으면 먼저 넣은 것만 전달된다`() = runBlocking {
        val clicks = nyummySingleClickFlow()
        val received = mutableListOf<String>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { clicks.collect { it() } }

        clicks.tryEmit { received += "first" }
        // DROP_LATEST는 넘쳐도 true를 돌려준다. SUSPEND로 바뀌면 false라 여기서 잡힌다.
        assertTrue(clicks.tryEmit { received += "second" })
        yield()

        assertEquals(listOf("first"), received)
        job.cancel()
    }
}
