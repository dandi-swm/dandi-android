package com.dandi.nyummy.quest.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

/** 퀘스트 화면 상태를 변이시키는 내부 이벤트. 준비 중 화면이라 아직 항목이 없다. */
// TODO: 퀘스트 기능 구현 시 실제 이벤트를 추가한다.
sealed interface QuestReducerEvent : ReducerEvent
