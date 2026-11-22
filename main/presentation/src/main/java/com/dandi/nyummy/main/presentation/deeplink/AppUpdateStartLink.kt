package com.dandi.nyummy.main.presentation.deeplink

import android.content.Intent
import com.dandi.nyummy.main.domain.deeplink.isAppUpdateLink

/**
 * 콜드 스타트(MainActivity.onCreate)에서 시작 링크 [link]가 앱 업데이트 링크라 Play 스토어를 열어야 하는지.
 *
 * 링크 하나는 한 번만 연다. onCreate 는 사용자가 링크를 누른 때 말고도 불린다.
 * - 화면 회전, 다크 모드 같은 설정 변경이나 시스템이 앱을 종료했다 되살린 경우([isRestored]): 같은 intent 가 다시 온다.
 * - 앱을 뒤로 가기로 나갔다가 최근 앱 목록에서 다시 연 경우([isLaunchedFromHistory]): 예전 intent 가 그대로 온다.
 * 이때 다시 열면 스토어에서 돌아올 때마다 스토어가 또 뜨므로 열지 않는다. 시작 백스택은 늘 인트로라서 앱 흐름은 그대로 간다.
 */
internal fun shouldOpenStoreOnStart(link: String, isRestored: Boolean, isLaunchedFromHistory: Boolean): Boolean =
    !isRestored && !isLaunchedFromHistory && isAppUpdateLink(link)

/** 최근 앱 목록에서 다시 연 intent 인지. 이때는 예전 링크가 그대로 들어 있다. */
fun Intent.isLaunchedFromHistory(): Boolean = flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
