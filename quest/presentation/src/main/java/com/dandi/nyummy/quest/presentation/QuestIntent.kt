package com.dandi.nyummy.quest.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 퀘스트 화면에서 발생하는 사용자 입력. 준비 중 화면이라 아직 항목이 없다. */
// TODO: 퀘스트 기능 구현 시 실제 인텐트를 추가한다.
sealed interface QuestIntent : MviIntent
