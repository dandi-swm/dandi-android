package com.dandi.nyummy.home.presentation.mock

/**
 * 홈 화면 목업 데이터. 백엔드 연동 전까지 디자인 시안(Figma `LIVE / Home · My Room`)과
 * 동일한 값을 보여주기 위한 임시 데이터다.
 */
// TODO: 오늘 식사 시트를 새 디자인 시스템으로 옮기며 GetDailyMealsUseCase 결과로 바꾸고 지운다.
internal object HomeMockData {

    // TODO: 오늘 식사 상세(매크로·식사 목록) API 연동 시 UseCase 결과로 교체하고 UIState 로 옮긴다.
    val sheetCarbohydrate = HomeMacroMock(currentGram = 185, goalGram = 240)
    val sheetProtein = HomeMacroMock(currentGram = 42, goalGram = 55)
    val sheetFat = HomeMacroMock(currentGram = 31, goalGram = 50)
    val todayMeals = listOf(
        HomeMealMock(name = "닭가슴살 샐러드", recordedAt = "12:24", calorieKcal = 1350),
    )
}

/** 오늘 식사 요약 시트의 탄·단·지 목업 값. */
internal data class HomeMacroMock(
    val currentGram: Int,
    val goalGram: Int,
) {
    /** 목표 대비 섭취 비율 정수 퍼센트. 목표가 없으면 0. */
    val percent: Int
        get() = if (goalGram <= 0) 0 else (currentGram * 100) / goalGram
}

/** 오늘 식사 요약 시트의 식사 한 건 목업 값. */
internal data class HomeMealMock(
    val name: String,
    val recordedAt: String,
    val calorieKcal: Int,
)
