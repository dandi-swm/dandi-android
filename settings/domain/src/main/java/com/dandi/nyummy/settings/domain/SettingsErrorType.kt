package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

/**
 * 설정 API(내 프로필, 식사 시각) 도메인 에러.
 *
 * `type` 은 서버 공통 에러 바디의 `code` 값과 일치해야 매칭된다.
 * 서버 `message` 는 디버그용이라 화면에는 [errorMsg] 만 보여 준다.
 */
enum class SettingsErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    /**
     * 식사 시각이 서버 규칙(0~23, 끼니끼리 겹치지 않음)에 맞지 않다(400).
     * 앱이 저장 전에 겹치는지 먼저 막으므로 보통은 오지 않는다.
     */
    INVALID_INPUT(
        type = "api.common.invalidInputValue",
        errorMsg = DUPLICATE_MEAL_TIMES_MESSAGE,
    ),

    /** 회원이 없다(404). 공통 404 안내("준비 중인 기능")가 맞지 않아 따로 둔다. */
    USER_NOT_FOUND(
        type = "api.user.notFound",
        errorMsg = "회원 정보를 찾지 못했어요. 다시 로그인해 주세요.",
    ),
}

/** 끼니끼리 같은 시각을 골랐을 때의 안내. 온보딩과 같은 문구다. */
internal const val DUPLICATE_MEAL_TIMES_MESSAGE = "끼니마다 다른 시각을 골라주세요."
