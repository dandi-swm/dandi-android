package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LogoutUseCaseTest {

    private val calls = mutableListOf<String>()
    private val navigationHelper = RecordingNavigationHelper()

    private val useCase = LogoutUseCase(
        repository = FakeAuthRepository(),
        pushTokenHelper = object : PushTokenHelper {
            override fun register(token: String?) = Unit
            override suspend fun deleteToken() {
                calls += "pushToken"
            }
        },
        companionMemory = object : CompanionMemoryHelper {
            override suspend fun lastSeenAtMillis(): Long? = null
            override suspend fun setLastSeenAtMillis(millis: Long) = Unit
            override suspend fun lastCatWeight(): String? = null
            override suspend fun setLastCatWeight(weight: String) = Unit
            override suspend fun clear() {
                calls += "companion"
            }
        },
        tutorialHelper = object : TutorialHelper {
            override suspend fun pendingSteps(): Set<TutorialStep> = emptySet()
            override suspend fun startAfterMeal() = Unit
            override suspend fun complete(step: TutorialStep) = Unit
            override suspend fun skipToHealthStory() = Unit
            override suspend fun finish() = Unit
            override suspend fun clear() {
                calls += "tutorial"
            }
            override suspend fun isMealTutorialRequired(): Boolean = false
            override suspend fun setMealTutorialRequired(required: Boolean) = Unit
        },
        navigationHelper = navigationHelper,
    )

    @Test
    fun `로그아웃하면 튜토리얼 진행 상태까지 지운 뒤 첫 화면으로 간다`() = runBlocking {
        useCase()

        assertEquals(listOf("pushToken", "companion", "tutorial"), calls)
        assertEquals(1, navigationHelper.initialCount)
    }
}
