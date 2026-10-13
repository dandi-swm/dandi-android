package com.dandi.nyummy.legal.presentation

import com.dandi.nyummy.legal.entity.LegalDocument

/** 정책 페이지(GitHub Pages, dandi-swm/nyummy-policy) 주소. 이 아래 주소만 앱 안에서 연다. */
internal const val POLICY_BASE_URL = "https://dandi-swm.github.io/nyummy-policy/"

/** 앱 안에서 열 주소. `inapp=1`을 붙이면 페이지가 상단 메뉴를 숨긴다(JS로 처리). */
internal val LegalDocument.url: String
    get() = when (this) {
        LegalDocument.TERMS -> "${POLICY_BASE_URL}terms.html?inapp=1"
        LegalDocument.PRIVACY -> "${POLICY_BASE_URL}privacy.html?inapp=1"
    }

/** 정책 페이지끼리의 링크인지. 그 밖의 주소(메일, 외부 사이트)는 앱 밖에서 연다. */
internal fun isPolicyUrl(url: String): Boolean = url.startsWith(POLICY_BASE_URL)

/** 정책 페이지 중 화면 제목을 아는 페이지. 약관과 방침이 서로를, 방침이 계정 삭제 안내를 링크한다. */
enum class PolicyPage(internal val fileName: String) {
    TERMS("terms.html"),
    PRIVACY("privacy.html"),
    ACCOUNT_DELETION("account-deletion.html"),
}

/** [url]이 가리키는 정책 페이지. 정책 페이지가 아니거나 모르는 페이지면 null. */
internal fun policyPageOf(url: String?): PolicyPage? {
    if (url == null || !isPolicyUrl(url)) return null
    val fileName = url.removePrefix(POLICY_BASE_URL).substringBefore('?').substringBefore('#')
    return PolicyPage.entries.firstOrNull { it.fileName == fileName }
}
