package com.dandi.nyummy.common.data.remoteconfig

import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.Json

/**
 * Firebase 원격 구성 기반 [RemoteConfigHelper].
 *
 * 키 이름은 [env]에 따라 고른다(release 빌드는 운영 키, 그 밖의 빌드는 `_debug` 키).
 * 콘솔에 값이 없거나 받지 못하면 각 키의 기본값을 쓴다. 최소 versionCode 기본값이 0이라 받지 못해도 강제 업데이트가 걸리지 않는다.
 */
class FirebaseRemoteConfigHelper(
    private val remoteConfig: FirebaseRemoteConfig,
    private val json: Json,
    private val env: RemoteConfigEnv,
) : RemoteConfigHelper {

    private val syncMutex = Mutex()

    private var isSynced = false

    override suspend fun sync() {
        syncMutex.withLock {
            if (isSynced) return
            runCatching { remoteConfig.fetchAndActivate().await() }
                .onSuccess { isSynced = true } // 실패하면 다음 sync() 호출에서 다시 시도한다.
        }
    }

    override fun <T> get(key: RemoteConfigKey<T>): T {
        val value = remoteConfig.getValue(env.keyOf(key.key))
        // 콘솔에 키가 없거나 아직 받지 못했다.
        if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return key.defaultValue
        return decodeRemoteConfigValue(value, key.serializer, json) ?: key.defaultValue
    }
}

/**
 * 원격 구성 값 하나를 [serializer]의 타입으로 읽는다. 원시 타입은 Firebase의 타입별 조회로, 그 밖의 타입은 문자열을 JSON으로 읽는다.
 * 형식이 맞지 않으면(숫자 자리에 글자 등) null.
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> decodeRemoteConfigValue(
    value: FirebaseRemoteConfigValue,
    serializer: KSerializer<T>,
    json: Json,
): T? = runCatching {
    when (serializer.descriptor.kind) {
        PrimitiveKind.BOOLEAN -> value.asBoolean()
        PrimitiveKind.INT -> value.asLong().toInt()
        PrimitiveKind.LONG -> value.asLong()
        PrimitiveKind.DOUBLE -> value.asDouble()
        PrimitiveKind.FLOAT -> value.asDouble().toFloat()
        PrimitiveKind.STRING -> value.asString()
        else -> json.decodeFromString(serializer, value.asString())
    } as T
}.getOrNull()
