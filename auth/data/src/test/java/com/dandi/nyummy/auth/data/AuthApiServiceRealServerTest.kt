package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * dev 실서버를 상대로 Retrofit 스택 전체(경로·직렬화·역직렬화)가 동작하는지 검증하는 스모크 테스트입니다.
 *
 * 실서버 상태에 따라 결과가 달라질 수 있어 CI용 [AuthApiServiceTest]와 분리되어 있습니다.
 */
class AuthApiServiceRealServerTest {

    private lateinit var apiService: AuthApiService

    @Before
    fun setUp() {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
        apiService = Retrofit.Builder()
            .baseUrl(DEV_SERVER_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApiService::class.java)
    }

    @Test
    fun `실서버 로그인 호출이 성공하고 토큰이 파싱된다`() = runBlocking {
        val response = apiService.login(
            LoginRequestDTO(email = "test@example.com", password = "pw1234")
        )

        println("[login] code=${response.code()} body=${response.body()}")

        assertTrue("HTTP ${response.code()}: ${response.errorBody()?.string()}", response.isSuccessful)
        val body = response.body()
        assertFalse("accessToken이 비어 있음", body?.accessToken.isNullOrEmpty())
        assertFalse("refreshToken이 비어 있음", body?.refreshToken.isNullOrEmpty())
    }

    @Test
    fun `실서버 이메일 인증 발송 호출이 성공한다`() = runBlocking {
        val response = apiService.requestEmailVerification(
            EmailVerificationRequestDTO(email = "test@example.com")
        )

        println("[email-verification] code=${response.code()}")

        assertTrue("HTTP ${response.code()}: ${response.errorBody()?.string()}", response.isSuccessful)
    }

    @Test
    fun `실서버 이메일 인증 확인 호출이 성공한다`() = runBlocking {
        val response = apiService.confirmEmailVerification(
            EmailVerificationConfirmRequestDTO(email = "test@example.com", verificationCode = "123456")
        )

        println("[email-verification/confirm] code=${response.code()}")

        assertTrue("HTTP ${response.code()}: ${response.errorBody()?.string()}", response.isSuccessful)
    }

    companion object {
        private const val DEV_SERVER_URL = "http://3.35.248.138:8080"
    }
}
