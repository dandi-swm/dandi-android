package com.dandi.nyummy.common.presentation.image

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CdnWebpFallbackTest {

    private val webp = "https://cdn.nyummy.co.kr/items/12.webp"
    private val png = "https://cdn.nyummy.co.kr/items/12.png"

    private val memory = FakeMemory()
    private var now = 1_000_000L
    private val fallback = CdnWebpFallback(memory, nowMillis = { now })

    /** 받기 한 번. 어떤 주소를 네트워크 허용 여부와 함께 불렀는지 남긴다. */
    private data class Call(val url: String, val networkAllowed: Boolean)

    private data class Result(val call: Call, val outcome: CdnLoadOutcome)

    private val calls = mutableListOf<Call>()

    /** 주소별 결과 표. 네트워크를 막은 받기(캐시만)는 [cacheOnly] 표를 본다. 표에 없으면 실패다. */
    private fun load(
        url: String = webp,
        network: Map<String, CdnLoadOutcome>,
        cacheOnly: Map<String, CdnLoadOutcome> = emptyMap(),
    ): Result = runBlocking {
        fallback.load(
            url = url,
            attempt = { target, networkAllowed ->
                val call = Call(target, networkAllowed)
                calls += call
                val table = if (networkAllowed) network else cacheOnly
                Result(call, table[target] ?: CdnLoadOutcome.FAILED)
            },
            outcomeOf = { it.outcome },
        )
    }

    @Test
    fun `webp가 있으면 webp 하나만 받고 아무것도 기억하지 않는다`() {
        val result = load(network = mapOf(webp to CdnLoadOutcome.SUCCESS))

        assertEquals(Call(webp, true), result.call)
        assertEquals(listOf(Call(webp, true)), calls)
        assertNull(memory.at(png))
    }

    @Test
    fun `webp가 없으면 png를 받고 그 경로를 기억한다`() {
        val result = load(network = mapOf(webp to CdnLoadOutcome.MISSING, png to CdnLoadOutcome.SUCCESS))

        assertEquals(CdnLoadOutcome.SUCCESS, result.outcome)
        assertEquals(listOf(Call(webp, true), Call(png, true)), calls)
        assertEquals(now, memory.at(png))
    }

    @Test
    fun `webp와 png가 모두 없으면 마지막 실패를 돌려주고 기억하지 않는다`() {
        val result = load(network = mapOf(webp to CdnLoadOutcome.MISSING, png to CdnLoadOutcome.MISSING))

        assertEquals(Call(png, true), result.call)
        assertEquals(CdnLoadOutcome.MISSING, result.outcome)
        assertNull(memory.at(png))
    }

    @Test
    fun `기억한 경로는 webp를 묻지 않고 곧장 png를 받는다`() {
        memory.store[png] = now - 1_000L

        val result = load(network = mapOf(webp to CdnLoadOutcome.MISSING, png to CdnLoadOutcome.SUCCESS))

        assertEquals(CdnLoadOutcome.SUCCESS, result.outcome)
        assertEquals(listOf(Call(png, true)), calls)
    }

    @Test
    fun `기억이 지나면 webp를 다시 묻고 서버가 바꿨으면 기억을 지운다`() {
        memory.store[png] = now - CdnWebpFallback.PNG_MEMORY_TTL_MILLIS

        val result = load(network = mapOf(webp to CdnLoadOutcome.SUCCESS, png to CdnLoadOutcome.SUCCESS))

        assertEquals(Call(webp, true), result.call)
        assertEquals(listOf(Call(webp, true)), calls)
        assertNull(memory.at(png))
    }

    @Test
    fun `기억이 지났는데 여전히 webp가 없으면 기억 시각을 새로 적는다`() {
        memory.store[png] = now - CdnWebpFallback.PNG_MEMORY_TTL_MILLIS - 1L

        load(network = mapOf(webp to CdnLoadOutcome.MISSING, png to CdnLoadOutcome.SUCCESS))

        assertEquals(listOf(Call(webp, true), Call(png, true)), calls)
        assertEquals(now, memory.at(png))
    }

    @Test
    fun `기억한 시각이 미래면(시계가 뒤로 감) 지난 것으로 보고 webp를 묻는다`() {
        memory.store[png] = now + 1_000L

        load(network = mapOf(webp to CdnLoadOutcome.SUCCESS))

        assertEquals(listOf(Call(webp, true)), calls)
    }

    @Test
    fun `기억과 달리 png가 없어졌으면 기억을 버리고 webp를 받는다`() {
        memory.store[png] = now - 1_000L

        val result = load(network = mapOf(webp to CdnLoadOutcome.SUCCESS, png to CdnLoadOutcome.MISSING))

        assertEquals(Call(webp, true), result.call)
        assertEquals(listOf(Call(png, true), Call(webp, true)), calls)
        assertNull(memory.at(png))
    }

    @Test
    fun `네트워크 오류면 png를 네트워크로 받지 않고 캐시에서만 찾으며 기억하지 않는다`() {
        val result = load(
            network = mapOf(webp to CdnLoadOutcome.FAILED, png to CdnLoadOutcome.SUCCESS),
            cacheOnly = mapOf(png to CdnLoadOutcome.SUCCESS),
        )

        assertEquals(Call(png, false), result.call)
        assertEquals(CdnLoadOutcome.SUCCESS, result.outcome)
        assertEquals(listOf(Call(webp, true), Call(png, false)), calls)
        assertNull(memory.at(png))
    }

    @Test
    fun `네트워크 오류인데 캐시에도 png가 없으면 webp 실패를 돌려준다`() {
        val result = load(network = mapOf(webp to CdnLoadOutcome.FAILED))

        assertEquals(Call(webp, true), result.call)
        assertEquals(CdnLoadOutcome.FAILED, result.outcome)
        assertNull(memory.at(png))
    }

    @Test
    fun `앱이 만든 CDN webp 주소가 아니면 한 번만 그대로 받는다`() {
        val banner = "https://cdn.example.com/banner.webp"

        load(url = banner, network = emptyMap())

        assertEquals(listOf(Call(banner, true)), calls)
    }

    @Test
    fun `403과 404만 파일 없음으로 보고 나머지 실패는 다른 오류로 본다`() {
        assertEquals(CdnLoadOutcome.MISSING, cdnFailureOutcomeOf(403))
        assertEquals(CdnLoadOutcome.MISSING, cdnFailureOutcomeOf(404))
        assertEquals(CdnLoadOutcome.FAILED, cdnFailureOutcomeOf(504)) // 오프라인에서 캐시만 찾다 실패
        assertEquals(CdnLoadOutcome.FAILED, cdnFailureOutcomeOf(500))
        assertEquals(CdnLoadOutcome.FAILED, cdnFailureOutcomeOf(null)) // 연결 실패, 디코딩 실패
    }

    private class FakeMemory : CdnPngFallbackMemory {
        val store = mutableMapOf<String, Long>()

        fun at(pngUrl: String): Long? = store[pngUrl]

        override suspend fun pngRememberedAt(pngUrl: String): Long? = store[pngUrl]

        override suspend fun rememberPng(pngUrl: String, atMillis: Long) {
            store[pngUrl] = atMillis
        }

        override suspend fun forget(pngUrl: String) {
            store.remove(pngUrl)
        }
    }
}
