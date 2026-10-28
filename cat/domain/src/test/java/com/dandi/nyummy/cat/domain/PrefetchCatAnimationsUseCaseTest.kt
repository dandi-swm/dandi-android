package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class PrefetchCatAnimationsUseCaseTest {

    private var refreshCount = 0

    private fun useCase(refresh: () -> Boolean) = PrefetchCatAnimationsUseCase(
        repository = object : CatRepository {
            override suspend fun getAnimations(): CatAnimationSetVO = error("미리 받기는 애니메이션을 읽지 않는다")
            override suspend fun refreshWeight(): Boolean {
                refreshCount++
                return refresh()
            }
        },
    )

    @Test
    fun `체형을 한 번 새로 읽는다`() = runBlocking {
        useCase { true }()

        assertEquals(1, refreshCount)
    }

    @Test
    fun `체형을 읽지 못해도 실패를 던지지 않는다`() = runBlocking {
        useCase { throw IOException("offline") }()
        useCase { throw httpException(500) }()

        assertEquals(2, refreshCount)
    }

    @Test(expected = CancellationException::class)
    fun `취소는 삼키지 않고 그대로 던진다`(): Unit = runBlocking {
        useCase { throw CancellationException("left") }()
    }
}
