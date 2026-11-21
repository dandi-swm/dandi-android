package com.dandi.nyummy.common.domain.session

import com.dandi.nyummy.common.domain.fake.CallLog
import com.dandi.nyummy.common.domain.fake.FakeMessageHelper
import com.dandi.nyummy.common.domain.fake.FakeNavigationHelper
import com.dandi.nyummy.common.domain.fake.StringNameResourceHelper
import com.dandi.nyummy.common.domain.fake.recordingFake
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import com.dandi.nyummy.common.domain.helper.SessionTokenHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.helper.WidgetHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SessionExpiryHandlerTest {

    private val log = CallLog()
    private val messageHelper = FakeMessageHelper()
    private val navigationHelper = FakeNavigationHelper()

    @Test
    fun `동시에 401 이 여러 개 와도 정리와 다이얼로그와 로그인 이동은 한 번만 한다`() {
        val handler = handler()
        val callers = 32
        val pool = Executors.newFixedThreadPool(callers)
        val start = CountDownLatch(1)
        val done = CountDownLatch(callers)
        repeat(callers) {
            pool.execute {
                start.await()
                handler.onSessionExpired()
                done.countDown()
            }
        }

        start.countDown()
        assertTrue(done.await(5, TimeUnit.SECONDS))
        pool.shutdown()
        // 다이얼로그는 하나뿐이고, 누를 수 있는 버튼도 하나뿐이다.
        messageHelper.dialogs.single().onClickButton?.invoke()

        assertEquals(StringResource.SESSION_EXPIRED_MESSAGE.name, messageHelper.dialogs.single().descText)
        assertTrue(messageHelper.dialogs.single().cantIgnore)
        // 다른 다이얼로그가 덮지 못하는 고정 다이얼로그로 띄운다.
        assertTrue(messageHelper.dialogs.single().pinned)
        assertEquals(1, log.count("SessionTokenHelper.clearTokens"))
        assertEquals(1, navigationHelper.initialCount)
    }

    @Test
    fun `로그아웃과 같은 정리를 한다 - 토큰을 가장 먼저 지운다`() {
        handler().onSessionExpired()

        assertEquals(
            listOf(
                "SessionTokenHelper.clearTokens",
                "PushTokenHelper.deleteToken",
                "CompanionMemoryHelper.clear",
                "TutorialHelper.clear",
                "WidgetHelper.clear",
            ),
            log.calls,
        )
    }

    @Test
    fun `정리 하나가 실패해도 나머지 정리와 로그인 이동은 한다`() {
        val handler = handler(pushTokenHelper = recordingFake(log, failingMethod = "deleteToken"))

        handler.onSessionExpired()
        messageHelper.dialogs.single().onClickButton?.invoke()

        assertEquals(1, log.count("WidgetHelper.clear"))
        assertEquals(1, navigationHelper.initialCount)
    }

    @Test
    fun `확인을 눌러도 정리가 끝나기 전에는 로그인 화면으로 가지 않는다`() = runBlocking {
        // 토큰 지우기가 끝나지 않은 상태를 만든다.
        val tokenCleared = CompletableDeferred<Unit>()
        val slowTokens = object : SessionTokenHelper {
            override suspend fun clearTokens() {
                tokenCleared.await()
                log.calls += "SessionTokenHelper.clearTokens"
            }
        }
        navigationHelper.onNavigateToInitial = { log.calls += "navigateToInitial" }
        val handler = handler(sessionTokenHelper = slowTokens)

        handler.onSessionExpired()
        messageHelper.dialogs.single().onClickButton?.invoke()
        assertEquals(0, navigationHelper.initialCount)

        tokenCleared.complete(Unit)

        assertEquals(1, navigationHelper.initialCount)
        assertTrue(log.calls.indexOf("SessionTokenHelper.clearTokens") < log.calls.indexOf("navigateToInitial"))
    }

    @Test
    fun `다시 로그인하기 전에는 몇 번을 불러도 다시 정리하지 않는다`() {
        val handler = handler()

        handler.onSessionExpired()
        handler.onSessionExpired()

        assertTrue(handler.isExpired)
        assertEquals(1, log.count("SessionTokenHelper.clearTokens"))
        assertEquals(1, messageHelper.dialogs.size)
    }

    @Test
    fun `안내 중 상태는 로그인 화면으로 옮기면 풀리고 401 재처리 막기는 다시 로그인할 때까지 남는다`() {
        val handler = handler()

        handler.onSessionExpired()
        assertTrue(handler.isNoticeShowing)

        messageHelper.dialogs.single().onClickButton?.invoke()

        assertEquals(1, navigationHelper.initialCount)
        assertFalse(handler.isNoticeShowing)
        assertTrue(handler.isExpired)

        handler.onSessionExpired()
        assertEquals(1, messageHelper.dialogs.size)
        assertFalse(handler.isNoticeShowing)
    }

    @Test
    fun `다시 로그인하면 다음 세션 만료를 다시 처리한다`() {
        val handler = handler()

        handler.onSessionExpired()
        handler.onSignedIn()
        assertFalse(handler.isExpired)
        handler.onSessionExpired()

        assertEquals(2, log.count("SessionTokenHelper.clearTokens"))
        assertEquals(2, messageHelper.dialogs.size)
    }

    private fun handler(
        sessionTokenHelper: SessionTokenHelper = recordingFake(log),
        pushTokenHelper: PushTokenHelper = recordingFake(log),
        companionMemory: CompanionMemoryHelper = recordingFake(log),
        tutorialHelper: TutorialHelper = recordingFake(log),
        widgetHelper: WidgetHelper = recordingFake(log),
    ) = SessionExpiryHandler(
        resourceHelper = StringNameResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        sessionTokenHelper = sessionTokenHelper,
        pushTokenHelper = pushTokenHelper,
        companionMemory = companionMemory,
        tutorialHelper = tutorialHelper,
        widgetHelper = widgetHelper,
        // 바로 실행되는 디스패처라 정리가 onSessionExpired 안에서 끝난다(멈추는 곳이 없으면).
        scope = CoroutineScope(Dispatchers.Unconfined),
    )
}
