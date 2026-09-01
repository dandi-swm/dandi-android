package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.MealImageUploadVO

interface MealRecordRepository {

    /** 촬영본 파일 정보로 presigned 업로드 URL 과 이미지 키를 발급받는다. */
    suspend fun issueImageUploadUrl(photoPath: String): MealImageUploadVO

    /** 발급받은 presigned URL 로 촬영본을 업로드한다. */
    suspend fun uploadImage(uploadTarget: MealImageUploadVO, photoPath: String)

    /** 업로드된 이미지 키로 식사를 생성하고 분석을 시작시킨다. */
    suspend fun createMeal(imageKey: String): CreatedMealVO
}
