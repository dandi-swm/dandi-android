package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.common.domain.navigation.AppLinks
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.shop.entity.ProductSort
import com.dandi.nyummy.shop.entity.ShopTab
import java.net.URI
import java.net.URLDecoder

/**
 * 상점 탭을 가리키는 링크인지 읽는다.
 *
 * 서버 SDUI 액션 링크 `https://link.nyummy.co.kr/shop/interior?sort=POPULAR`([parseLink])와,
 * 그 링크가 별칭으로 바뀐 상점 라우트(`/shop` + [ShopPage.ARG_TAB]·[ShopPage.ARG_SORT], [parse])를 같은 규칙으로 읽는다.
 * 상점 화면은 이 결과로 화면을 새로 쌓지 않고 탭과 정렬만 바꾼다.
 */
object ShopTabLink {

    /**
     * @property tab 열 탭
     * @property sort 정렬. 링크에 정렬이 없으면 null(지금 정렬 유지), 모르는 값이면 기본 정렬이다.
     */
    data class Target(val tab: ShopTab, val sort: ProductSort?)

    /** 앱 링크 문자열을 읽는다. 앱 링크가 아니거나 상점 탭 링크가 아니면 null. */
    fun parseLink(link: String): Target? {
        val uri = runCatching { URI(link.trim()) }.getOrNull() ?: return null
        val isAppLink = (uri.scheme.equals("https", ignoreCase = true) || uri.scheme.equals("http", ignoreCase = true)) &&
            uri.host.equals(AppLinks.HOST, ignoreCase = true)
        if (!isAppLink) return null
        val path = uri.rawPath.orEmpty().trimEnd('/')
        return parse(NavRoute(path = path, args = parseQuery(uri.rawQuery)))
    }

    /** 상점 라우트(또는 경로·쿼리로 나눈 링크)를 읽는다. 상점 탭 링크가 아니면 null. */
    fun parse(route: NavRoute): Target? {
        val tab = when {
            route.path == ShopPage.PATH -> ShopTab.fromLinkSegment(route.args[ShopPage.ARG_TAB]) ?: return null
            route.path.startsWith(ShopPage.PATH + "/") -> {
                val segment = route.path.removePrefix(ShopPage.PATH + "/")
                ShopTab.fromLinkSegment(segment)?.takeIf { it != ShopTab.RECOMMEND } ?: return null
            }
            else -> return null
        }
        val sort = route.args[ShopPage.ARG_SORT]?.let(ProductSort::fromOrDefault)
        return Target(tab = tab, sort = sort)
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        val args = linkedMapOf<String, String>()
        for (pair in rawQuery.orEmpty().split('&')) {
            val key = decode(pair.substringBefore('='))
            if (key.isEmpty()) continue
            args.putIfAbsent(key, decode(pair.substringAfter('=', missingDelimiterValue = "")))
        }
        return args
    }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)
}
