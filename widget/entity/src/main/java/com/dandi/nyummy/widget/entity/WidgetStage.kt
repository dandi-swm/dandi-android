package com.dandi.nyummy.widget.entity

/**
 * 기기 홈 화면 위젯이 보이는 상태. 상태마다 피그마 `Widget · 20 사용 시안 · 상태별`의 한 줄이다.
 *
 * - 끼니 단위(연속 기록이 살아 있는 날): [NIGHT], [WAITING], [FULL], [MISSED_ONE], [MISSED_TWO], [MISSED_ALL]
 * - 일 단위(연속이 끊긴 뒤): [AWAY]
 * - 그 밖: [REUNION], [LOGGED_OUT]
 */
enum class WidgetStage {
    /** 자정부터 첫 끼니 기다림 전까지, 오늘 아직 기록하지 않음 */
    NIGHT,

    /** 먹지 않은 끼니의 끼니 시각 1시간 전부터 마감까지 */
    WAITING,

    /** 기록한 뒤 다음 기다림까지(마지막 끼니면 자정까지) */
    FULL,

    /** 마지막 기록 뒤로 마감이 지난 끼니가 1개 */
    MISSED_ONE,

    /** 마지막 기록 뒤로 마감이 지난 끼니가 2개 이상(먹는 끼니를 모두 놓친 건 아님) */
    MISSED_TWO,

    /** 오늘 기록이 없고 먹는 끼니를 모두 놓침(집사를 찾습니다) */
    MISSED_ALL,

    /** 하루 이상을 통째로 거름. 거른 날 수는 [WidgetPlanVO.awayDays] */
    AWAY,

    /** 연속이 끊긴 뒤 다시 기록함. 그 끼니의 [FULL] 자리를 대신한다 */
    REUNION,

    /** 로그인하지 않았거나 로그인이 풀림 */
    LOGGED_OUT,
}
