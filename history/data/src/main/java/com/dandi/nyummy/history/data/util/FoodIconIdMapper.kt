package com.dandi.nyummy.history.data.util

import com.dandi.nyummy.history.entity.MealAnalysisStatus

/**
 * 서버 iconId를 화면에 쓸 음식 아이콘 식별자로 바꾼다. 분석이 끝난 식사만 아이콘을 쓰고, 나머지는 빈 값이다.
 * 서버는 분석 전이거나 실패한 식사에도 기본 아이콘(1)을 내려 주므로, 그대로 쓰면 엉뚱한 음식 아이콘이 보인다.
 */
internal fun foodIconIdOf(iconId: Long?, status: MealAnalysisStatus): String =
    if (status == MealAnalysisStatus.COMPLETED && iconId != null) iconId.toString() else ""
