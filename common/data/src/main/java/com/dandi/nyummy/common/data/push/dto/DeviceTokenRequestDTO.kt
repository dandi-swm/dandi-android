package com.dandi.nyummy.common.data.push.dto

import kotlinx.serialization.Serializable

/**
 * 푸시 토큰 등록 요청입니다.
 *
 * @property token 이 기기의 FCM 토큰
 * @property platform 서버 `DevicePlatform` 값(`ANDROID` | `IOS`). 서버 필수값이다.
 *   기본값을 두면 직렬화가 기본값과 같은 필드를 빼고 보내므로(encodeDefaults 꺼짐) 기본값 없이 넘긴다.
 */
@Serializable
data class DeviceTokenRequestDTO(
    val token: String,
    val platform: String,
) {
    companion object {
        const val PLATFORM_ANDROID = "ANDROID"
    }
}
