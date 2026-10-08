package com.dandi.nyummy.quest.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState

/** 퀘스트 화면 상태. 준비 중 화면이라 아직 담는 값이 없다. */
data class QuestUIState(
    val placeholder: Unit = Unit,
) : UiState {

    companion object {
        val empty = QuestUIState()
    }
}
