package com.dandi.nyummy.common.data.tti

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseTTITraceValuesTest {

    private val introResult = mapOf<String, Any?>(
        "tti.page_name" to "intro",
        "tti.instance_no" to 1L,
        "tti.is_bounced" to false,
        "tti.is_timeout" to false,
        "tti.tti_log_version" to 1,
        "tti.tti_time" to 1_065_053_626L,
        "tti.view_creation_time" to -1,
        "tti.api_request_ready_time" to -1,
        "tti.api_response_time" to 900_214_208L,
        "tti.view_binding_time" to -1,
        "tti.image_loaded_time" to -1,
        "tti.user_wait_included" to true,
    )

    @Test
    fun `측정한 구간만 ms metric 으로 바꾼다`() {
        val values = FirebaseTTITraceValues.from(introResult)

        assertEquals(mapOf("tti_ms" to 1065L, "api_response_ms" to 900L), values.metrics)
    }

    @Test
    fun `필터에 쓸 값만 attribute 로 넣고 page_name 과 instance_no 는 뺀다`() {
        val values = FirebaseTTITraceValues.from(introResult)

        assertEquals(
            mapOf(
                "is_bounced" to "false",
                "is_timeout" to "false",
                "user_wait_included" to "true",
                "tti_log_version" to "1",
            ),
            values.attributes,
        )
    }

    @Test
    fun `없는 attribute 는 넣지 않는다`() {
        val values = FirebaseTTITraceValues.from(mapOf("tti.is_timeout" to true))

        assertEquals(mapOf("is_timeout" to "true"), values.attributes)
    }

    @Test
    fun `트레이스 이름은 화면별로 tti_ 접두어를 붙인다`() {
        assertEquals("tti_intro", FirebaseTTITraceValues.traceNameOf("intro"))
    }
}
