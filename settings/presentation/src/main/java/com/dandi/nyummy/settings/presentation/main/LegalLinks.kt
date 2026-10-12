package com.dandi.nyummy.settings.presentation.main

import com.dandi.nyummy.settings.entity.LegalDocument

/**
 * 약관 문서 주소. 운영, 법무에서 주소가 정해지면 채운다(SRS TBD-006).
 * 비어 있는 동안은 눌러도 열지 못했다는 스낵바가 뜬다.
 */
internal val LegalDocument.url: String
    get() = when (this) {
        LegalDocument.TERMS -> TERMS_URL
        LegalDocument.PRIVACY -> PRIVACY_URL
    }

// TODO(legal): 약관 주소가 정해지면 채운다.
private const val TERMS_URL = ""
private const val PRIVACY_URL = ""
