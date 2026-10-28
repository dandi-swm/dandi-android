package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class GetCatAnimationsUseCaseTest {

    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()

    private fun useCase(block: () -> CatAnimationSetVO) = GetCatAnimationsUseCase(
        repository = object : CatRepository {
            override suspend fun getAnimations(): CatAnimationSetVO = block()
            override suspend fun refreshWeight(): Boolean = error("조회는 체형을 다시 읽지 않는다")
        },
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper,
    )

    private fun assertNothingShown() {
        assertTrue(messageHelper.dialogs.isEmpty())
        assertTrue(messageHelper.snackBars.isEmpty())
        assertEquals(0, navigationHelper.backCount)
        assertEquals(0, navigationHelper.initialCount)
    }

    @Test
    fun `애니메이션이 있으면 돌려준다`() = runBlocking {
        val animations = catAnimationSet(CatState.HOME_DAY)

        val result = useCase { animations }()

        assertEquals(Result.success(animations), result)
        assertNothingShown()
    }

    @Test
    fun `앱 에셋에 애니메이션이 하나도 없으면 조용히 실패한다`() = runBlocking {
        val result = useCase { CatAnimationSetVO.empty }()

        assertTrue(result.isFailure)
        assertNothingShown()
    }

    @Test
    fun `읽다가 실패해도 아무것도 띄우지 않고 실패만 돌려준다`() = runBlocking {
        assertTrue(useCase { throw IOException("asset") }().isFailure)
        assertTrue(useCase { throw httpException(401) }().isFailure)
        assertNothingShown()
    }

    @Test(expected = CancellationException::class)
    fun `취소는 삼키지 않고 그대로 던진다`(): Unit = runBlocking {
        useCase { throw CancellationException("left home") }()
        Unit
    }
}

internal fun httpException(code: Int, errorCode: String? = null) = HttpResponseException(
    status = HttpResponseStatus.create(code),
    rawCode = code,
    errorRequestUrl = "https://test/api/v1/cats",
    msg = "Http Request Failed ($code)",
    cause = errorCode?.let(::Throwable),
)
