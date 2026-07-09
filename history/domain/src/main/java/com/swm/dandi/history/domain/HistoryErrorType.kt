package com.swm.dandi.history.domain

import com.swm.dandi.common.domain.error.HttpErrorType

enum class HistoryErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    UNKNOWN(
        type = "api.history.unknown",
        errorMsg = "히스토리 정보를 불러오지 못했습니다.",
    ),
}
