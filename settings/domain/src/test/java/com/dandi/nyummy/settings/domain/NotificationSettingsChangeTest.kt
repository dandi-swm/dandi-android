package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSettingsChangeTest {

    private val current = NotificationSettingsVO(isServiceEnabled = true, isMarketingEnabled = true)

    @Test
    fun `바꾸는 항목만 적용하고 나머지는 그대로 둔다`() {
        assertEquals(
            NotificationSettingsVO(isServiceEnabled = false, isMarketingEnabled = true),
            NotificationSettingsChange(isServiceEnabled = false).applyTo(current),
        )
    }

    @Test
    fun `되돌리기는 바꾼 항목만 저장된 값으로 돌린다`() {
        val saved = NotificationSettingsVO(isServiceEnabled = true, isMarketingEnabled = false)

        val revert = NotificationSettingsChange(isMarketingEnabled = true).revertTo(saved)

        assertEquals(NotificationSettingsChange(isMarketingEnabled = false), revert)
        assertEquals(
            NotificationSettingsVO(isServiceEnabled = true, isMarketingEnabled = false),
            revert.applyTo(current),
        )
    }
}
