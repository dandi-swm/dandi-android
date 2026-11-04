package com.dandi.nyummy.widget.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dandi.nyummy.common.data.BaseLocalDataSource
import com.dandi.nyummy.widget.data.dto.WidgetSnapshotDTO
import com.dandi.nyummy.widget.entity.WidgetSnapshotVO
import kotlinx.serialization.json.Json

/** 위젯 DataStore("home_widget")에 스냅샷 JSON 하나를 읽고 쓴다. 깨진 JSON은 빈 값으로 읽는다. */
class WidgetDataSource(
    dataStore: DataStore<Preferences>,
    private val json: Json,
) : BaseLocalDataSource(dataStore) {

    suspend fun read(): WidgetSnapshotVO = decode(read(KEY_SNAPSHOT))

    suspend fun update(transform: (WidgetSnapshotVO) -> WidgetSnapshotVO) {
        editAtomically { prefs ->
            val next = transform(decode(prefs[KEY_SNAPSHOT]))
            prefs[KEY_SNAPSHOT] = json.encodeToString(WidgetSnapshotDTO.serializer(), WidgetSnapshotDTO.from(next))
        }
    }

    suspend fun clear() {
        clearAll()
    }

    private fun decode(raw: String?): WidgetSnapshotVO {
        if (raw.isNullOrBlank()) return WidgetSnapshotVO.empty
        return runCatching { json.decodeFromString(WidgetSnapshotDTO.serializer(), raw).toVO() }
            .getOrDefault(WidgetSnapshotVO.empty)
    }

    private companion object {
        val KEY_SNAPSHOT = stringPreferencesKey("widget_snapshot")
    }
}
