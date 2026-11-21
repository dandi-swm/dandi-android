package com.dandi.nyummy.common.domain.session

import com.dandi.nyummy.common.domain.coroutine.IoScope
import com.dandi.nyummy.common.domain.error.runSuspendCatching
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.SessionTokenHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.helper.WidgetHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인 세션이 끝났을 때(재발급까지 실패한 401) 앱 전체에서 한 번만 정리하고 로그인 화면으로 보낸다.
 *
 * 화면 여러 개가 동시에 요청하다 모두 401을 받아도 [onSessionExpired]는 처음 한 번만 일한다.
 * 1. 이 기기의 세션을 지운다: 로그인 토큰, 푸시 토큰, 냐미가 기억한 값, 튜토리얼 진행 상태, 홈 화면 위젯. 로그아웃과 같은 정리다.
 *    토큰을 지우면 토큰 재발급도 더 하지 않아, 정리 뒤에 자동 로그인이 다시 일어나지 않는다.
 * 2. 닫을 수 없고 다른 다이얼로그가 덮지 못하는 "로그인 만료" 다이얼로그를 띄운다([MessageHelper.showPinnedDialog]).
 * 3. 확인을 누르면 정리가 끝나기를 기다린 뒤 초기(로그인) 화면으로 보낸다. 그래야 다시 로그인할 때의 토큰 저장이 정리보다 먼저 실행되지 않는다.
 *
 * 상태는 둘이다.
 * - [isExpired]: 만료를 처리했고 아직 다시 로그인하지 않았다. 이 동안 오는 401은 다시 정리하거나 안내하지 않는다(재진입 막기).
 * - [isNoticeShowing]: 만료 안내를 띄운 뒤 로그인 화면으로 옮기기 전까지. 이 동안만 다른 공통 안내(404, 5xx)를 띄우지 않는다.
 *   로그인 화면에 도착한 뒤에는 로그인, 가입 요청의 404와 5xx를 평소처럼 안내해야 하므로 풀어 둔다.
 *
 * 다시 로그인하면([onSignedIn]) 다음 만료를 또 안내할 수 있게 된다. 앱 프로세스가 새로 뜨면 처음 상태로 돌아간다.
 */
@Singleton
class SessionExpiryHandler @Inject constructor(
    private val resourceHelper: ResourceHelper,
    private val messageHelper: MessageHelper,
    private val navigationHelper: NavigationHelper,
    private val sessionTokenHelper: SessionTokenHelper,
    private val pushTokenHelper: PushTokenHelper,
    private val companionMemory: CompanionMemoryHelper,
    private val tutorialHelper: TutorialHelper,
    private val widgetHelper: WidgetHelper,
    @IoScope private val scope: CoroutineScope,
) {

    private val expired = AtomicBoolean(false)
    private val noticeShowing = AtomicBoolean(false)

    /** 세션 만료를 이미 처리했고 아직 다시 로그인하지 않았는지. 이 동안 오는 401은 다시 처리하지 않는다. */
    val isExpired: Boolean
        get() = expired.get()

    /** 만료 안내를 띄웠고 아직 로그인 화면으로 옮기지 않았는지. 이 동안에는 다른 공통 오류 다이얼로그를 띄우지 않는다. */
    val isNoticeShowing: Boolean
        get() = noticeShowing.get()

    /** 로그인 세션이 끝났다. 몇 번을 불러도 정리, 안내, 이동은 처음 한 번만 한다. */
    fun onSessionExpired() {
        // 동시에 여러 스레드가 불러도 compareAndSet은 한 곳만 통과시킨다.
        if (!expired.compareAndSet(false, true)) return
        noticeShowing.set(true)
        val cleanup = scope.launch { clearLocalSession() }
        messageHelper.showPinnedDialog(
            descText = resourceHelper.getString(StringResource.SESSION_EXPIRED_MESSAGE),
            buttonText = resourceHelper.getString(StringResource.SIGN_IN_AGAIN),
            onClickButton = { moveToSignIn(cleanup) },
        )
    }

    /** 새로 로그인했다(로그인, 소셜 로그인, 회원가입 성공). 다음 세션 만료를 다시 처리할 수 있게 한다. */
    fun onSignedIn() {
        noticeShowing.set(false)
        expired.set(false)
    }

    private fun moveToSignIn(cleanup: Job) {
        scope.launch {
            cleanup.join()
            // 목적지(로그인 화면)를 직접 지정하지 않는다. common:domain 이 feature 의 Page 를 참조하면 순환 의존이 되므로
            // "초기 화면으로 이동" 신호만 날리고 실제 이동은 NavHost 가 결정한다.
            navigationHelper.navigateToInitial()
            // 로그인 화면에서는 로그인, 가입 요청의 404와 5xx를 다시 안내한다. 401 재처리 막기([isExpired])는 다시 로그인할 때까지 둔다.
            noticeShowing.set(false)
        }
    }

    /** 정리는 서로 상관없으니 하나가 실패해도 나머지는 계속한다. 토큰을 가장 먼저 지운다. */
    private suspend fun clearLocalSession() {
        runSuspendCatching { sessionTokenHelper.clearTokens() }
        runSuspendCatching { pushTokenHelper.deleteToken() }
        runSuspendCatching { companionMemory.clear() }
        runSuspendCatching { tutorialHelper.clear() }
        runSuspendCatching { widgetHelper.clear() }
    }
}
