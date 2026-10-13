package com.dandi.nyummy.settings.data

import com.dandi.nyummy.settings.data.dto.MealTimeRequestDTO
import com.dandi.nyummy.settings.data.dto.PushSettingDTO
import com.dandi.nyummy.settings.data.dto.UserProfileDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT

interface SettingsApiService {

    /** 내 프로필. 설정은 여기서 평소 식사 시각만 읽는다. */
    @GET(USERS_ME_PATH)
    suspend fun getMyProfile(): Response<UserProfileDTO>

    /** 평소 식사 시각을 세 끼 한 번에 바꾼다. 빠지거나 null인 끼니는 알림을 받지 않는다. 성공은 본문 없는 200. */
    @PUT("$USERS_ME_PATH/meal-time")
    suspend fun updateMealTime(@Body request: MealTimeRequestDTO): Response<Unit>

    /** 알림 수신 설정(서비스 알림, 마케팅 알림). */
    @GET("$USERS_ME_PATH/push")
    suspend fun getPushSetting(): Response<PushSettingDTO>

    /** 알림 수신 설정을 바꾼다. 담은 항목만 바뀌고 빠진 항목은 그대로다. 성공은 본문 없는 200. */
    @PATCH("$USERS_ME_PATH/push")
    suspend fun updatePushSetting(@Body request: PushSettingDTO): Response<Unit>

    /** 회원 탈퇴. 되돌릴 수 없다. 성공은 본문 없는 204. */
    @DELETE(USERS_ME_PATH)
    suspend fun withdraw(): Response<Unit>

    companion object {
        const val USERS_ME_PATH = "/api/v1/users/me"
    }
}
