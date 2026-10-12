package com.dandi.nyummy.settings.data

import com.dandi.nyummy.settings.data.dto.MealTimeRequestDTO
import com.dandi.nyummy.settings.data.dto.UserProfileDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface SettingsApiService {

    /** 내 프로필. 설정은 여기서 평소 식사 시각만 읽는다. */
    @GET(USERS_ME_PATH)
    suspend fun getMyProfile(): Response<UserProfileDTO>

    /** 평소 식사 시각을 세 끼 한 번에 바꾼다. 빠지거나 null인 끼니는 알림을 받지 않는다. 성공은 본문 없는 200. */
    @PUT("$USERS_ME_PATH/meal-time")
    suspend fun updateMealTime(@Body request: MealTimeRequestDTO): Response<Unit>

    companion object {
        const val USERS_ME_PATH = "/api/v1/users/me"
    }
}
