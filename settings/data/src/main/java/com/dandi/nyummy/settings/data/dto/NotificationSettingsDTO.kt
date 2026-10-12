package com.dandi.nyummy.settings.data.dto

import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import kotlinx.serialization.Serializable

/** 알림 설정. 조회 응답과 저장 요청(전체를 통째로 보낸다)에 같이 쓴다. */
@Serializable
data class NotificationSettingsDTO(
    val allEnabled: Boolean? = null,
    val mealReminderEnabled: Boolean? = null,
    val noticeEnabled: Boolean? = null,
) {
    fun toVO(): NotificationSettingsVO {
        val default = NotificationSettingsVO.default
        return NotificationSettingsVO(
            isAllEnabled = allEnabled ?: default.isAllEnabled,
            isMealReminderEnabled = mealReminderEnabled ?: default.isMealReminderEnabled,
            isNoticeEnabled = noticeEnabled ?: default.isNoticeEnabled,
        )
    }

    companion object {
        fun from(vo: NotificationSettingsVO) = NotificationSettingsDTO(
            allEnabled = vo.isAllEnabled,
            mealReminderEnabled = vo.isMealReminderEnabled,
            noticeEnabled = vo.isNoticeEnabled,
        )
    }
}
