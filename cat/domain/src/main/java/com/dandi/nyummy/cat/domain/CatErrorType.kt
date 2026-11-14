package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.entity.cat.CatNameValidator

/**
 * 고양이 변경(이름 바꾸기) 도메인 에러.
 * `type` 은 서버 공통 에러 바디의 `code` 값과 일치해야 매칭된다. 화면에는 [errorMsg] 만 보여 준다.
 * 다시 보내도 같은 결과라 StringResource.RETRY 없이 안내만 한다.
 */
enum class CatErrorType(
    override val type: String,
    override val errorMsg: StringResource,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    /** 이름이 너무 길다(400). 앱이 서버와 같은 길이로 먼저 막으므로 보통은 오지 않는다. */
    NAME_TOO_LONG(
        type = "api.cat.nameTooLong",
        errorMsg = StringResource.CAT_NAME_TOO_LONG,
    ),

    /** 공백만 있는 이름 등 서버 규칙에 맞지 않는 값(400). 앱이 먼저 막으므로 보통은 오지 않는다. */
    INVALID_INPUT(
        type = "api.common.invalidInputValue",
        errorMsg = StringResource.CAT_INVALID_NAME,
    ),

    /** 고양이가 없다(404). 공통 404 안내("준비 중인 기능")가 맞지 않아 따로 둔다. */
    CAT_NOT_FOUND(
        type = "api.cat.notFound",
        errorMsg = StringResource.CAT_NOT_FOUND,
    ),
}
