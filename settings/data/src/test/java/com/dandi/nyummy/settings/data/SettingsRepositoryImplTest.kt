package com.dandi.nyummy.settings.data

import android.content.ContextWrapper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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

/** 저장소가 식사 시각을 서버에서 읽고 쓸 때 DTO 변환과 원격 호출을 이어 주는지 확인합니다. */
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
        repository = SettingsRepositoryImpl(
            // 식사 시각은 알림 설정과 배경음을 쓰지 않는다. 만들기만 하고 부르지 않는다.
            notificationDataSource = NotificationSettingsDataSource(ContextWrapper(null), json),
            localDataSource = SettingsLocalDataSource(UnusedDataStore),
            remoteDataSource = SettingsRemoteDataSource(apiService),
        )
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

    private object UnusedDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = emptyFlow()
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            error("not used")
    }
}
