package com.dandi.nyummy.quest.presentation

import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class QuestViewModel @Inject constructor() :
    MviViewModel<QuestIntent, QuestUIState, QuestReducerEvent>(QuestUIState.empty) {

    // TODO: 퀘스트 API 연동 시 UseCase 호출, 인텐트 처리, 리듀서를 채운다.
    override fun onIntent(intent: QuestIntent) = Unit

    override fun reduce(state: QuestUIState, event: QuestReducerEvent): QuestUIState = state
}
