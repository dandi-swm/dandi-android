package com.dandi.nyummy.common.data.analytics

import android.content.Intent
import android.net.Uri
import co.ab180.airbridge.Airbridge
import com.dandi.nyummy.common.domain.navigation.AppLinks
import java.net.URI

/**
 * Airbridge 트래킹 링크에 심는 스킴. 대시보드(트래킹 링크 > 딥링크)와 매니페스트 intent-filter 의 값과 같아야 한다.
 * 트래킹 링크의 목적지는 `nyummy://<앱 링크 경로>`로 적는다(예: `nyummy://shop/promotions/3`).
 * SDK 는 `airbridge_referrer` 쿼리가 없는 스킴 링크를 트래킹 링크로 인정하지 않아, 그런 링크는 아무 화면도 열지 않는다.
 */
private const val AIRBRIDGE_SCHEME = "nyummy"

/** Airbridge 가 붙이는 쿼리. 화면 인자가 아니라서 앱 링크로 바꿀 때 뺀다. */
private const val AIRBRIDGE_QUERY_PREFIX = "airbridge_"

/**
 * 이 intent 가 Airbridge 트래킹 링크로 열린 것이면 SDK 가 클릭을 기록하고, 링크에 심어 둔 목적지를
 * 앱 링크(`https://link.nyummy.co.kr/...`)로 바꿔 [onAppLink]로 준다. 결과는 조금 뒤에 온다.
 * SDK 를 켜지 않았거나 트래킹 링크가 아니면 false 를 주고 [onAppLink]는 부르지 않는다.
 */
fun AirbridgeSdk.handleDeeplink(intent: Intent, onAppLink: (String) -> Unit): Boolean {
    if (!isInitialized) return false
    return Airbridge.handleDeeplink(intent) { uri -> uri.toAppLink()?.let(onAppLink) }
}

/**
 * 앱이 없던 사람이 트래킹 링크를 누르고 설치했다면, 설치 뒤 처음 켰을 때 그 링크의 목적지를 [onAppLink]로 준다.
 * 설치 뒤 처음 부를 때만 동작하고, 이어받을 링크가 없으면 [onAppLink]는 부르지 않는다.
 */
fun AirbridgeSdk.handleDeferredDeeplink(onAppLink: (String) -> Unit) {
    if (!isInitialized) return
    Airbridge.handleDeferredDeeplink { uri -> uri?.toAppLink()?.let(onAppLink) }
}

/**
 * SDK 가 돌려준 목적지를 앱 링크로 바꾼다. 화면 해석은 앱 링크 한 길만 쓰게 하려는 것이다.
 * - `nyummy://shop/promotions/3?sort=new` → `https://link.nyummy.co.kr/shop/promotions/3?sort=new`
 *   (스킴 링크는 첫 구간이 host 자리에 온다)
 * - 경로가 없는 `nyummy://` 는 홈으로 연다.
 * - 이미 앱 링크(http 와 https 모두, 화면 쪽 앱 링크 판정과 같다)면 경로는 그대로 두고 Airbridge 쿼리만 뺀다.
 * - 계층형이 아닌 주소(`nyummy:shop`)와 그 밖의 주소(다른 광고 플랫폼이 넘기는 링크 등)는 열지 않는다.
 */
internal fun Uri.toAppLink(): String? = toString().toAppLinkOrNull()

/** [toAppLink]의 변환. android.net.Uri 는 JVM 단위 테스트에서 돌지 않아 문자열과 [URI]로 푼다. */
internal fun String.toAppLinkOrNull(): String? {
    // java.net.URI 는 `nyummy://`, `nyummy://?a=b` 처럼 authority 가 빈 주소를 받지 않아, 실패하면 빈 authority 를 빼고 다시 읽는다.
    val uri = runCatching { URI(this) }.recoverCatching { URI(replaceFirst("://", ":///")) }.getOrNull() ?: return null
    if (uri.isOpaque) return null
    val scheme = uri.scheme ?: return null

    val path = when {
        (scheme.equals("https", ignoreCase = true) || scheme.equals("http", ignoreCase = true)) &&
            uri.host.equals(AppLinks.HOST, ignoreCase = true) ->
            uri.rawPath.orEmpty()
        scheme.equals(AIRBRIDGE_SCHEME, ignoreCase = true) -> {
            val segments = (listOfNotNull(uri.rawAuthority) + uri.rawPath.orEmpty().split('/')).filter { it.isNotEmpty() }
            if (segments.isEmpty()) HOME_PATH else segments.joinToString(separator = "/", prefix = "/")
        }
        else -> return null
    }
    val query = uri.rawQuery.orEmpty().split('&')
        .filter { it.isNotEmpty() && !it.startsWith(AIRBRIDGE_QUERY_PREFIX) }
        .joinToString("&")
    return AppLinks.BASE_URL + path + if (query.isEmpty()) "" else "?$query"
}

/** 경로 없는 스킴 링크가 여는 화면. 홈 라우트 경로와 같다. */
private const val HOME_PATH = "/home"
