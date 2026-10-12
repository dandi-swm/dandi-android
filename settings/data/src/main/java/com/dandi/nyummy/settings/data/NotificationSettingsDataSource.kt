package com.dandi.nyummy.settings.data

import android.content.Context
import com.dandi.nyummy.settings.data.dto.NotificationSettingsDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 서버 API가 생기기 전까지 쓰는 알림 설정 데이터 소스.
 *
 * assets의 happy case JSON([HAPPY_CASE_ASSET])을 실제와 같은 [Json] 설정으로 파싱해 DTO → VO 경로를 그대로 탄다.
 * 저장한 값은 앱이 살아 있는 동안 메모리에만 남는다.
 *
 * TODO(server): API가 생기면 `BaseRemoteDataSource` + `SettingsApiService`로 바꾸고 JSON 파일을 지운다.
 */
class NotificationSettingsDataSource(
    private val context: Context,
    private val json: Json,
    private val latencyMillis: Long = MOCK_LATENCY_MILLIS,
) {

    private val mutex = Mutex()
    private var saved: NotificationSettingsDTO? = null

    /** `GET /api/v1/users/me/notification-settings` */
    suspend fun getNotificationSettings(): NotificationSettingsDTO {
        delay(latencyMillis)
        return mutex.withLock { saved ?: readHappyCase().also { saved = it } }
    }

    /** `PUT /api/v1/users/me/notification-settings`. 저장한 값을 그대로 돌려준다. */
    suspend fun updateNotificationSettings(request: NotificationSettingsDTO): NotificationSettingsDTO {
        delay(latencyMillis)
        return mutex.withLock { request.also { saved = it } }
    }

    private suspend fun readHappyCase(): NotificationSettingsDTO = withContext(Dispatchers.IO) {
        val raw = context.assets.open(HAPPY_CASE_ASSET).bufferedReader().use { it.readText() }
        json.decodeFromString<NotificationSettingsDTO>(raw)
    }

    companion object {
        const val HAPPY_CASE_ASSET = "settings_notification_happy_case.json"
        private const val MOCK_LATENCY_MILLIS = 300L
    }
}
