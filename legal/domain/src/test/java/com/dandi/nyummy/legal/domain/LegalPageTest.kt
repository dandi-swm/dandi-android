package com.dandi.nyummy.legal.domain

import com.dandi.nyummy.legal.entity.LegalDocument
import org.junit.Assert.assertEquals
import org.junit.Test

class LegalPageTest {

    @Test
    fun `문서마다 자기 경로로 간다`() {
        assertEquals("/legal/terms", LegalPage(LegalDocument.TERMS).toRoute().path)
        assertEquals("/legal/privacy", LegalPage(LegalDocument.PRIVACY).toRoute().path)
    }

    @Test
    fun `경로에 인자를 싣지 않는다`() {
        LegalDocument.entries.forEach { document ->
            assertEquals(emptyMap<String, String>(), LegalPage(document).toRoute().args)
        }
    }
}
