package com.dandi.nyummy.legal.presentation

import com.dandi.nyummy.legal.entity.LegalDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LegalLinksTest {

    @Test
    fun `한국어 앱에서는 한국어판을 상단 메뉴를 숨기는 주소로 연다`() {
        assertEquals("https://www.nyummy.co.kr/ko/policy/terms.html?inapp=1", LegalDocument.TERMS.urlFor(Locale.KOREA))
        assertEquals("https://www.nyummy.co.kr/ko/policy/privacy.html?inapp=1", LegalDocument.PRIVACY.urlFor(Locale.KOREA))
    }

    @Test
    fun `일본어 앱에서는 일본어판을 연다`() {
        assertEquals("https://www.nyummy.co.kr/jp/policy/terms.html?inapp=1", LegalDocument.TERMS.urlFor(Locale.JAPAN))
        assertEquals("https://www.nyummy.co.kr/jp/policy/privacy.html?inapp=1", LegalDocument.PRIVACY.urlFor(Locale.JAPANESE))
    }

    @Test
    fun `한국어와 일본어가 아닌 언어는 한국어판을 연다`() {
        assertEquals("https://www.nyummy.co.kr/ko/policy/terms.html?inapp=1", LegalDocument.TERMS.urlFor(Locale.US))
    }

    @Test
    fun `기본 주소는 앱 언어를 따른다`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.JAPAN)
            assertEquals("https://www.nyummy.co.kr/jp/policy/terms.html?inapp=1", LegalDocument.TERMS.url)
            Locale.setDefault(Locale.KOREA)
            assertEquals("https://www.nyummy.co.kr/ko/policy/terms.html?inapp=1", LegalDocument.TERMS.url)
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `정책 페이지 아래 주소만 앱 안에서 연다`() {
        assertTrue(isPolicyUrl("https://www.nyummy.co.kr/ko/policy/account-deletion.html?inapp=1"))
        assertTrue(isPolicyUrl("https://www.nyummy.co.kr/jp/policy/privacy.html"))
        assertTrue(isPolicyUrl("https://dandi-swm.github.io/nyummy-policy/account-deletion.html?inapp=1"))
        assertFalse(isPolicyUrl("https://www.nyummy.co.kr/"))
        assertFalse(isPolicyUrl("https://www.nyummy.co.kr/en/policy/terms.html"))
        assertFalse(isPolicyUrl("https://dandi-swm.github.io/other/"))
        assertFalse(isPolicyUrl("https://www.privacy.go.kr"))
        assertFalse(isPolicyUrl("mailto:help@nyummy.co.kr"))
    }

    @Test
    fun `주소에서 정책 페이지를 찾을 때 쿼리와 앵커는 무시한다`() {
        assertEquals(PolicyPage.PRIVACY, policyPageOf("https://www.nyummy.co.kr/ko/policy/privacy.html?inapp=1#s5"))
        assertEquals(PolicyPage.TERMS, policyPageOf("https://www.nyummy.co.kr/jp/policy/terms.html#top"))
        assertEquals(
            PolicyPage.ACCOUNT_DELETION,
            policyPageOf("https://www.nyummy.co.kr/jp/policy/account-deletion.html?inapp=1"),
        )
    }

    @Test
    fun `예전 정책 페이지 주소도 찾는다`() {
        assertEquals(PolicyPage.PRIVACY, policyPageOf("https://dandi-swm.github.io/nyummy-policy/privacy.html?inapp=1#s5"))
        assertEquals(PolicyPage.TERMS, policyPageOf("https://dandi-swm.github.io/nyummy-policy/terms.html"))
        assertEquals(
            PolicyPage.ACCOUNT_DELETION,
            policyPageOf("https://dandi-swm.github.io/nyummy-policy/account-deletion.html?inapp=1"),
        )
    }

    @Test
    fun `모르는 페이지나 정책 페이지가 아닌 주소는 찾지 않는다`() {
        assertNull(policyPageOf("https://www.nyummy.co.kr/ko/policy/"))
        assertNull(policyPageOf("https://www.nyummy.co.kr/ko/policy/faq.html"))
        assertNull(policyPageOf("https://dandi-swm.github.io/nyummy-policy/"))
        assertNull(policyPageOf("https://example.com/privacy.html"))
        assertNull(policyPageOf(null))
    }
}
