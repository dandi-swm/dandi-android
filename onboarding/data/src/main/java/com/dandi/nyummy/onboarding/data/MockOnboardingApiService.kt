package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.onboarding.data.dto.CatDTO
import com.dandi.nyummy.onboarding.data.dto.CatRegisterRequestDTO
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import retrofit2.Response

/**
 * 서버 API 가 생기기 전까지 쓰는 [OnboardingApiService] mock.
 *
 * 임시 명세의 응답 JSON 을 실제와 같은 [Json] 설정으로 파싱해 DTO → VO 변환 경로를 그대로 탄다.
 * 서버가 준비되면 [OnboardingDataModule] 에서 `retrofit.create` 로 바꾸고 이 파일을 지운다.
 */
class MockOnboardingApiService(
    private val json: Json,
    private val latencyMillis: Long = MOCK_LATENCY_MILLIS,
) : OnboardingApiService {

    override suspend fun registerCat(request: CatRegisterRequestDTO): Response<CatDTO> {
        delay(latencyMillis)
        return Response.success(json.decodeFromString<CatDTO>(registerCatResponseJson(request.name)))
    }

    companion object {
        private const val MOCK_LATENCY_MILLIS = 600L

        /** `POST /api/v1/cats` 성공 응답 예시 */
        fun registerCatResponseJson(name: String): String =
            """{"catId":1,"name":${Json.encodeToString(name)},"createdAt":"2026-10-04T09:00:00+09:00"}"""
    }
}