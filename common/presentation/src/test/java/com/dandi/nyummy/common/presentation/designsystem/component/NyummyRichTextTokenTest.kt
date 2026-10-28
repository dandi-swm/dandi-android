package com.dandi.nyummy.common.presentation.designsystem.component

import com.dandi.nyummy.common.presentation.designsystem.theme.DefaultNyummyColors
import com.dandi.nyummy.common.presentation.designsystem.theme.DefaultNyummyTypography
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 서버(상점 SDUI)가 보내는 글자·색 토큰 이름이 v2 스타일과 색으로 가는지 확인한다.
 * 서버 이름은 백엔드와의 계약이라, 앱에서 합치거나 없앤 이름도 계속 받아야 한다.
 */
class NyummyRichTextTokenTest {

    private val typography = DefaultNyummyTypography
    private val content = DefaultNyummyColors.content

    @Test
    fun `지금 쓰는 글자 이름은 같은 이름의 스타일로 간다`() {
        assertEquals(typography.displayM, typography.styleOf("displayM"))
        assertEquals(typography.titleL, typography.styleOf("titleL"))
        assertEquals(typography.labelM, typography.styleOf("labelM"))
        assertEquals(typography.numberS, typography.styleOf("numberS"))
    }

    @Test
    fun `v2에서 합친 Strong 글자 이름은 같은 크기의 라벨로 간다`() {
        assertEquals(typography.labelM, typography.styleOf("labelMStrong"))
        assertEquals(typography.labelS, typography.styleOf("labelSStrong"))
    }

    @Test
    fun `모르는 글자 이름은 bodyM으로 그린다`() {
        assertEquals(typography.bodyM, typography.styleOf("headline"))
    }

    @Test
    fun `지금 쓰는 색 이름은 같은 역할로 간다`() {
        assertEquals(content.secondary, content.colorOf("contentSecondary"))
        assertEquals(content.brand, content.colorOf("contentBrand"))
        assertEquals(content.danger, content.colorOf("contentDanger"))
        assertEquals(content.promo, content.colorOf("contentPromo"))
    }

    @Test
    fun `v2에 없는 색 이름은 대응표의 v2 색으로 간다`() {
        assertEquals(content.brand, content.colorOf("contentSuccess"))
        assertEquals(content.promo, content.colorOf("contentWarning"))
        assertEquals(content.secondary, content.colorOf("contentInfo"))
        assertEquals(content.onAction, content.colorOf("contentOnInverse"))
        assertEquals(content.primary, content.colorOf("contentOnCoin"))
    }

    @Test
    fun `모르는 색 이름은 primary로 그린다`() {
        assertEquals(content.primary, content.colorOf("contentRainbow"))
    }
}
