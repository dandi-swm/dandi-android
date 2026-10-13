package com.dandi.nyummy.legal.presentation

import com.dandi.nyummy.legal.entity.LegalDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalLinksTest {

    @Test
    fun `앱 안에서는 상단 메뉴를 숨기는 주소로 연다`() {
        assertEquals("https://dandi-swm.github.io/nyummy-policy/terms.html?inapp=1", LegalDocument.TERMS.url)
        assertEquals("https://dandi-swm.github.io/nyummy-policy/privacy.html?inapp=1", LegalDocument.PRIVACY.url)
    }

    @Test
    fun `정책 페이지 아래 주소만 앱 안에서 연다`() {
        assertTrue(isPolicyUrl("https://dandi-swm.github.io/nyummy-policy/account-deletion.html?inapp=1"))
        assertFalse(isPolicyUrl("https://dandi-swm.github.io/other/"))
        assertFalse(isPolicyUrl("https://www.privacy.go.kr"))
        assertFalse(isPolicyUrl("mailto:help@nyummy.co.kr"))
    }

    @Test
    fun `주소에서 정책 페이지를 찾을 때 쿼리와 앵커는 무시한다`() {
        assertEquals(PolicyPage.PRIVACY, policyPageOf("https://dandi-swm.github.io/nyummy-policy/privacy.html?inapp=1#s5"))
        assertEquals(PolicyPage.TERMS, policyPageOf("https://dandi-swm.github.io/nyummy-policy/terms.html"))
        assertEquals(
            PolicyPage.ACCOUNT_DELETION,
            policyPageOf("https://dandi-swm.github.io/nyummy-policy/account-deletion.html?inapp=1"),
        )
    }

    @Test
    fun `모르는 페이지나 정책 페이지가 아닌 주소는 찾지 않는다`() {
        assertNull(policyPageOf("https://dandi-swm.github.io/nyummy-policy/"))
        assertNull(policyPageOf("https://example.com/privacy.html"))
        assertNull(policyPageOf(null))
    }
}
