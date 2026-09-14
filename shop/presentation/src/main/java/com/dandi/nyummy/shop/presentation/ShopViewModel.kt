package com.dandi.nyummy.shop.presentation

import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ShopViewModel @Inject constructor() :
    MviViewModel<ShopIntent, ShopUIState, ShopReducerEvent>(ShopUIState.empty) {

    // TODO: 상점 API 연동 시 UseCase 호출·인텐트 처리·리듀서를 채운다.
    override fun onIntent(intent: ShopIntent) = Unit

    override fun reduce(state: ShopUIState, event: ShopReducerEvent): ShopUIState = state
}
