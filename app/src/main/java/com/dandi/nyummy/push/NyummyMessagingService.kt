package com.dandi.nyummy.push

import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 서버 푸시 수신기. payload는 [PushPayload] 참고.
 *
 * 서버가 notification 블록을 함께 보내므로 이 수신기는 **앱이 켜져 있을 때만** 불린다.
 * 백그라운드에서는 OS가 알림을 띄우고, 누르면 MainActivity가 extras의 링크로 화면을 연다.
 * 켜져 있을 때도 같게 동작한다. 서버가 준 제목과 본문으로 알림만 띄우고, 화면 이동과 갱신은 알림을 눌렀을 때 한다.
 */
@AndroidEntryPoint
class NyummyMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushTokenHelper: PushTokenHelper

    override fun onNewToken(token: String) {
        // 로그인한 상태일 때만 서버에 등록된다. 로그아웃 상태면 다음 로그인 때 등록한다.
        pushTokenHelper.register(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = PushPayload.from(message.data)
        PushNotifier.notify(
            context = this,
            title = payload.title ?: message.notification?.title,
            body = payload.body ?: message.notification?.body,
            deepLink = payload.appLink,
            notificationId = payload.notificationId,
        )
    }
}
