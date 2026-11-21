package com.dandi.nyummy.common.data.tutorial

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.IOException
import org.junit.Assert.fail
import org.junit.Assert.assertSame
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
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

    @Test
    fun `기기 저장소 쓰기가 실패해도 기록 함수는 예외를 던지지 않는다`() = runBlocking {
        val failing = TutorialHelperImpl(FailingWriteDataStore { IOException("No space left on device") })

        // 화면이 viewModelScope.launch 에서 바로 부르는 기록 함수들. 하나라도 던지면 앱이 꺼진다.
        failing.startAfterMeal()
        failing.complete(TutorialStep.HOME_MAIL)
        failing.skipToHealthStory()
        failing.finish()
        failing.setMealTutorialRequired(true)
        failing.clear()

        // 남지 않았으니 다음에 안내가 다시 뜰 수 있다(읽기는 그대로 된다).
        assertTrue(failing.pendingSteps().isEmpty())
    }

    @Test
    fun `IOException 밖의 쓰기 실패도 삼키지만 코루틴 취소는 다시 던진다`() = runBlocking {
        TutorialHelperImpl(FailingWriteDataStore { IllegalStateException("corrupted") }).finish()

        val cancel = CancellationException("screen closed")
        try {
            TutorialHelperImpl(FailingWriteDataStore { cancel }).complete(TutorialStep.MAILBOX)
            fail("CancellationException 을 삼켰다")
        } catch (e: CancellationException) {
            assertSame(cancel, e)
        }
    }

    /** 읽기는 빈 값, 쓰기는 [error]를 던지는 DataStore. */
    private class FailingWriteDataStore(private val error: () -> Exception) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf(emptyPreferences())
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw error()
    }
}
