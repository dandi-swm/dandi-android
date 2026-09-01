package com.dandi.nyummy.meal.entity

/**
 * 식사 사진을 외부 스토리지에 직접 업로드하기 위한 presigned 발급 정보입니다.
 *
 * @property uploadUrl 사진을 업로드할 presigned URL
 * @property imageKey 업로드 완료 후 식사 생성 요청에 사용하는 이미지 키
 * @property uploadMethod 업로드에 사용할 HTTP 메서드 (예: PUT)
 * @property uploadHeaders 업로드 요청에 그대로 실어야 하는 헤더 목록
 * @property expiresAt presigned URL 만료 시각 문자열
 */
data class MealImageUploadVO(
    val uploadUrl: String = "",
    val imageKey: String = "",
    val uploadMethod: String = "",
    val uploadHeaders: Map<String, String> = emptyMap(),
    val expiresAt: String = "",
) {
    companion object {
        val empty = MealImageUploadVO()
    }
}
