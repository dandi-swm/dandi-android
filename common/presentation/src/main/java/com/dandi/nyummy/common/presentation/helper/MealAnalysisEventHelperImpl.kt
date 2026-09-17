package com.dandi.nyummy.common.presentation.helper

import com.dandi.nyummy.common.domain.analysis.MealAnalysisEvent
import com.dandi.nyummy.common.domain.helper.MealAnalysisEventHelper
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 구독자가 없을 때 발행된 이벤트는 흘려보낸다 — 화면이 열려 있지 않다면
 * 다음 진입 시 어차피 최신 데이터를 새로 조회하기 때문이다.
 */
class MealAnalysisEventHelperImpl : MealAnalysisEventHelper {
    private val _events = MutableSharedFlow<MealAnalysisEvent>(
        extraBufferCapacity = EVENT_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: Flow<MealAnalysisEvent> = _events.asSharedFlow()

    override fun publish(event: MealAnalysisEvent) {
        _events.tryEmit(event)
    }

    private companion object {
        const val EVENT_BUFFER_CAPACITY = 8
    }
}
