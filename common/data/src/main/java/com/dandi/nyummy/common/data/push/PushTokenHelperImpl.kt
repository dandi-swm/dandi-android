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
import kotlinx.coroutines.withTimeoutOrNull

/**
 * FCM 토큰을 서버에 등록하고 지운다. 로그인한 상태(리프레시 토큰이 있음)일 때만 등록한다.
 * 실패는 로그만 남긴다. 서버가 멱등이라 다음 실행이나 다음 로그인 때 다시 보내면 된다.
 * 삭제는 끝날 때까지 기다리므로(로그아웃이 기다린다) 그 뒤의 로그인 등록과 순서가 섞이지 않는다.
 * [mutex]는 진행 중인 등록과 삭제가 겹치지 않게 한다.
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

    override suspend fun deleteToken() {
        // 네트워크가 안 되면 오래 걸릴 수 있어 상한을 둔다. 로그아웃 화면 이동이 이 시간만큼만 기다린다.
        mutex.withLock { withTimeoutOrNull(DELETE_TIMEOUT_MILLIS) { deleteNow() } }
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
        const val DELETE_TIMEOUT_MILLIS = 3_000L
    }
}
