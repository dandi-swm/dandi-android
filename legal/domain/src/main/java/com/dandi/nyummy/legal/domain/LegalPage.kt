package com.dandi.nyummy.legal.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.legal.entity.LegalDocument

/**
 * 약관 문서 보기. 로그인하지 않아도 열린다.
 * 문서마다 경로를 따로 두어, 링크로 열어도 없는 문서를 가리킬 일이 없다.
 */
data class LegalPage(val document: LegalDocument) : Page {

    override fun toRoute(): NavRoute = NavRoute(pathOf(document))

    companion object {
        const val TERMS_PATH = "/legal/terms"
        const val PRIVACY_PATH = "/legal/privacy"

        fun pathOf(document: LegalDocument): String = when (document) {
            LegalDocument.TERMS -> TERMS_PATH
            LegalDocument.PRIVACY -> PRIVACY_PATH
        }
    }
}
