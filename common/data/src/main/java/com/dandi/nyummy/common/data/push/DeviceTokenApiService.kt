package com.dandi.nyummy.common.data.push

import com.dandi.nyummy.common.data.push.dto.DeviceTokenRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 푸시 토큰 등록 API. 로그인한 사용자 기준이라 인증 Retrofit으로 만든다.
 * 서버는 사용자당 한 행만 두고 다시 부르면 토큰을 교체한다(멱등). 성공은 본문 없는 200이다.
 */
interface DeviceTokenApiService {
    @POST("/api/v1/notifications/device-tokens")
    suspend fun register(@Body request: DeviceTokenRequestDTO): Response<Unit>
}
