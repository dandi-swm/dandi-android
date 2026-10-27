package com.dandi.nyummy.common.data.tutorial

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
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
    fun `시작하기 전에는 어떤 단계도 보여 주지 않는다`() = runBlocking {
        TutorialStep.entries.forEach { assertFalse(helper.isPending(it)) }
    }

    @Test
    fun `시작하면 마친 단계만 빼고 보여 준다`() = runBlocking {
        helper.start()
        helper.complete(TutorialStep.HOME_MAIL_POINTER)

        assertFalse(helper.isPending(TutorialStep.HOME_MAIL_POINTER))
        assertTrue(helper.isPending(TutorialStep.MAILBOX))
        assertTrue(helper.isPending(TutorialStep.ROOM))
    }

    @Test
    fun `다시 시작하면 마친 단계 기록을 비운다`() = runBlocking {
        helper.start()
        helper.complete(TutorialStep.ROOM)

        helper.start()

        assertTrue(helper.isPending(TutorialStep.ROOM))
    }

    @Test
    fun `건너뛰면 남은 단계를 모두 끝낸다`() = runBlocking {
        helper.start()

        helper.skipAll()

        TutorialStep.entries.forEach { assertFalse(helper.isPending(it)) }
    }

    @Test
    fun `지우면 대상이 아니게 된다`() = runBlocking {
        helper.start()
        helper.setMealTutorialRequired(true)

        helper.clear()

        TutorialStep.entries.forEach { assertFalse(helper.isPending(it)) }
        assertFalse(helper.isMealTutorialRequired())
    }

    @Test
    fun `식사 튜토리얼 여부는 옮겨 둔 값을 그대로 읽는다`() = runBlocking {
        assertFalse(helper.isMealTutorialRequired())

        helper.setMealTutorialRequired(true)
        assertTrue(helper.isMealTutorialRequired())

        helper.setMealTutorialRequired(false)
        assertFalse(helper.isMealTutorialRequired())
    }
}
