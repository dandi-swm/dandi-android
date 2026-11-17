package com.dandi.nyummy.common.presentation.designsystem.foundation

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** collectNyummySingleClicks는 실제 SystemClock을 읽어서 JVM 단위 테스트가 아니라 기기에서 돌린다. */
@RunWith(AndroidJUnit4::class)
class NyummySingleClickCollectTest {

    @Test
    fun collectNyummySingleClicks는_받은_람다를_실행한다() = runBlocking {
        var ran = 0
        collectNyummySingleClicks(flow { emit { ran++ } })
        assertEquals(1, ran)
    }

    @Test
    fun onClick이_CancellationException을_던져도_듣기를_멈추지_않는다() = runBlocking {
        val clicks = nyummySingleClickFlow()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { collectNyummySingleClicks(clicks) }

        clicks.emit { throw CancellationException("화면이 닫힘") }
        yield()

        assertTrue(job.isActive)
        job.cancel()
    }

    @Test
    fun onClick_안에서_듣는_코루틴이_취소되면_끝난다() = runBlocking {
        lateinit var job: Job
        var reachedAfterCollect = false
        // 값 하나를 흘리고 끝나는 흐름. ensureActive()가 없으면 예외를 삼키고 collect가 정상 반환해
        // 다음 줄까지 가 버린다. 있으면 그 자리에서 취소 예외가 다시 던져져 다음 줄에 닿지 않는다.
        val clicks = flow<() -> Unit> {
            emit {
                job.cancel()
                throw CancellationException("듣는 쪽이 취소됨")
            }
        }
        job = launch(start = CoroutineStart.LAZY) {
            collectNyummySingleClicks(clicks)
            reachedAfterCollect = true
        }

        job.join()

        assertTrue(job.isCancelled)
        assertFalse(reachedAfterCollect)
    }
}
