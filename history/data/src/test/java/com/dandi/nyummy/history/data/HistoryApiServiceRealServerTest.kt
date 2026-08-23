package com.dandi.nyummy.history.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * dev 실서버를 상대로 Retrofit 스택 전체(경로·직렬화·역직렬화·toVO 매핑)가 동작하는지 검증하는 스모크 테스트입니다.
 *
 * meal API 는 인증이 필요하므로 매 실행마다 dev 테스트 계정으로 로그인해 액세스 토큰을 새로 발급받는다.
 * 실서버 상태/계정에 따라 결과가 달라질 수 있어 CI 단위 테스트와 분리해 이해한다.
 */
class HistoryApiServiceRealServerTest {

    private lateinit var apiService: HistoryApiService

    @Before
    fun setUp() = runBlocking {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
        val converter = json.asConverterFactory("application/json".toMediaType())

        val loginService = Retrofit.Builder()
            .baseUrl(DEV_SERVER_URL)
            .addConverterFactory(converter)
            .build()
            .create(LoginService::class.java)
        val token = loginService.login(LoginBody(email = TEST_EMAIL, password = TEST_PASSWORD))
            .body()?.accessToken
        assertTrue("dev 계정 로그인 실패 - 토큰을 받지 못함", !token.isNullOrBlank())

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build(),
                )
            }
            .build()
        apiService = Retrofit.Builder()
            .baseUrl(DEV_SERVER_URL)
            .client(client)
            .addConverterFactory(converter)
            .build()
            .create(HistoryApiService::class.java)
    }

    @Test
    fun `실서버 월간 조회가 성공하고 현재 월 날짜가 VO 로 매핑된다`() = runBlocking {
        val response = apiService.getMonthlyMeals(year = 2026, month = 8)

        println("[monthly] code=${response.code()}")
        assertTrue("HTTP ${response.code()}: ${response.errorBody()?.string()}", response.isSuccessful)

        val vo = response.body()!!.toVO()
        // isCurrentMonth==true 인 날짜만 담기므로, 매핑이 맞으면 8월 날짜들이 존재해야 한다.
        assertTrue("days 가 비어있음 - isCurrentMonth 직렬화 매핑 확인 필요", vo.days.isNotEmpty())
        assertTrue("현재 월(8월) 날짜만 담겨야 한다", vo.days.all { it.date.month == 8 })
    }

    @Test
    fun `실서버 일일 조회가 성공하고 하루 영양이 VO 로 매핑된다`() = runBlocking {
        val response = apiService.getDailyMeals(year = 2026, month = 8, day = 23)

        println("[daily] code=${response.code()}")
        assertTrue("HTTP ${response.code()}: ${response.errorBody()?.string()}", response.isSuccessful)

        val vo = response.body()!!.toVO()
        assertTrue("목표 열량이 매핑되어야 한다", vo.nutrition.targetCalorieKcal > 0)
    }

    private interface LoginService {
        @POST("/api/v1/auth/login")
        suspend fun login(@Body body: LoginBody): Response<TokenBody>
    }

    @Serializable
    private data class LoginBody(val email: String, val password: String)

    @Serializable
    private data class TokenBody(
        val accessToken: String? = null,
        val refreshToken: String? = null,
    )

    companion object {
        private const val DEV_SERVER_URL = "http://3.35.248.138:8080"
        private const val TEST_EMAIL = "test@dandi.com"
        private const val TEST_PASSWORD = "Test1234!"
    }
}
