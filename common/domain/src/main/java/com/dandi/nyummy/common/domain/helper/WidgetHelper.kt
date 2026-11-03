package com.dandi.nyummy.common.domain.helper

import com.dandi.nyummy.common.entity.meal.MealTimesVO

/**
 * 기기 홈 화면 위젯에 앱이 아는 값을 넘긴다. 위젯은 이 값과 기기 시계만으로 지금 상태를 계산한다.
 *
 * 여러 기능(식사 기록, 홈, 온보딩, 설정, 인증)이 값을 넘기지만 위젯 모듈을 모르게 하려고 공통 Helper로 둔다.
 * 구현은 값을 기기에 남긴 뒤 위젯을 다시 그린다. 저장에 실패해도 예외를 던지지 않는다(위젯 갱신이 본래 흐름을 막지 않게).
 */
interface WidgetHelper {
    /** 식사 접수에 성공한 순간 부른다. 튜토리얼 식사는 넘기지 않는다. */
    suspend fun onMealRecorded()

    /** 홈 요약을 받을 때마다 부른다. */
    suspend fun onHomeSummary(streakDays: Int, todayRecordedCount: Int)

    /** 온보딩이나 설정에서 끼니 시각 저장에 성공하면 부른다. */
    suspend fun onMealTimesSaved(mealTimes: MealTimesVO)

    /** 로그인·회원가입·로그아웃·탈퇴 때 부른다. 다른 계정의 기록이 섞이지 않게 모두 지운다. */
    suspend fun clear()
}
