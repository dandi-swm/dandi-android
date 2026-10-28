package com.dandi.nyummy.cat.entity

/**
 * 냐미 애니메이션을 고르는 앱의 상황. 앱 에셋 동작 목록 파일(`animations.json`)의 `state` 값과 이름이 같다.
 * 상황마다 어떤 동작을 얼마나 자주 보일지는 그 파일이 정하고, 언제 어느 상황인지는 앱이 정한다.
 */
enum class CatState {
    /** 홈, 낮(6~23시) 기본 */
    HOME_DAY,

    /** 홈, 낮에 한동안 건드리지 않음 */
    HOME_DROWSY,

    /** 홈, 23~6시 */
    HOME_NIGHT,

    /** 냐미를 누름 */
    HOME_TAP,

    /** 짧게 여러 번 누름 */
    HOME_TAP_BURST,

    /** 오늘 처음 홈에 들어옴 */
    HOME_GREET,

    /** 하루 넘어 홈에 들어옴 */
    HOME_WELCOME_BACK,

    /** 식사를 기록하고 홈에 돌아옴 */
    HOME_AFTER_MEAL,

    /** 마이룸의 냐미 */
    ROOM_IDLE,

    /** 앱이 모르는 상황. 파일에 새 상황이 있어도 앱이 죽지 않게 받아 둔다. */
    UNKNOWN,
    ;

    companion object {
        fun from(raw: String?): CatState =
            entries.firstOrNull { it != UNKNOWN && it.name == raw } ?: UNKNOWN
    }
}
