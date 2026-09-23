package com.dandi.nyummy.common.domain.helper

import com.dandi.nyummy.common.domain.analysis.MealAnalysisEvent
import kotlinx.coroutines.flow.Flow

/**
 * 식사 분석 완료 이벤트를 발행/구독하는 단일 통로입니다.
 *
 * FCM 수신부(app 모듈)가 [publish] 하고, 화면(ViewModel)이 [events] 를 구독해 스스로 재조회한다.
 * 구독자가 여럿일 수 있으므로 [NavigationHelper] 의 Channel 과 달리 브로드캐스트 형태로 동작한다.
 */
interface MealAnalysisEventHelper {
    val events: Flow<MealAnalysisEvent>
    fun publish(event: MealAnalysisEvent)
}
