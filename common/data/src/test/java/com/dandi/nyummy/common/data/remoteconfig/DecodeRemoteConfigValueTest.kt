package com.dandi.nyummy.common.data.remoteconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecodeRemoteConfigValueTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `원시 타입은 Firebase 타입별 조회로 읽는다`() {
        assertEquals(true, decodeRemoteConfigValue(FakeValue("true"), Boolean.serializer(), json))
        assertEquals(4L, decodeRemoteConfigValue(FakeValue("4"), Long.serializer(), json))
        assertEquals(4, decodeRemoteConfigValue(FakeValue("4"), Int.serializer(), json))
        assertEquals(1.5, decodeRemoteConfigValue(FakeValue("1.5"), Double.serializer(), json)!!, 0.0)
        assertEquals("링크", decodeRemoteConfigValue(FakeValue("링크"), String.serializer(), json))
    }

    @Test
    fun `목록과 VO는 문자열을 JSON으로 읽는다`() {
        assertEquals(
            listOf(1000L, 2000L),
            decodeRemoteConfigValue(FakeValue("[1000, 2000]"), ListSerializer(Long.serializer()), json),
        )
        assertEquals(
            Sample(title = "제목"),
            decodeRemoteConfigValue(FakeValue("""{"title":"제목","extra":1}"""), Sample.serializer(), json),
        )
    }

    @Test
    fun `형식이 맞지 않으면 null 이라 호출부가 기본값을 쓴다`() {
        assertNull(decodeRemoteConfigValue(FakeValue("abc"), Long.serializer(), json))
        assertNull(decodeRemoteConfigValue(FakeValue("maybe"), Boolean.serializer(), json))
        assertNull(decodeRemoteConfigValue(FakeValue("{not json"), Sample.serializer(), json))
    }

    @Serializable
    private data class Sample(val title: String = "")

    /** Firebase 값 흉내. 문자열 하나로 타입별 조회를 Firebase와 같은 규칙으로 돌려준다. */
    private class FakeValue(private val raw: String) : FirebaseRemoteConfigValue {
        override fun asLong(): Long = raw.toLongOrNull() ?: throw IllegalArgumentException(raw)
        override fun asDouble(): Double = raw.toDoubleOrNull() ?: throw IllegalArgumentException(raw)
        override fun asString(): String = raw
        override fun asByteArray(): ByteArray = raw.toByteArray()
        override fun asBoolean(): Boolean = when (raw) {
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException(raw)
        }
        override fun getSource(): Int = FirebaseRemoteConfig.VALUE_SOURCE_REMOTE
    }
}
