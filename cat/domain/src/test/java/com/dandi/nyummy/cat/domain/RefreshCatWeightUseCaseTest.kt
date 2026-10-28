package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RefreshCatWeightUseCaseTest {

    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()

    private fun useCase(refresh: () -> Boolean) = RefreshCatWeightUseCase(
        repository = object : CatRepository {
            override suspend fun getAnimations(): CatAnimationSetVO = CatAnimationSetVO.empty
            override suspend fun refreshWeight(): Boolean = refresh()
        },
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper,
    )

    @Test
    fun `체형이 바뀌었는지 그대로 돌려준다`() = runBlocking {
        assertTrue(useCase { true }())
        assertFalse(useCase { false }())
    }

    @Test
    fun `서버나 네트워크 오류는 알리지 않고 false다`() = runBlocking {
        assertFalse(useCase { throw httpException(500) }())
        assertFalse(useCase { throw IOException("offline") }())

        assertTrue(messageHelper.dialogs.isEmpty())
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `로그인이 만료됐으면 세션 만료 안내 후 처음 화면으로 보낸다`() = runBlocking {
        assertFalse(useCase { throw httpException(401) }())

        messageHelper.dialogs.single().onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
    }
}
