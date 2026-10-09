package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import java.io.IOException
import javax.inject.Inject

/**
 * 로그아웃하고 로그인 화면으로 돌아간다.
 *
 * 서버 로그아웃은 멱등이고, 실패해도 이 기기의 토큰은 이미 지웠으므로 사용자에게 알리지 않는다.
 * 로그인 만료(401)도 같은 결과라 공통 안내를 띄우지 않는다.
 */
class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val navigationHelper: NavigationHelper,
) {
    suspend operator fun invoke() {
        try {
            repository.logout()
        } catch (_: HttpResponseException) {
        } catch (_: IOException) {
        }
        navigationHelper.navigateToInitial()
    }
}
