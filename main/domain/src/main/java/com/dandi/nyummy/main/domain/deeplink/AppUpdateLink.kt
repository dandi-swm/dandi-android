package com.dandi.nyummy.main.domain.deeplink

import com.dandi.nyummy.common.domain.navigation.AppLinks
import java.net.URI

/**
 * 앱 업데이트 링크의 경로. 서버의 앱 업데이트 푸시(`PushType.APP_UPDATE`)가 `https://link.nyummy.co.kr/app-update`를 준다.
 * 화면 라우트가 아니라 Play 스토어의 냐미 페이지를 여는 링크라서 [matchRoute]의 레지스트리에는 없다.
 */
const val APP_UPDATE_LINK_PATH = "/app-update"

/**
 * [link]가 앱 업데이트 링크인지. 앱 링크 호스트([AppLinks.HOST])의 http(s) 주소이고 경로가 [APP_UPDATE_LINK_PATH]일 때만 맞다.
 *
 * 서버나 사람이 적은 링크가 조금 달라도 알아보게, 스킴, 호스트, 경로의 대소문자와 앞뒤 슬래시(`/app-update/`)는 따지지 않고
 * 쿼리와 프래그먼트는 보지 않는다. 광고 트래킹 링크 `nyummy://app-update`는 앱 셸이 이미 같은 앱 링크로 바꿔서 넘긴다.
 * 주소로 읽을 수 없으면 false 다.
 */
fun isAppUpdateLink(link: String): Boolean {
    val uri = runCatching { URI(link.trim()) }.getOrNull() ?: return false
    if (uri.isOpaque) return false
    val isWeb = uri.scheme.equals("https", ignoreCase = true) || uri.scheme.equals("http", ignoreCase = true)
    if (!isWeb || !uri.host.equals(AppLinks.HOST, ignoreCase = true)) return false
    val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
    return segments.size == 1 && segments.single().equals(APP_UPDATE_LINK_PATH.trimStart('/'), ignoreCase = true)
}
