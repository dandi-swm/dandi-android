package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.onboarding.data.dto.CatDTO
import com.dandi.nyummy.onboarding.entity.CatVO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingRepositoryImplTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Test
    fun `mock 응답 JSON 을 파싱해 등록한 이름의 고양이를 돌려준다`() = runBlocking {
        val repository = OnboardingRepositoryImpl(
            OnboardingDataSource(MockOnboardingApiService(json, latencyMillis = 0L)),
        )

        val cat = repository.registerCat("냐미 \"2세\"")

        assertEquals(CatVO(id = 1L, name = "냐미 \"2세\""), cat)
    }

    @Test
    fun `응답 필드가 비어 있으면 VO 기본값으로 채운다`() {
        val dto = json.decodeFromString<CatDTO>("""{"catId":null}""")

        assertEquals(CatVO(id = 0L, name = ""), dto.toVO())
    }
}
