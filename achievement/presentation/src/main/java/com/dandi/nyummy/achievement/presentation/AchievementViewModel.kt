package com.dandi.nyummy.achievement.presentation

import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AchievementViewModel @Inject constructor() :
    MviViewModel<AchievementIntent, AchievementUIState, AchievementReducerEvent>(AchievementUIState.empty) {

    // TODO: 업적 API 연동 시 UseCase 호출, 인텐트 처리, 리듀서를 채운다.
    override fun onIntent(intent: AchievementIntent) = Unit

    override fun reduce(state: AchievementUIState, event: AchievementReducerEvent): AchievementUIState = state
}
