package com.dandi.nyummy.onboarding.data

import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.onboarding.data.dto.CatDTO
import com.dandi.nyummy.onboarding.entity.CatVO
import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
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
            FakeAppPreferenceProvider(),
        )

        val cat = repository.registerCat("냐미 \"2세\"")

        assertEquals(CatVO(id = 1L, name = "냐미 \"2세\""), cat)
    }

    @Test
    fun `온보딩 완료를 기록하면 영속 진행 상태를 해제한다`() = runBlocking {
        val preferences = FakeAppPreferenceProvider().apply { onboardingIncomplete = true }
        val repository = OnboardingRepositoryImpl(
            OnboardingDataSource(MockOnboardingApiService(json, latencyMillis = 0L)),
            preferences,
        )

        repository.markOnboardingComplete()

        assertEquals(false, preferences.onboardingIncomplete)
    }

    @Test
    fun `평소 식사 시각은 기기 저장소에 그대로 저장한다`() = runBlocking {
        val preferences = FakeAppPreferenceProvider()
        val repository = OnboardingRepositoryImpl(
            OnboardingDataSource(MockOnboardingApiService(json, latencyMillis = 0L)),
            preferences,
        )
        val mealTimes = MealTimesVO(breakfast = MealTimesVO.DefaultBreakfast.copy(isSkipped = true))

        repository.saveMealTimes(mealTimes)

        assertEquals(mealTimes, preferences.mealTimes)
    }

    @Test
    fun `응답 필드가 비어 있으면 VO 기본값으로 채운다`() {
        val dto = json.decodeFromString<CatDTO>("""{"catId":null}""")

        assertEquals(CatVO(id = 0L, name = ""), dto.toVO())
    }

    private class FakeAppPreferenceProvider : AppPreferenceProvider {
        var onboardingIncomplete = false

        override suspend fun hasShownPermissionNotice(): Boolean = false
        override suspend fun markPermissionNoticeShown() = Unit
        override suspend fun isOnboardingIncomplete(): Boolean = onboardingIncomplete
        override suspend fun setOnboardingIncomplete(incomplete: Boolean) {
            onboardingIncomplete = incomplete
        }

        var mealTimes: MealTimesVO? = null

        override suspend fun getMealTimes(): MealTimesVO? = mealTimes
        override suspend fun setMealTimes(mealTimes: MealTimesVO) {
            this.mealTimes = mealTimes
        }
    }
}
