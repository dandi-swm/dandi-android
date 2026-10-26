package com.dandi.nyummy.cat.data

import com.dandi.nyummy.cat.data.dto.CatAnimationSetDTO
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.domain.error.HttpResponseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class CatRepositoryImplTest {

    private val api = FakeCatApiService()
    private val tokens = FakeTokenProvider()

    private fun CoroutineScope.repository() = CatRepositoryImpl(CatDataSource(api), tokens, ioScope = this)

    /** 앱의 IO 스코프처럼 미리 받기 하나가 실패해도 스코프 전체가 취소되지 않게 한다. */
    private fun runSupervised(block: suspend CoroutineScope.() -> Unit) = runBlocking { supervisorScope(block) }

    @Test
    fun `미리 받은 결과를 다음 조회가 API를 다시 부르지 않고 이어받는다`() = runSupervised {
        api.responses += success("SLIM")
        val repository = repository()

        assertEquals("SLIM", repository.prefetchAnimations()?.weight)
        assertEquals("SLIM", repository.getAnimations().weight)
        assertEquals(1, api.calls)
    }

    @Test
    fun `받는 중에 다시 미리 받으면 같은 요청을 기다린다`() = runSupervised {
        api.responses += success("SLIM")
        val repository = repository()

        val results = listOf(async { repository.prefetchAnimations() }, async { repository.prefetchAnimations() }).awaitAll()

        assertEquals(listOf("SLIM", "SLIM"), results.map { it?.weight })
        assertEquals(1, api.calls)
    }

    @Test
    fun `미리 받은 결과는 한 번만 쓰고 다음 조회는 새로 받는다`() = runSupervised {
        api.responses += listOf(success("SLIM"), success("CHUBBY"))
        val repository = repository()

        repository.prefetchAnimations()

        assertEquals("SLIM", repository.getAnimations().weight)
        assertEquals("CHUBBY", repository.getAnimations().weight)
        assertEquals(2, api.calls)
    }

    @Test
    fun `미리 받기가 실패하면 그 실패를 던지고, 다음 조회도 다시 받지 않고 그 실패를 넘긴다`() = runSupervised {
        api.responses += listOf(Response.error(500, "".toResponseBody()), success("SLIM"))
        val repository = repository()

        assertTrue(runCatching { repository.prefetchAnimations() }.exceptionOrNull() is HttpResponseException)

        val failure = runCatching { repository.getAnimations() }.exceptionOrNull()
        assertTrue(failure is HttpResponseException)
        assertEquals(1, api.calls)

        // 실패를 넘긴 뒤의 조회는 새로 받는다.
        assertEquals("SLIM", repository.getAnimations().weight)
        assertEquals(2, api.calls)
    }

    @Test
    fun `로그인 전이면 미리 받지 않는다`() = runSupervised {
        tokens.refreshToken = null
        api.responses += success("SLIM")
        val repository = repository()

        assertNull(repository.prefetchAnimations())
        assertEquals(0, api.calls)

        assertEquals("SLIM", repository.getAnimations().weight)
        assertEquals(1, api.calls)
    }

    @Test
    fun `로그인 전에 끝난 미리 받기가 남아 있어도 로그인한 뒤 미리 받으면 새로 받는다`() = runSupervised {
        tokens.refreshToken = null
        api.responses += success("SLIM")
        val repository = repository()
        assertNull(repository.prefetchAnimations())

        tokens.refreshToken = "refresh"

        assertEquals("SLIM", repository.prefetchAnimations()?.weight)
        assertEquals("SLIM", repository.getAnimations().weight)
        assertEquals(1, api.calls)
    }

    private fun success(weight: String): Response<CatAnimationSetDTO> = Response.success(CatAnimationSetDTO(weight = weight))
}

/** 넣어 둔 응답을 차례로 돌려주고, 불린 횟수를 센다. */
private class FakeCatApiService : CatApiService {
    val responses = ArrayDeque<Response<CatAnimationSetDTO>>()
    var calls = 0
        private set

    override suspend fun getAnimations(): Response<CatAnimationSetDTO> {
        calls++
        return responses.removeFirst()
    }
}

private class FakeTokenProvider : TokenProvider {
    override var accessToken: String? = "access"
    override var refreshToken: String? = "refresh"

    override suspend fun update(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }

    override suspend fun clear() {
        accessToken = null
        refreshToken = null
    }
}
