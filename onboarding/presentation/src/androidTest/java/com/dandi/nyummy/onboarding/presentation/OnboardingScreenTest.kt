package com.dandi.nyummy.onboarding.presentation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMealTimeSheet
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.onboarding.domain.CatNameError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.dandi.nyummy.common.presentation.R as CommonR

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun setScreen(state: OnboardingUIState, onIntent: (OnboardingIntent) -> Unit = {}) {
        composeRule.setContent { NyummyTheme { OnboardingScreen(uiState = state, onIntent = onIntent) } }
    }

    @Test
    fun 나레이션은_이름표_없이_보이고_건너뛰기를_누를_수_있다() {
        val intents = mutableListOf<OnboardingIntent>()
        setScreen(OnboardingUIState(isLineRevealed = true)) { intents += it }

        composeRule.onNodeWithText(text(R.string.onboarding_line_alley_1)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_tap_to_continue)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_skip)).performClick()

        assertEquals(listOf<OnboardingIntent>(OnboardingIntent.Skip), intents)
    }

    @Test
    fun 이름을_받기_전_냐미는_물음표_이름표로_말한다() {
        setScreen(OnboardingUIState(sceneIndex = 1, isLineRevealed = true))

        composeRule.onNodeWithText(text(R.string.onboarding_speaker_unknown_cat)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_line_meet_1)).assertIsDisplayed()
    }

    @Test
    fun 선택지는_대사가_끝나면_대화창_아래에_뜨고_누르면_고른_번호를_보낸다() {
        val intents = mutableListOf<OnboardingIntent>()
        val lastLine = OnboardingScript.scenes[1].lines.lastIndex
        setScreen(OnboardingUIState(sceneIndex = 1, lineIndex = lastLine, isLineRevealed = true)) { intents += it }

        composeRule.onNodeWithText(text(R.string.onboarding_choice_snack)).performClick()

        assertEquals(listOf<OnboardingIntent>(OnboardingIntent.SelectChoice(1)), intents)
    }

    @Test
    fun 사용자_대사는_나_이름표로_보인다() {
        setScreen(OnboardingUIState(sceneIndex = 1, choiceIndex = 0, isLineRevealed = true))

        composeRule.onNodeWithText(text(R.string.onboarding_speaker_user)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_line_hand_1)).assertIsDisplayed()
    }

    @Test
    fun 빈_이름_오류는_입력칸_아래에_보인다() {
        setScreen(namingState().copy(catNameError = CatNameError.EMPTY))

        composeRule.onNodeWithText(text(R.string.onboarding_name_error_empty)).assertIsDisplayed()
    }

    @Test
    fun 등록_중에는_버튼_문구가_바뀌고_누를_수_없다() {
        setScreen(namingState().copy(catNameInput = "냐미", isSubmitting = true))

        composeRule.onNodeWithText(text(R.string.onboarding_name_submitting)).assertIsNotEnabled()
    }

    @Test
    fun 이름을_받은_뒤_시작하기를_누르면_시작_인텐트를_보낸다() {
        val intents = mutableListOf<OnboardingIntent>()
        val celebrate = OnboardingScript.scenes.indexOfFirst { it.action == OnboardingSceneAction.Start }
        setScreen(
            OnboardingUIState(
                sceneIndex = celebrate,
                lineIndex = OnboardingScript.scenes[celebrate].lines.lastIndex,
                isLineRevealed = true,
                catName = "냐미",
            ),
        ) { intents += it }

        composeRule.onNodeWithText("냐미").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.onboarding_start)).performClick()

        assertEquals(listOf<OnboardingIntent>(OnboardingIntent.ClickStart), intents)
    }

    @Test
    fun 식사_시각_장면은_기본값과_안_먹어요를_보여주고_줄을_누르면_그_끼니를_보낸다() {
        val intents = mutableListOf<OnboardingIntent>()
        val mealTimes = MealTimesVO(breakfast = MealTimesVO.DefaultBreakfast.copy(isSkipped = true))
        setScreen(mealTimeState().copy(mealTimes = mealTimes)) { intents += it }

        composeRule.onNodeWithText(text(CommonR.string.nyummy_meal_skipped)).assertIsDisplayed()
        composeRule.onNodeWithText(timeText(isPm = true, hour12 = 12)).assertIsDisplayed()
        composeRule.onNodeWithText(timeText(isPm = true, hour12 = 6)).assertIsDisplayed()
        composeRule.onNodeWithText(text(CommonR.string.nyummy_meal_lunch)).performClick()

        assertEquals(listOf<OnboardingIntent>(OnboardingIntent.ClickMealTime(Meal.LUNCH)), intents)
    }

    @Test
    fun 시간_시트에서_확인하면_보이는_시각을_돌려준다() {
        var selected: MealTimeVO? = null
        composeRule.setContent {
            NyummyTheme {
                NyummyMealTimeSheet(
                    meal = Meal.DINNER,
                    initial = MealTimesVO.DefaultDinner,
                    onSelect = { selected = it },
                    onDismissRequest = {},
                )
            }
        }

        composeRule.onNodeWithText(text(CommonR.string.nyummy_meal_sheet_title, text(CommonR.string.nyummy_meal_dinner)))
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(CommonR.string.nyummy_meal_sheet_confirm)).performClick()

        assertEquals(MealTimeVO(hour = 18), selected)
    }

    @Test
    fun 시간_시트에서_안_먹어요를_고르면_시각은_남기고_건너뛴다() {
        var selected: MealTimeVO? = null
        composeRule.setContent {
            NyummyTheme {
                NyummyMealTimeSheet(
                    meal = Meal.BREAKFAST,
                    initial = MealTimesVO.DefaultBreakfast,
                    onSelect = { selected = it },
                    onDismissRequest = {},
                )
            }
        }

        composeRule.onNodeWithText(
            text(CommonR.string.nyummy_meal_sheet_skip, text(CommonR.string.nyummy_meal_breakfast)),
        ).performClick()

        assertEquals(MealTimesVO.DefaultBreakfast.copy(isSkipped = true), selected)
    }

    private fun namingState() = OnboardingUIState(
        sceneIndex = OnboardingScript.namingSceneIndex,
        lineIndex = OnboardingScript.scenes[OnboardingScript.namingSceneIndex].lines.lastIndex,
        isLineRevealed = true,
    )

    private fun mealTimeState() = OnboardingUIState(
        sceneIndex = OnboardingScript.scenes.lastIndex,
        isLineRevealed = true,
        catName = "냐미",
    )

    private fun timeText(isPm: Boolean, hour12: Int): String = text(
        CommonR.string.nyummy_meal_time_format,
        text(if (isPm) CommonR.string.nyummy_meal_pm else CommonR.string.nyummy_meal_am),
        hour12,
    )
}
