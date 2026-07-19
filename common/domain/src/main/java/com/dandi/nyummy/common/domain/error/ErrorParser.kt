package com.dandi.nyummy.common.domain.error

inline fun <reified ErrorType> HttpResponseException.handlingErrorOnUseCase(): ErrorType?
        where ErrorType : Enum<ErrorType>,
              ErrorType : HttpErrorType {
    return enumValues<ErrorType>().firstOrNull { (it.type == this.cause?.message) && it.isHandledOnDomain }
}

fun HttpResponseException.isCommonErrorHandling(): Boolean {
    return this.rawCode == 401 || this.rawCode == 404 || this.rawCode >= 500
}
