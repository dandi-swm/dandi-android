package com.dandi.nyummy.common.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStore Preferences 기반 로컬 데이터소스의 공통 베이스.
 *
 * [BaseRemoteDataSource.checkResponse] 가 원격 실패를 한 곳에서 처리하듯,
 * 디스크 읽기 실패([IOException] — 파일 손상 등)를 [emptyPreferences] 복구로 통일한다.
 * 그 외 예외는 삼키지 않고 전파하며, 쓰기 실패도 호출자에게 그대로 전파한다.
 *
 * 쓰기에 Mutex 를 두지 않는다 — [DataStore.edit] 가 자체적으로 직렬화·원자성을 보장하므로
 * read-modify-write 유실이 발생하지 않는다. (docs/architecture/data-layer.md 의
 * Mutex 지침은 SharedPreferences 전용)
 */
abstract class BaseLocalDataSource(
    private val dataStore: DataStore<Preferences>,
) {
    private val safeData: Flow<Preferences> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }

    /** [key] 값의 변경을 구독한다. 미저장 시 null. */
    protected fun <T> observe(key: Preferences.Key<T>): Flow<T?> =
        safeData.map { prefs -> prefs[key] }

    /** [key] 의 현재 값을 1회 읽는다. 미저장 시 null. */
    protected suspend fun <T> read(key: Preferences.Key<T>): T? =
        safeData.first()[key]

    /** [key] 에 [value] 를 저장한다. */
    protected suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /** [key] 를 삭제한다. */
    protected suspend fun <T> remove(key: Preferences.Key<T>) {
        dataStore.edit { prefs -> prefs.remove(key) }
    }

    /** 이 DataStore 파일의 모든 키를 삭제한다. */
    protected suspend fun clearAll() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    /** 여러 키를 한 트랜잭션으로 원자적 갱신할 때 사용한다 (예: access+refresh 토큰 쌍). */
    protected suspend fun editAtomically(block: (MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }
}
