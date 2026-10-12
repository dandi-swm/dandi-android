package com.dandi.nyummy.common.data.push

import com.dandi.nyummy.common.domain.error.HttpResponseException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** 푸시 토큰 등록 요청이 서버 계약(경로, body, 본문 없는 200)과 맞는지 확인합니다. */
class DeviceTokenDataSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var dataSource: DeviceTokenDataSource

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DeviceTokenApiService::class.java)
        dataSource = DeviceTokenDataSource(apiService)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `토큰과 ANDROID 플랫폼을 보내고 본문 없는 200을 성공으로 본다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        dataSource.register("fcm-token-123")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/notifications/device-tokens", request.path)
        assertEquals(
            Json.parseToJsonElement("""{"token":"fcm-token-123","platform":"ANDROID"}"""),
            Json.parseToJsonElement(request.body.readUtf8()),
        )
    }

    @Test
    fun `인증이 없으면 HttpResponseException으로 올라온다`() {
        server.enqueue(MockResponse().setResponseCode(401))

        val error = assertThrows(HttpResponseException::class.java) {
            runBlocking { dataSource.register("fcm-token-123") }
        }

        assertEquals(401, error.rawCode)
    }
}
