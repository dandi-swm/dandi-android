package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
import com.dandi.nyummy.common.domain.helper.ImagePrefetchHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PrefetchCatAnimationsUseCaseTest {

    private val images = RecordingImagePrefetchHelper()

    private fun useCase(prefetch: () -> CatAnimationSetVO?) = PrefetchCatAnimationsUseCase(
        repository = object : CatRepository {
            override suspend fun getAnimations(): CatAnimationSetVO = error("미리 받기는 조회를 부르지 않는다")
            override suspend fun prefetchAnimations(): CatAnimationSetVO? = prefetch()
        },
        imagePrefetchHelper = images,
    )

    @Test
    fun `앱이 재생하는 기다림, 여유, 먹기, 놀이 상태의 시트를 모든 묶음까지 한 번씩 받는다`() = runBlocking {
        val set = animationSet(
            CatState.CONFLICTED_CAUTIOUS to listOf(listOf("wait-0a", "wait-0b"), listOf("wait-1", "shared")),
            CatState.RELAXED to listOf(listOf("relax-0"), listOf("shared")),
            CatState.CONTENT to listOf(listOf("eat-0")),
            CatState.PLAYFUL to listOf(listOf("play-0")),
            CatState.ANXIOUS to listOf(listOf("anxious-0")),
        )

        useCase { set }()

        assertEquals(
            listOf(listOf("wait-0a", "wait-0b", "wait-1", "shared", "relax-0", "eat-0", "play-0")),
            images.requests,
        )
    }

    @Test
    fun `상태가 없으면 화면처럼 기본 인사 상태의 시트를 받는다`() = runBlocking {
        val set = animationSet(
            CatState.RELAXED to listOf(listOf("relax-0")),
            CatState.FRIENDLY to listOf(listOf("hello-0")),
        )

        useCase { set }()

        assertEquals(listOf(listOf("hello-0", "relax-0")), images.requests)
    }

    @Test
    fun `로그인 전이라 애니메이션이 없으면 그림은 받지 않는다`() = runBlocking {
        useCase { null }()

        assertTrue(images.requests.isEmpty())
    }

    @Test
    fun `애니메이션을 받지 못해도 실패를 던지지 않고, 그림은 받지 않는다`() = runBlocking {
        useCase { throw IOException("offline") }()

        assertTrue(images.requests.isEmpty())
    }

    private fun animationSet(vararg states: Pair<CatState, List<List<String>>>) = CatAnimationSetVO(
        weight = "NORMAL",
        animations = states.associate { (state, groups) ->
            state to CatAnimationVO(
                state = state,
                frame = SpriteFrameVO(width = 136, height = 136, framesPerRow = 4, durationMs = 100),
                groups = groups.map { urls -> urls.map { SpriteClipVO(url = it, frames = 8) } },
                lines = emptyList(),
            )
        },
    )
}

private class RecordingImagePrefetchHelper : ImagePrefetchHelper {
    val requests = mutableListOf<List<String>>()

    override suspend fun prefetchSpriteSheets(urls: List<String>) {
        requests += urls
    }
}
