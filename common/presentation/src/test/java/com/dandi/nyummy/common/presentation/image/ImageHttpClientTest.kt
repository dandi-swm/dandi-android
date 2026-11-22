package com.dandi.nyummy.common.presentation.image

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageHttpClientTest {

    @Test
    fun `연결, 읽기, 쓰기 타임아웃은 API 클라이언트와 같은 30초다`() {
        val client = newImageHttpClient()

        assertEquals(30_000, client.connectTimeoutMillis)
        assertEquals(30_000, client.readTimeoutMillis)
        assertEquals(30_000, client.writeTimeoutMillis)
    }

    @Test
    fun `요청 전체 시간 제한은 두지 않아 느리게 받는 큰 그림을 자르지 않는다`() {
        assertEquals(0, newImageHttpClient().callTimeoutMillis)
    }
}
