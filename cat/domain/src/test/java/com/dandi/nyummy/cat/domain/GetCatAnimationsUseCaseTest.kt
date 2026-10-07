package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO
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
    fun `성공하면 애니메이션을 돌려준다`() = runBlocking {
        val animations = CatAnimationSetVO(weight = "NORMAL")

        val result = useCase { animations }()

        assertEquals(Result.success(animations), result)
        assertNothingShown()
    }

    @Test
    fun `고양이가 없으면(404) 아무것도 띄우지 않고 뒤로 가지도 않는다`() = runBlocking {
        val result = useCase { throw httpException(404, "api.cat.notFound") }()

        assertTrue(result.isFailure)
        assertNothingShown()
    }

    @Test
    fun `서버 오류도 홈을 막지 않도록 조용히 실패한다`() = runBlocking {
        val result = useCase { throw httpException(500, "api.cat.animationMetadataInvalid") }()

        assertTrue(result.isFailure)
        assertNothingShown()
    }

    @Test
    fun `네트워크 오류도 조용히 실패한다`() = runBlocking {
        val result = useCase { throw IOException("offline") }()

        assertTrue(result.isFailure)
        assertNothingShown()
    }

    @Test
    fun `401 이면 세션 만료 안내 후 처음 화면으로 보낸다`() = runBlocking {
        val result = useCase { throw httpException(401, null) }()

        val dialog = messageHelper.dialogs.single()
        dialog.onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
        assertTrue(result.isFailure)
    }

    @Test(expected = CancellationException::class)
    fun `취소는 삼키지 않고 그대로 던진다`(): Unit = runBlocking {
        useCase { throw CancellationException("left home") }()
        Unit
    }

    private fun httpException(code: Int, errorCode: String?) = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/api/v1/cats/animations",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )
}
