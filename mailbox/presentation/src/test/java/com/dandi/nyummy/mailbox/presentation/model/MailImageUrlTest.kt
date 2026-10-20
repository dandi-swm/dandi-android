package com.dandi.nyummy.mailbox.presentation.model

import com.dandi.nyummy.mailbox.entity.MailCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class MailImageUrlTest {

    @Test
    fun `우편 종류로 CDN 경로를 고르고 그림 번호를 붙인다`() {
        assertEquals("https://cdn.nyummy.co.kr/reward-boxes/2.png", mailImageUrl(MailCategory.REWARD_BOX, 2L))
        assertEquals("https://cdn.nyummy.co.kr/coins/1.png", mailImageUrl(MailCategory.COIN, 1L))
        assertEquals("https://cdn.nyummy.co.kr/items/7.png", mailImageUrl(MailCategory.ITEM, 7L))
    }

    @Test
    fun `그림 번호가 없으면 빈 주소다`() {
        assertEquals("", mailImageUrl(MailCategory.ITEM, null))
    }
}
