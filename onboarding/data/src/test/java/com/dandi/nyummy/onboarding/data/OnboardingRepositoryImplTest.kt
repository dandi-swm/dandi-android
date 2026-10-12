package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
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

class OnboardingRepositoryImplTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private lateinit var server: MockWebServer
    private lateinit var preferences: FakeAppPreferenceProvider
    private lateinit var repository: OnboardingRepositoryImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OnboardingApiService::class.java)
        preferences = FakeAppPreferenceProvider()
        repository = OnboardingRepositoryImpl(OnboardingDataSource(apiService), preferences)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `고양이 생성은 이름과 식사 시각을 보내고 본문 없는 201을 성공으로 본다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))

        repository.createCat("냐미 \"2세\"", MealTimesVO.default)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/cats", request.path)
        assertEquals(
            Json.parseToJsonElement("""{"name":"냐미 \"2세\"","breakfastHour":8,"lunchHour":12,"dinnerHour":18}"""),
            Json.parseToJsonElement(request.body.readUtf8()),
        )
    }

    @Test
    fun `안 먹는 끼니는 시각을 보내지 않는다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201))
        val mealTimes = MealTimesVO.default.with(Meal.BREAKFAST, MealTimeVO(hour = 8, isSkipped = true))

        repository.createCat("냐미", mealTimes)

        assertEquals(
            Json.parseToJsonElement("""{"name":"냐미","lunchHour":12,"dinnerHour":18}"""),
            Json.parseToJsonElement(server.takeRequest().body.readUtf8()),
        )
    }

    @Test
    fun `이미 고양이가 있으면 서버 code를 담은 HttpResponseException으로 올라온다`() {
        server.enqueue(
            MockResponse().setResponseCode(409).setBody("""{"code":"api.cat.alreadyExists","message":"이미 고양이가 존재합니다."}"""),
        )

        val error = assertThrows(HttpResponseException::class.java) {
            runBlocking { repository.createCat("냐미", MealTimesVO.default) }
        }

        assertEquals(409, error.rawCode)
        assertEquals("api.cat.alreadyExists", error.cause?.message)
    }

    @Test
    fun `온보딩 완료를 기록하면 영속 진행 상태를 해제한다`() = runBlocking {
        preferences.onboardingIncomplete = true

        repository.markOnboardingComplete()

        assertEquals(false, preferences.onboardingIncomplete)
    }

    private class FakeAppPreferenceProvider : AppPreferenceProvider {
        var onboardingIncomplete = false

        override suspend fun hasShownPermissionNotice(): Boolean = false
        override suspend fun markPermissionNoticeShown() = Unit
        override suspend fun isOnboardingIncomplete(): Boolean = onboardingIncomplete
        override suspend fun setOnboardingIncomplete(incomplete: Boolean) {
            onboardingIncomplete = incomplete
        }

    }
}
