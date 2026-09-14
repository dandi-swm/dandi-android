package com.dandi.nyummy.collection.presentation

import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor() :
    MviViewModel<CollectionIntent, CollectionUIState, CollectionReducerEvent>(CollectionUIState.empty) {

    // TODO: 컬렉션 API 연동 시 UseCase 호출·인텐트 처리·리듀서를 채운다.
    override fun onIntent(intent: CollectionIntent) = Unit

    override fun reduce(state: CollectionUIState, event: CollectionReducerEvent): CollectionUIState =
        state
}
