package com.dandi.nyummy.intro.data.dto

import com.dandi.nyummy.common.entity.UNKNOWN
import com.dandi.nyummy.intro.entity.IntroVO
import kotlinx.serialization.Serializable

/**
 * GET /intro 응답.
 *
 * @property devTestMsg 서버 개발용 테스트 메시지
 * @property minAppVersion 최소 요구 앱 버전
 * @property recommendAppVersion 권장 앱 버전
 */
@Serializable
data class IntroDTO(
    val devTestMsg: String? = null,
    val minAppVersion: String? = null,
    val recommendAppVersion: String? = null,
) {
    fun toVO(): IntroVO = IntroVO(
        devTestMsg = devTestMsg ?: UNKNOWN,
        minAppVersion = minAppVersion ?: UNKNOWN,
        recommendAppVersion = recommendAppVersion ?: UNKNOWN,
    )
}
