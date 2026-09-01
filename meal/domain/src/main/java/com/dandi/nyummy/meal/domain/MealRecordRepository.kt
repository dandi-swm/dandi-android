package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.MealImageUploadVO

interface MealRecordRepository {

    /**
     * 업로드 전에 촬영본을 검증하고, 크기 상한을 넘으면 상한 이하로 재압축한다.
     * 검증·압축이 불가능하면 [MealPhotoInvalidException] 을 던진다.
     */
    suspend fun prepareUploadImage(photoPath: String)

    /** 촬영본 파일 정보로 presigned 업로드 URL 과 이미지 키를 발급받는다. */
    suspend fun issueImageUploadUrl(photoPath: String): MealImageUploadVO

    /** 발급받은 presigned URL 로 촬영본을 업로드한다. */
    suspend fun uploadImage(uploadTarget: MealImageUploadVO, photoPath: String)

    /** 업로드된 이미지 키로 식사를 생성하고 분석을 시작시킨다. */
    suspend fun createMeal(imageKey: String): CreatedMealVO
}
