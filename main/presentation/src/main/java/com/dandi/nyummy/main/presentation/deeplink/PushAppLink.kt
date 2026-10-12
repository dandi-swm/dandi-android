package com.dandi.nyummy.main.presentation.deeplink

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.dandi.nyummy.common.domain.navigation.LinkArgs
import com.dandi.nyummy.common.domain.navigation.NavRoute

/**
 * 서버 푸시 data에서 열 화면의 링크(전체 App Link URL)를 담는 키.
 * 앱이 백그라운드일 때 OS가 띄운 알림을 누르면 data가 이 키로 intent extras에 들어온다.
 */
const val PUSH_APP_LINK_KEY = "appLink"

/**
 * 이 intent가 열려는 링크. App Link와 앱이 띄운 알림은 [Intent.getData]로,
 * OS가 띄운 푸시 알림은 extras의 [PUSH_APP_LINK_KEY]로 들어온다.
 */
fun Intent.appLinkUri(): Uri? =
    data ?: getStringExtra(PUSH_APP_LINK_KEY)?.takeIf { it.isNotBlank() }?.toUri()

/** 이 intent를 연 알림의 식별자. 앱이 띄운 알림과 OS가 띄운 푸시 알림 모두 extras의 [LinkArgs.NOTIFICATION_ID]로 온다. */
fun Intent.pushNotificationId(): String? =
    getStringExtra(LinkArgs.NOTIFICATION_ID)?.takeIf { it.isNotBlank() }

/** 알림에서 열린 링크면 그 알림의 식별자를 라우트 인자에 싣는다. 같은 링크라도 알림마다 다른 키가 된다. */
fun NavRoute.withNotificationId(notificationId: String?): NavRoute =
    if (notificationId == null) this else copy(args = args + (LinkArgs.NOTIFICATION_ID to notificationId))
