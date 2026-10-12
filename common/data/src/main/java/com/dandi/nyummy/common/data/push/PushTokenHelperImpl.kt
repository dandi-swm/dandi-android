package com.dandi.nyummy.common.data.push

import android.util.Log
import com.dandi.nyummy.common.data.BuildConfig
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * FCM 토큰을 서버에 등록하고 지운다. 로그인한 상태(리프레시 토큰이 있음)일 때만 등록한다.
 * 실패는 로그만 남긴다. 서버가 멱등이라 다음 실행이나 다음 로그인 때 다시 보내면 된다.
 * 등록과 삭제는 [mutex]로 차례대로 실행한다. 로그아웃 직후 다시 로그인할 때 지우기 전 토큰을 등록하지 않게 하려는 것이다.
 */
class PushTokenHelperImpl(
    private val dataSource: DeviceTokenDataSource,
    private val tokenProvider: TokenProvider,
    private val ioScope: CoroutineScope,
) : PushTokenHelper {

    private val mutex = Mutex()

    override fun register(token: String?) {
        ioScope.launch {
            mutex.withLock { registerNow(token) }
        }
    }

    override fun deleteToken() {
        ioScope.launch {
            mutex.withLock { deleteNow() }
        }
    }

    private suspend fun registerNow(token: String?) {
        // 토큰 저장소는 첫 읽기 때 디스크를 막고 읽으므로 IO 스코프 안에서 확인한다.
        if (tokenProvider.refreshToken.isNullOrBlank()) return
        try {
            val pushToken = token ?: FirebaseMessaging.getInstance().token.await()
            dataSource.register(pushToken)
            // 테스트 발송에 쓸 수 있게 디버그 빌드에서만 토큰을 남긴다.
            if (BuildConfig.DEBUG) Log.d(TAG, "푸시 토큰 등록: $pushToken")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "푸시 토큰 등록 실패", e)
        }
    }

    private suspend fun deleteNow() {
        try {
            FirebaseMessaging.getInstance().deleteToken().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "푸시 토큰 삭제 실패", e)
        }
    }

    private companion object {
        const val TAG = "[NyummyFcm]"
    }
}
