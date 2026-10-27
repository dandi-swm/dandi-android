package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LogoutUseCaseTest {

    private val navigationHelper = RecordingNavigationHelper()

    /** 지울 때마다 그 시점에 첫 화면으로 간 횟수를 남긴다. 첫 화면으로 가기 전에 지웠는지 본다. */
    private val tutorialHelper = object : TutorialHelper {
        val initialCountsAtClear = mutableListOf<Int>()
        override suspend fun isPending(step: TutorialStep) = false
        override suspend fun complete(step: TutorialStep) = Unit
        override suspend fun start() = Unit
        override suspend fun skipAll() = Unit
        override suspend fun clear() {
            initialCountsAtClear += navigationHelper.initialCount
        }
        override suspend fun isMealTutorialRequired() = false
        override suspend fun setMealTutorialRequired(required: Boolean) = Unit
    }
    private val pushTokenHelper = object : PushTokenHelper {
        override fun register(token: String?) = Unit
        override suspend fun deleteToken() = Unit
    }

    @Test
    fun `로그아웃하면 튜토리얼 진행 상태를 지운 뒤 첫 화면으로 간다`() = runBlocking {
        LogoutUseCase(FakeAuthRepository(), pushTokenHelper, tutorialHelper, navigationHelper)()

        assertEquals(listOf(0), tutorialHelper.initialCountsAtClear)
        assertEquals(1, navigationHelper.initialCount)
    }
}
