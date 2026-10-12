package com.dandi.nyummy.settings.data.dto

import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import kotlinx.serialization.Serializable

/**
 * 알림 수신 설정(`GET`, `PATCH /api/v1/users/me/push`)입니다. 조회 응답과 변경 요청에 같이 쓴다.
 * 변경 요청에는 바꾸는 항목만 담고, 빠진 항목은 서버가 그대로 둔다.
 *
 * @property isServicePushEnabled 서비스 알림(식사 시간 알림, 분석 결과 알림)을 받는지
 * @property isMarketingPushEnabled 마케팅 알림(기록을 쉴 때 알림)을 받는지. 받지 않다가 받게 되면 서버가 동의 시각을 남긴다.
 */
@Serializable
data class PushSettingDTO(
    val isServicePushEnabled: Boolean? = null,
    val isMarketingPushEnabled: Boolean? = null,
) {
    fun toVO(): NotificationSettingsVO {
        val default = NotificationSettingsVO.default
        return NotificationSettingsVO(
            isServiceEnabled = isServicePushEnabled ?: default.isServiceEnabled,
            isMarketingEnabled = isMarketingPushEnabled ?: default.isMarketingEnabled,
        )
    }

    companion object {
        fun from(change: NotificationSettingsChange) = PushSettingDTO(
            isServicePushEnabled = change.isServiceEnabled,
            isMarketingPushEnabled = change.isMarketingEnabled,
        )
    }
}
