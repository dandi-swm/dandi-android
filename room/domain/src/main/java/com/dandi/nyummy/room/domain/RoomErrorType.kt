package com.dandi.nyummy.room.domain

import com.dandi.nyummy.common.domain.error.HttpErrorType

// TODO: 백엔드가 마이룸 API 에러 code를 정하면 type을 실제 값으로 바꾸고 쓰지 않는 항목은 지운다.
//  지금 서버는 요청과 관계없이 성공 고정 응답만 주므로 아래 code는 임시로 정한 이름이다.
/**
 * 마이룸 도메인 에러. `type`은 서버 에러 바디의 `code`와 철자까지 같아야 한다.
 */
enum class RoomErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    /** 방에 놓으려는 아이템을 이제 가지고 있지 않다(다른 기기에서 지웠다). 저장하지 않고 방을 다시 불러온다. */
    ROOM_ITEM_NOT_FOUND(
        type = "api.room.itemNotFound",
        errorMsg = "",
    ),
}

/** 저장하려던 방에 이제 가지고 있지 않은 아이템이 있다. 화면은 방을 다시 불러와 지금 가진 것으로 보여 준다. */
class RoomItemGoneException : Exception()
