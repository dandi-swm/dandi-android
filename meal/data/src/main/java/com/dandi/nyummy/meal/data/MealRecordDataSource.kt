package com.dandi.nyummy.meal.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.meal.data.dto.CreateMealRequestDTO
import com.dandi.nyummy.meal.data.dto.CreatedMealDTO
import com.dandi.nyummy.meal.data.dto.MealImageUploadDTO
import com.dandi.nyummy.meal.data.dto.UploadImageUrlRequestDTO
import okhttp3.RequestBody

class MealRecordDataSource(
    private val apiService: MealRecordApiService,
    private val uploadApiService: MealImageUploadApiService,
) : BaseRemoteDataSource() {
    suspend fun issueImageUploadUrl(request: UploadImageUrlRequestDTO): MealImageUploadDTO =
        checkResponse(apiService.issueImageUploadUrl(request))

    suspend fun uploadImage(uploadUrl: String, headers: Map<String, String>, body: RequestBody) =
        checkResponse(uploadApiService.uploadImage(uploadUrl, headers, body))

    suspend fun createMeal(request: CreateMealRequestDTO): CreatedMealDTO =
        checkResponse(apiService.createMeal(request))
}
