package com.dandi.nyummy.settings.data

import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** 저장소가 서버 값(식사 시각, 알림 수신 설정)을 읽고 쓸 때 DTO 변환과 원격 호출을 이어 주는지 확인합니다. */
class SettingsRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: SettingsRepositoryImpl

    // 앱의 Json 설정(NetworkModule)과 같게 둔다.
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
        repository = SettingsRepositoryImpl(remoteDataSource = SettingsRemoteDataSource(apiService))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `서버의 식사 시각을 읽어 비워 둔 끼니는 건너뛴 것으로 바꾼다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"breakfastHour":null,"lunchHour":12,"dinnerHour":19}"""),
        )

        val mealTimes = repository.getMealTimes()

        assertEquals("/api/v1/users/me", server.takeRequest().path)
        assertEquals(
            MealTimesVO(
                breakfast = MealTimesVO.DefaultBreakfast.copy(isSkipped = true),
                lunch = MealTimeVO(hour = 12),
                dinner = MealTimeVO(hour = 19),
            ),
            mealTimes,
        )
    }

    @Test
    fun `식사 시각을 저장하면 건너뛴 끼니를 빼고 서버로 보낸다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        repository.saveMealTimes(MealTimesVO(breakfast = MealTimeVO(hour = 9, isSkipped = true), lunch = MealTimeVO(hour = 12)))

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/users/me/meal-time", request.path)
        assertEquals(
            Json.parseToJsonElement("""{"lunchHour":12,"dinnerHour":18}"""),
            Json.parseToJsonElement(request.body.readUtf8()),
        )
    }

    @Test
    fun `알림 수신 설정을 읽어 VO 로 바꾼다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"isServicePushEnabled":true,"isMarketingPushEnabled":true}"""),
        )

        val settings = repository.getNotificationSettings()

        assertEquals("/api/v1/users/me/push", server.takeRequest().path)
        assertEquals(NotificationSettingsVO(isServiceEnabled = true, isMarketingEnabled = true), settings)
    }

    @Test
    fun `알림 수신 설정은 바꾸는 항목만 서버로 보낸다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        repository.updateNotificationSettings(NotificationSettingsChange(isServiceEnabled = false))

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("""{"isServicePushEnabled":false}""", request.body.readUtf8())
    }
}
