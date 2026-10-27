package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import java.io.IOException
import javax.inject.Inject

/**
 * 로그아웃하고 로그인 화면으로 돌아간다.
 *
 * 서버 로그아웃은 멱등이고, 실패해도 이 기기의 토큰은 이미 지웠으므로 사용자에게 알리지 않는다.
 * 로그인 만료(401)도 같은 결과라 공통 안내를 띄우지 않는다.
 * 이 기기의 첫 사용자 튜토리얼 진행 상태도 지운다. 다른 계정으로 로그인했을 때 남은 튜토리얼이 뜨지 않게 한다.
 * 이 기기의 푸시 토큰도 지운다. 서버 로그아웃이 실패해 서버에 토큰이 남아도 이 기기로는 푸시가 가지 않는다.
 * 지우기가 끝난 뒤 로그인 화면으로 간다. 그래야 다시 로그인할 때의 토큰 등록이 지우기보다 먼저 실행되지 않는다.
 */
class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val pushTokenHelper: PushTokenHelper,
    private val tutorialHelper: TutorialHelper,
    private val navigationHelper: NavigationHelper,
) {
    suspend operator fun invoke() {
        try {
            repository.logout()
        } catch (_: HttpResponseException) {
        } catch (_: IOException) {
        }
        pushTokenHelper.deleteToken()
        try {
            tutorialHelper.clear()
        } catch (_: IOException) {
        }
        navigationHelper.navigateToInitial()
    }
}
