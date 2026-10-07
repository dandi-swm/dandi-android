package com.dandi.nyummy.cat.entity

/**
 * 냐미의 감정 상태. 서버 애니메이션 메타데이터의 `state` 값과 이름이 같다.
 * 어떤 상황에 어떤 상태를 보여 줄지는 서버가 정하지 않고 앱이 고른다.
 */
enum class CatState {
    /** 무기록 1일째 저녁 */
    ANXIOUS,

    /** 기록 목록이나 분석 결과를 보는 중 */
    ATTENTIVE,

    /** 오늘 아직 기록 없음, 분석 실패 후 */
    CONFLICTED_CAUTIOUS,

    /** 기록 직후 먹는 반응 */
    CONTENT,

    /** 음식이 아닌 사진 */
    DISGUSTED,

    /** 며칠 만에 다시 들어옴, 밥 주기 직전 */
    EXCITED,

    /** 홈 진입 기본 인사 */
    FRIENDLY,

    /** 최고 컨디션 */
    FRIENDLY_RELAXED,

    /** 자정 임박, 연속 기록이 끊길 위기 */
    FRIGHTENED,

    /** 카메라를 켰을 때, 새 음식 사진 */
    INTERESTED,

    /** 캐릭터 연타 */
    IRRITATED,

    /** 마이룸 꾸미기, 캐릭터 터치 */
    PLAYFUL,

    /** 뷰파인더 조준, 분석 중 */
    PREDATORY,

    /** 오늘 기록이 끝난 뒤 여유 */
    RELAXED,

    /** 연속 기록이 끊긴 순간 */
    SUPER_TERRIFIED,

    /** 무기록 3일째 직전 */
    TERRIFIED,

    /** 마이룸, 새 아이템 획득 */
    THIS_IS_MINE,

    /** 업로드나 네트워크 오류 */
    THREATENED,

    /** 연속 기록이 이어지는 중 */
    TRUSTING,

    /** 무기록 2일째 */
    WORRIED,

    /** 앱이 아직 모르는 상태. 서버에 상태가 새로 생겨도 앱이 죽지 않게 받아 둔다. */
    UNKNOWN,
    ;

    companion object {
        fun from(raw: String?): CatState =
            entries.firstOrNull { it != UNKNOWN && it.name == raw } ?: UNKNOWN
    }
}
