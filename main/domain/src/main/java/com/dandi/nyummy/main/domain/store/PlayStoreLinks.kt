package com.dandi.nyummy.main.domain.store

/**
 * Google Play 의 냐미 앱 페이지 주소.
 *
 * 패키지 이름은 Play 에 올라간 이름으로 고정한다. 기기의 패키지 이름(`context.packageName`)은 빌드 종류에 따라 접미사가
 * 붙을 수 있어 스토어 페이지를 찾지 못할 수 있다(지금은 debug 도 접미사 없이 같다, `app/build.gradle.kts`의 applicationId).
 */
object PlayStoreLinks {

    /** Play 에 올라간 냐미 패키지 이름. */
    const val PACKAGE_NAME = "com.dandi.nyummy"

    /** Play 스토어 앱으로 바로 여는 주소. */
    const val MARKET_URL = "market://details?id=$PACKAGE_NAME"

    /** Play 스토어 앱이 없거나 열리지 않을 때 브라우저로 여는 웹 주소. */
    const val WEB_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"

    /** 열어 볼 순서. Play 스토어 앱을 먼저, 안 되면 웹 주소. */
    val candidates: List<String> = listOf(MARKET_URL, WEB_URL)
}

/**
 * [urls]를 앞에서부터 [open]으로 열어 보고 처음 열린 주소를 준다. [open]은 열었으면 true, 열 앱이 없으면 false 다.
 * 모두 실패하면 null 이다. 열리는 주소를 만나면 뒤의 주소는 열지 않는다.
 */
fun openFirstAvailable(urls: List<String>, open: (String) -> Boolean): String? = urls.firstOrNull(open)
