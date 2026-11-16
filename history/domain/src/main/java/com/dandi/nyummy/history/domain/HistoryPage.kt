package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.history.entity.MealAnalysisVersion

/**
 * 히스토리(캘린더/식사 기록 조회) 화면으로 이동하기 위한 네비게이션 정보입니다.
 *
 * [PATH]는 앱 내 이동, 백스택, 화면 등록, 딥링크에서 공통으로 사용하는 히스토리 화면의 식별자입니다.
 */
object HistoryPage : Page {

    const val PATH = "/history"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/**
 * 식사 하나의 상세 화면. 기록 화면에서 식사를 누르거나, 분석 결과 알림의 식사 링크(`/meals/{mealId}`)로 들어온다.
 * 뒤로 가면 기록 화면이다.
 *
 * @property analysisVersion 식사를 분석한 방식. 기록 화면은 목록에서 받은 값을 넘기고, 링크로 들어오면 모르므로 null이다.
 * null이면 상세 화면이 서버에 물어 정한다.
 */
data class MealDetailPage(
    val mealId: Long,
    val analysisVersion: MealAnalysisVersion? = null,
) : Page {

    override fun toRoute(): NavRoute = NavRoute(
        path = PATH,
        args = buildMap {
            put(ARG_MEAL_ID, mealId.toString())
            analysisVersion?.let { put(ARG_VERSION, versionArgOf(it)) }
        },
    )

    companion object {
        const val ARG_MEAL_ID = "mealId"

        /** 분석 버전 인자(`?v=1` 또는 `?v=2`). 없으면 모르는 것이다. */
        const val ARG_VERSION = "v"
        const val PATH = "/history/meals/{$ARG_MEAL_ID}"

        private const val VERSION_V1 = "1"
        private const val VERSION_V2 = "2"

        private fun versionArgOf(version: MealAnalysisVersion): String = when (version) {
            MealAnalysisVersion.V1 -> VERSION_V1
            MealAnalysisVersion.V2 -> VERSION_V2
        }

        /** [ARG_VERSION] 값을 분석 버전으로 읽는다. 없거나 모르는 값이면 null이다. */
        fun versionOf(arg: String?): MealAnalysisVersion? = when (arg) {
            VERSION_V1 -> MealAnalysisVersion.V1
            VERSION_V2 -> MealAnalysisVersion.V2
            else -> null
        }
    }
}
