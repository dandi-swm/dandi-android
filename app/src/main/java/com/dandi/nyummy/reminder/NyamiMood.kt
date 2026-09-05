package com.dandi.nyummy.reminder

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dandi.nyummy.R

/**
 * 리텐션 넛지(알림/위젯)에서 쓰는 냐미의 기분.
 *
 * 마지막 식사 기록에서 멀어질수록 HAPPY → SAD → CRYING → ANGRY 순으로 나빠진다.
 * FCM data 메시지의 "mood" 키 값과 1:1 매칭된다(대소문자 무시).
 */
enum class NyamiMood(
    @field:DrawableRes val imageRes: Int,
    @field:StringRes val notiTitleRes: Int,
    @field:StringRes val notiBodyRes: Int,
    @field:StringRes val widgetMessageRes: Int,
) {
    HAPPY(
        imageRes = R.drawable.nyami_mood_happy,
        notiTitleRes = R.string.nyami_noti_title_happy,
        notiBodyRes = R.string.nyami_noti_body_happy,
        widgetMessageRes = R.string.nyami_widget_msg_happy,
    ),
    SAD(
        imageRes = R.drawable.nyami_mood_sad,
        notiTitleRes = R.string.nyami_noti_title_sad,
        notiBodyRes = R.string.nyami_noti_body_sad,
        widgetMessageRes = R.string.nyami_widget_msg_sad,
    ),
    CRYING(
        imageRes = R.drawable.nyami_mood_crying,
        notiTitleRes = R.string.nyami_noti_title_crying,
        notiBodyRes = R.string.nyami_noti_body_crying,
        widgetMessageRes = R.string.nyami_widget_msg_crying,
    ),
    ANGRY(
        imageRes = R.drawable.nyami_mood_angry,
        notiTitleRes = R.string.nyami_noti_title_angry,
        notiBodyRes = R.string.nyami_noti_body_angry,
        widgetMessageRes = R.string.nyami_widget_msg_angry,
    ),
    ;

    companion object {
        fun fromKeyOrNull(key: String?): NyamiMood? =
            entries.find { it.name.equals(key, ignoreCase = true) }

        /** 마지막 기록 후 경과 시간 기준 기분. 기록이 없으면(elapsed null) SAD 로 시작한다. */
        fun fromElapsed(elapsedMillis: Long?): NyamiMood {
            val hours = (elapsedMillis ?: return SAD) / MILLIS_PER_HOUR
            return when {
                hours < 6 -> HAPPY
                hours < 12 -> SAD
                hours < 24 -> CRYING
                else -> ANGRY
            }
        }

        private const val MILLIS_PER_HOUR = 60L * 60L * 1000L
    }
}
