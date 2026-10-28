package com.dandi.nyummy.common.data.tutorial

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TutorialHelperImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val helper = TutorialHelperImpl(
        PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("app_prefs.preferences_pb") },
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `튜토리얼 식사 전에는 남은 단계가 없다`() = runBlocking {
        assertTrue(helper.pendingSteps().isEmpty())
    }

    @Test
    fun `튜토리얼 식사가 접수되면 뒤 단계를 모두 남긴다`() = runBlocking {
        helper.startAfterMeal()

        assertEquals(TutorialStep.entries.toSet(), helper.pendingSteps())
    }

    @Test
    fun `마친 단계만 빠진다`() = runBlocking {
        helper.startAfterMeal()
        helper.complete(TutorialStep.HOME_MAIL)

        assertFalse(helper.isPending(TutorialStep.HOME_MAIL))
        assertTrue(helper.isPending(TutorialStep.MAILBOX))
        assertTrue(helper.isPending(TutorialStep.HEALTH_STORY))
    }

    @Test
    fun `건너뛰면 냐미 건강 이야기만 남는다`() = runBlocking {
        helper.startAfterMeal()

        helper.skipToHealthStory()

        assertEquals(setOf(TutorialStep.HEALTH_STORY), helper.pendingSteps())
    }

    @Test
    fun `튜토리얼 식사 전에 건너뛰어도 냐미 건강 이야기는 남는다`() = runBlocking {
        helper.skipToHealthStory()

        assertEquals(setOf(TutorialStep.HEALTH_STORY), helper.pendingSteps())
    }

    @Test
    fun `마치면 남은 단계가 없다`() = runBlocking {
        helper.startAfterMeal()

        helper.finish()

        assertTrue(helper.pendingSteps().isEmpty())
    }

    @Test
    fun `식사 튜토리얼 여부를 옮겨 둔다`() = runBlocking {
        assertFalse(helper.isMealTutorialRequired())

        helper.setMealTutorialRequired(true)

        assertTrue(helper.isMealTutorialRequired())
    }

    @Test
    fun `지우면 진행 상태와 식사 튜토리얼 여부를 모두 비운다`() = runBlocking {
        helper.startAfterMeal()
        helper.setMealTutorialRequired(true)

        helper.clear()

        assertTrue(helper.pendingSteps().isEmpty())
        assertFalse(helper.isMealTutorialRequired())
    }
}
