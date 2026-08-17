package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

enum class IntroErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    REQUIRED_FORCE_UPDATE(
        type = "api.intro.requiredForceUpdate",
        errorMsg = "현재 앱이 최소 요구 버전을 만족하지 않습니다.\nPlay Store에서 최신버전을 업데이트 해주세요.",
        isHandledOnDomain = true,
    ),
}
