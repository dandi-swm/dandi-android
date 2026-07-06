package com.swm.dandi.sprite.domain

import com.swm.dandi.common.domain.error.HttpErrorType

/**
 * sprite 기능에서 서버/API 실패를 매핑할 때 사용할 error type.
 *
 * 현재 sprite 는 로컬 리소스 기반 renderer/sample 이라 별도 복구 가능한 에러 흐름이 없다.
 * 추후 sprite manifest 나 asset 정보를 API/저장소에서 가져오게 되면 이 enum 을
 * 실제 매핑 지점으로 사용한다.
 */
enum class SpriteErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    UNKNOWN(
        type = "api.sprite.unknown",
        errorMsg = "알수없는 에러가 발생했습니다.",
        isHandledOnDomain = true,
    ),
}
