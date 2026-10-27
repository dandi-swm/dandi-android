package com.dandi.nyummy.meal.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.presentation.coachmark.CoachMark
import com.dandi.nyummy.common.presentation.coachmark.CoachMarkKey
import com.dandi.nyummy.common.presentation.coachmark.ShowCoachMark
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialExpression
import java.io.File

/**
 * 식사 기록 튜토리얼의 사진 자리. 실제 파일이 아니라 앱에 든 예시 음식 사진([R.drawable.meal_tutorial_sample])을 뜻한다.
 * 사진 경로 하나로 튜토리얼인지 정해서, 되살린 화면도 같은 판단을 하고 예시 사진을 올리는 일이 없다.
 */
internal const val TUTORIAL_PHOTO_PATH = "meal_tutorial_sample"

/** 확인 단계의 담기 버튼. 식사 기록 튜토리얼이 가리킨다. */
internal val MealSubmitCoachMarkKey = CoachMarkKey("meal.submit")

/** 냐미가 준비한 예시 사진으로 하는 식사 기록 튜토리얼인지. */
val MealCameraPhase.isTutorial: Boolean
    get() = when (this) {
        MealCameraPhase.Preview -> false
        is MealCameraPhase.Captured -> photoPath == TUTORIAL_PHOTO_PATH
        is MealCameraPhase.Ceremony -> photoPath == TUTORIAL_PHOTO_PATH
    }

/** 사진 경로를 그리는 모델로 바꾼다. 튜토리얼이면 예시 사진 리소스, 아니면 캐시 파일. */
internal fun mealPhotoModel(photoPath: String): Any =
    if (photoPath == TUTORIAL_PHOTO_PATH) R.drawable.meal_tutorial_sample else File(photoPath)

/**
 * 식사 기록 튜토리얼 안내. 확인 단계에서 담기 버튼을 가리킨다. 건너뛸 수 없고, 뒤로 가기는 화면이 평소처럼 처리한다
 * (기록하지 않고 홈으로 돌아가면 홈이 다시 카메라 버튼을 가리킨다).
 */
@Composable
internal fun MealTutorialCoachMark(phase: MealCameraPhase) {
    val mark = if (phase is MealCameraPhase.Captured && phase.isTutorial) {
        CoachMark(
            title = stringResource(R.string.meal_tutorial_title),
            body = stringResource(R.string.meal_tutorial_body),
            expression = NyummyTutorialExpression.Cheer,
            target = MealSubmitCoachMarkKey,
        )
    } else {
        null
    }
    ShowCoachMark(mark)
}
