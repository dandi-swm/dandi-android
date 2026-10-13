package com.dandi.nyummy.settings.data.dto

import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import org.junit.Assert.assertEquals
import org.junit.Test

class PushSettingDTOTest {

    @Test
    fun `서버 값을 그대로 옮긴다`() {
        val settings = PushSettingDTO(isServicePushEnabled = false, isMarketingPushEnabled = true).toVO()

        assertEquals(NotificationSettingsVO(isServiceEnabled = false, isMarketingEnabled = true), settings)
    }

    @Test
    fun `빠진 값은 서버 기본값으로 둔다 서비스 알림은 받고 마케팅 알림은 받지 않는다`() {
        assertEquals(NotificationSettingsVO(isServiceEnabled = true, isMarketingEnabled = false), PushSettingDTO().toVO())
    }

    @Test
    fun `변경 요청에는 바꾸는 항목만 담는다`() {
        assertEquals(
            PushSettingDTO(isServicePushEnabled = null, isMarketingPushEnabled = true),
            PushSettingDTO.from(NotificationSettingsChange(isMarketingEnabled = true)),
        )
    }
}
