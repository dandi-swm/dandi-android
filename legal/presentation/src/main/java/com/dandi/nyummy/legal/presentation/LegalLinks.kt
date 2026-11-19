package com.dandi.nyummy.legal.presentation

import com.dandi.nyummy.legal.entity.LegalDocument
import java.util.Locale

/** 정책 페이지(www.nyummy.co.kr) 한국어판 주소. 일본어가 아닌 언어는 모두 이 페이지를 연다. */
internal const val POLICY_BASE_URL_KO = "https://www.nyummy.co.kr/ko/policy/"

/** 정책 페이지 일본어판 주소. */
internal const val POLICY_BASE_URL_JP = "https://www.nyummy.co.kr/jp/policy/"

/** 예전 정책 페이지(GitHub Pages) 주소. 옛 페이지가 남아 서로를 링크할 수 있어 여기도 앱 안에서 연다. */
internal const val LEGACY_POLICY_BASE_URL = "https://dandi-swm.github.io/nyummy-policy/"

/** 이 아래 주소만 앱 안에서 연다. */
private val POLICY_BASE_URLS = listOf(POLICY_BASE_URL_KO, POLICY_BASE_URL_JP, LEGACY_POLICY_BASE_URL)

/**
 * [locale] 언어에 맞는 정책 페이지 주소. 일본어면 일본어판, 그 밖은 한국어판.
 * 서버 `X-Language` 헤더와 같이 앱 언어는 [Locale.getDefault]의 언어로 본다.
 */
internal fun policyBaseUrlOf(locale: Locale): String =
    if (locale.language == Locale.JAPANESE.language) POLICY_BASE_URL_JP else POLICY_BASE_URL_KO

/** 앱 안에서 열 주소(앱 언어 기준). `inapp=1`을 붙이면 페이지가 상단 메뉴를 숨긴다(JS로 처리). */
internal val LegalDocument.url: String
    get() = urlFor(Locale.getDefault())

/** [locale] 언어로 앱 안에서 열 주소. */
internal fun LegalDocument.urlFor(locale: Locale): String {
    val base = policyBaseUrlOf(locale)
    return when (this) {
        LegalDocument.TERMS -> "${base}terms.html?inapp=1"
        LegalDocument.PRIVACY -> "${base}privacy.html?inapp=1"
    }
}

/** 정책 페이지끼리의 링크인지(새 한국어·일본어판, 예전 GitHub Pages). 그 밖의 주소(메일, 외부 사이트)는 앱 밖에서 연다. */
internal fun isPolicyUrl(url: String): Boolean = policyBaseOf(url) != null

private fun policyBaseOf(url: String): String? = POLICY_BASE_URLS.firstOrNull { url.startsWith(it) }

/** 정책 페이지 중 화면 제목을 아는 페이지. 약관과 방침이 서로를, 방침이 계정 삭제 안내를 링크한다. */
enum class PolicyPage(internal val fileName: String) {
    TERMS("terms.html"),
    PRIVACY("privacy.html"),
    ACCOUNT_DELETION("account-deletion.html"),
}

/** [url]이 가리키는 정책 페이지. 정책 페이지가 아니거나 모르는 페이지면 null. */
internal fun policyPageOf(url: String?): PolicyPage? {
    val base = url?.let(::policyBaseOf) ?: return null
    val fileName = url.removePrefix(base).substringBefore('?').substringBefore('#')
    return PolicyPage.entries.firstOrNull { it.fileName == fileName }
}
