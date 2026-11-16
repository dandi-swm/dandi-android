package com.dandi.nyummy.history.domain

import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.history.entity.MealFoodChangeVO
import com.dandi.nyummy.history.entity.MealFoodLineVO

/** 음식 목록 방식(V2) 식사의 상세 조회와 수정. */
interface MealV2Repository {

    /** 식사 단건 상세 조회 (GET /api/v2/meals/{mealId}). V1 식사도 조회되며 그때는 음식 줄이 비어 있다. */
    suspend fun getMeal(mealId: Long): MealDetailV2VO

    /** 식사 대표 이름 수정 (PUT /api/v2/meals/{mealId}). 음식 줄과 영양은 바뀌지 않는다. */
    suspend fun updateMealName(mealId: Long, name: String): MealDetailV2VO

    /** 식사 삭제 (DELETE /api/v2/meals/{mealId}). */
    suspend fun deleteMeal(mealId: Long)

    /**
     * 음식 목록 전체 보내기 (PUT /api/v2/meals/{mealId}/foods). 순서를 바꾸거나 음식을 추가할 때 쓴다.
     * 맨 위 줄이 대표 음식이 되고, 목록에 없는 기존 줄은 서버가 지운다.
     *
     * @return 보낸 뒤의 분석 상태. 값을 새로 구해야 하면 WAITING, 아니면 COMPLETED다.
     */
    suspend fun submitFoods(mealId: Long, foods: List<MealFoodLineVO>): MealAnalysisStatus

    /** 음식 줄 수정 (PATCH /api/v2/meals/{mealId}/foods/{mealFoodId}). [change]에 담긴 항목만 바꾼다. */
    suspend fun updateFood(mealId: Long, mealFoodId: Long, change: MealFoodChangeVO)

    /** 음식 줄 삭제 (DELETE /api/v2/meals/{mealId}/foods/{mealFoodId}). */
    suspend fun deleteFood(mealId: Long, mealFoodId: Long)

    /** 영양 분석 상태 조회 (GET /api/v2/meals/{mealId}/analysis). */
    suspend fun getAnalysisStatus(mealId: Long): MealAnalysisStatus
}
