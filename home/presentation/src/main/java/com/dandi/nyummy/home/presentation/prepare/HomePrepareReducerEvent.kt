package com.dandi.nyummy.home.presentation.prepare

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

sealed interface HomePrepareReducerEvent : ReducerEvent {

    /** 냐미 미리 받기가 끝났다(받지 못했거나 기다림 상한에 닿은 경우도 포함). */
    data object PrepareCompleted : HomePrepareReducerEvent
}
