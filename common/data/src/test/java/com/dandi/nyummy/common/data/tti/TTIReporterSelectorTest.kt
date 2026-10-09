package com.dandi.nyummy.common.data.tti

import com.dandi.nyummy.tti.NoOpTTIReporter
import com.dandi.nyummy.tti.TTIReporter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class TTIReporterSelectorTest {

    private val remote = object : TTIReporter {
        override fun startView(key: String, name: String, attributes: Map<String, Any?>) = Unit
        override fun stopView(key: String, attributes: Map<String, Any?>) = Unit
    }

    @Test
    fun `release 가 아니면 외부 전송을 만들지 않고 NoOp 을 쓴다`() {
        var created = false

        val reporter = selectTTIReporter(isReleaseBuild = false) {
            created = true
            remote
        }

        assertSame(NoOpTTIReporter, reporter)
        assertFalse(created)
    }

    @Test
    fun `release 면 외부 전송 reporter 를 쓴다`() {
        assertSame(remote, selectTTIReporter(isReleaseBuild = true) { remote })
    }

    @Test
    fun `release 라도 외부 전송을 만들다 실패하면 NoOp 으로 떨어진다`() {
        val reporter = selectTTIReporter(isReleaseBuild = true) {
            throw IllegalStateException("Firebase 초기화 실패")
        }

        assertSame(NoOpTTIReporter, reporter)
    }
}
