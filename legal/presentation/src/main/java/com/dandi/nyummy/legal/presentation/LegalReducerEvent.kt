package com.dandi.nyummy.legal.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

sealed interface LegalReducerEvent : ReducerEvent {
    data object LoadStarted : LegalReducerEvent
    data object LoadFinished : LegalReducerEvent
    data object LoadFailed : LegalReducerEvent
    data class HistoryChanged(val shownPage: PolicyPage?, val canGoBack: Boolean) : LegalReducerEvent
}
