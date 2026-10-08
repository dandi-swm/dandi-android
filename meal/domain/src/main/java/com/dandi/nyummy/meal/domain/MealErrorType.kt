package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

/**
 * 식사 기록(사진 업로드, 식사 생성) API 도메인 에러.
 *
 * `type` 은 서버 공통 에러 바디의 `code` 값과 일치해야 매칭된다.
 * 서버 `message` 는 디버그용이라 화면에는 [errorMsg] 만 보여 준다.
 * 서버의 MealErrorCode, S3ErrorCode가 바뀌면 여기도 같이 고친다.
 */
enum class MealErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    STALE_IMAGE(
        type = "api.meal.staleImage",
        errorMsg = "찍은 지 오래된 사진은 기록할 수 없어요. 다시 찍어주세요",
    ),
    CAPTURE_TIME_NOT_FOUND(
        type = "api.meal.captureTimeNotFound",
        errorMsg = "촬영 시각을 확인할 수 없는 사진이에요. 다시 찍어주세요",
    ),
    DUPLICATE_IMAGE_KEY(
        type = "api.meal.duplicateImageKey",
        errorMsg = "이미 기록한 사진이에요",
    ),
    UNSUPPORTED_CONTENT_TYPE(
        type = "api.s3.unsupportedContentType",
        errorMsg = "기록할 수 없는 사진 형식이에요. 다시 찍어주세요",
    ),
    FILE_SIZE_EXCEEDED(
        type = "api.s3.fileSizeExceeded",
        errorMsg = "사진 용량이 너무 커요. 다시 찍어주세요",
    ),
}
