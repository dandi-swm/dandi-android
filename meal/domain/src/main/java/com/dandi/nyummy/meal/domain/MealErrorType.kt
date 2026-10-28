package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

/**
 * 식사 기록(사진 업로드, 식사 생성) API 도메인 에러.
 *
 * `type` 은 서버 공통 에러 바디의 `code` 값과 일치해야 매칭된다.
 * 서버 `message` 는 디버그용이라 화면에는 [errorMsg] 만 보여 준다.
 * 서버의 MealErrorCode, S3ErrorCode가 바뀌면 여기도 같이 고친다. 기록하지 못한 경우는 모두 에러 코드로 오므로
 * 서버가 코드를 새로 알려 주면 항목을 더하고, 여기 없는 코드는 UseCase 의 기본 안내로 처리된다.
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
    DUPLICATE_IMAGE_KEY(
        type = "api.meal.duplicateImageKey",
        errorMsg = "이미 기록한 사진이에요",
    ),
    /**
     * 오늘 기록할 수 있는 횟수를 다 썼다(409). 지우거나 분석에 실패한 기록도 횟수에 들어간다.
     * 사진 업로드 주소 발급과 식사 생성 두 단계에서 온다. 다시 시도해도 내일까지는 성공하지 않는다.
     */
    DAILY_COUNT_EXCEEDED(
        type = "api.meal.dailyCountExceeded",
        errorMsg = "오늘은 더 기록할 수 없어요. 내일 다시 기록해주세요",
    ),
    UNSUPPORTED_CONTENT_TYPE(
        type = "api.s3.unsupportedContentType",
        errorMsg = "기록할 수 없는 사진 형식이에요. 다시 찍어주세요",
    ),
    FILE_SIZE_EXCEEDED(
        type = "api.s3.fileSizeExceeded",
        errorMsg = "사진 용량이 너무 커요. 다시 찍어주세요",
    ),
    /** 튜토리얼 식사를 이미 했다(409). 다른 기기에서 먼저 마친 경우라 안내하지 않고 마친 것으로 본다. */
    TUTORIAL_ALREADY_COMPLETED(
        type = "api.meal.tutorialAlreadyCompleted",
        errorMsg = "이미 튜토리얼을 마쳤어요",
    ),
}
