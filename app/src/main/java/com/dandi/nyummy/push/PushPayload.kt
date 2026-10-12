package com.dandi.nyummy.push

import com.dandi.nyummy.common.domain.navigation.LinkArgs
import com.dandi.nyummy.main.presentation.deeplink.PUSH_APP_LINK_KEY

/**
 * 서버 푸시의 data(값은 모두 문자열). 서버는 notification 블록도 함께 보내서, 앱이 백그라운드면 OS가 그 알림을 띄우고
 * data는 알림을 누를 때 intent extras로 온다. 앱이 켜져 있을 때만 이 data로 앱이 직접 알림을 띄운다.
 * 어느 쪽이든 화면 이동은 알림을 눌렀을 때 [appLink]로 한다. 종류(`type`)에 따라 다르게 처리하는 곳은 없어 읽지 않는다.
 *
 * @property notificationId 알림마다 다른 서버 식별자. 같은 알림이 두 번 오면 교체하는 데 쓴다.
 * @property appLink 알림을 누르면 열 화면의 App Link 전체 URL(예: `https://link.nyummy.co.kr/meals/123`)
 */
data class PushPayload(
    val notificationId: String?,
    val title: String?,
    val body: String?,
    val appLink: String?,
) {
    companion object {
        const val KEY_NOTIFICATION_ID = LinkArgs.NOTIFICATION_ID
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"

        fun from(data: Map<String, String>): PushPayload = PushPayload(
            notificationId = data[KEY_NOTIFICATION_ID].nonBlank(),
            title = data[KEY_TITLE].nonBlank(),
            body = data[KEY_BODY].nonBlank(),
            appLink = data[PUSH_APP_LINK_KEY].nonBlank(),
        )

        private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }
    }
}
