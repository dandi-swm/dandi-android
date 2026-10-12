package com.dandi.nyummy.settings.data

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.serverErrorCode
import com.dandi.nyummy.settings.data.dto.MealTimeRequestDTO
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

/** 식사 시각 읽기, 저장 요청이 서버 계약(경로, body, 본문 없는 200)과 맞는지 확인합니다. */
class SettingsRemoteDataSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var dataSource: SettingsRemoteDataSource

    // 앱의 Json 설정(NetworkModule)과 같게 둔다. explicitNulls가 꺼져 있어 null 필드는 보내지 않는다.
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
            .create(SettingsApiService::class.java)
        dataSource = SettingsRemoteDataSource(apiService)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `내 프로필을 GET 으로 받아 식사 시각만 읽는다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":6,"email":null,"nickname":"냐미","coin":0,"breakfastHour":8,"lunchHour":null,"dinnerHour":19}""",
            ),
        )

        val profile = dataSource.getMyProfile()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/users/me", request.path)
        assertEquals(8, profile.breakfastHour)
        assertEquals(null, profile.lunchHour)
        assertEquals(19, profile.dinnerHour)
    }

    @Test
    fun `식사 시각은 PUT 으로 보내고 건너뛴 끼니는 빼며 본문 없는 200을 성공으로 본다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        dataSource.updateMealTime(MealTimeRequestDTO(breakfastHour = 8, dinnerHour = 19))

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/users/me/meal-time", request.path)
        assertEquals(
            Json.parseToJsonElement("""{"breakfastHour":8,"dinnerHour":19}"""),
            Json.parseToJsonElement(request.body.readUtf8()),
        )
    }

    @Test
    fun `세 끼를 모두 건너뛰면 빈 객체를 보낸다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        dataSource.updateMealTime(MealTimeRequestDTO())

        assertEquals("{}", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `서버가 거절하면 code 를 담은 HttpResponseException 으로 올라온다`() {
        server.enqueue(
            MockResponse().setResponseCode(400).setBody(
                """{"code":"api.common.invalidInputValue","message":"요청 값이 올바르지 않습니다."}""",
            ),
        )

        val error = assertThrows(HttpResponseException::class.java) {
            runBlocking { dataSource.updateMealTime(MealTimeRequestDTO(breakfastHour = 8, lunchHour = 8)) }
        }

        assertEquals(400, error.rawCode)
        assertEquals("api.common.invalidInputValue", error.serverErrorCode)
    }
}
