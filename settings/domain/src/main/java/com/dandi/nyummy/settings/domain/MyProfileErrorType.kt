package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType
import com.dandi.nyummy.common.domain.helper.StringResource

/**
 * 마이페이지(내 프로필 조회·수정) 도메인 에러.
 *
 * [SettingsErrorType]의 잘못된 입력 안내는 식사 시각 문구라 따로 둔다.
 * `type` 은 서버 공통 에러 바디의 `code` 값과 일치해야 매칭된다. 화면에는 [errorMsg] 만 보여 준다.
 */
enum class MyProfileErrorType(
    override val type: String,
    override val errorMsg: StringResource,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    /** 서버 규칙에 맞지 않는 값(400). 앱이 고를 수 있는 값만 보내므로 보통은 오지 않는다. */
    INVALID_INPUT(
        type = "api.common.invalidInputValue",
        errorMsg = StringResource.SETTINGS_PROFILE_INVALID_INPUT,
    ),

    /** 회원이 없다(404). 공통 404 안내("준비 중인 기능")가 맞지 않아 따로 둔다. */
    USER_NOT_FOUND(
        type = "api.user.notFound",
        errorMsg = StringResource.SETTINGS_USER_NOT_FOUND,
    ),
}
