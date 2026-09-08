package com.dandi.nyummy.reminder

import android.content.Context
import androidx.core.content.edit

/**
 * 마지막으로 확인한 FCM 토큰 보관소 (임시 구현 — SharedPreferences).
 *
 * FCM 토큰 회전 자체는 SDK/서버가 관리하므로 막을 수 없다(재설치·데이터 삭제·기기 복원 시 변경).
 * 대신 직전 토큰과 비교해 회전을 감지하고, 서버 등록 API가 생기면 여기서 비교 후
 * 바뀐 경우에만 재등록하는 용도로 쓴다.
 */
object FcmTokenStore {

    private const val PREFS_NAME = "nyummy_fcm_token"
    private const val KEY_TOKEN = "token"

    fun currentOrNull(context: Context): String? =
        prefs(context).getString(KEY_TOKEN, null)

    /** 토큰을 저장하고, 기존 값에서 바뀌었는지(=회전) 여부를 돌려준다. */
    fun saveAndCheckRotated(context: Context, token: String): Boolean {
        val previous = currentOrNull(context)
        prefs(context).edit { putString(KEY_TOKEN, token) }
        return previous != null && previous != token
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
