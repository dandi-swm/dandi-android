package com.dandi.nyummy.achievement.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

/** 업적 화면 상태를 변이시키는 내부 이벤트. 준비 중 화면이라 아직 항목이 없다. */
// TODO: 업적 기능 구현 시 실제 이벤트를 추가한다.
sealed interface AchievementReducerEvent : ReducerEvent
