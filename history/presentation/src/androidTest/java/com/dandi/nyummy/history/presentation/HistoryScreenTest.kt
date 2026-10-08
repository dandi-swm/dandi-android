package com.dandi.nyummy.history.presentation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.history.entity.HistoryCalendarDayVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.model.HistoryMonth
import com.dandi.nyummy.history.presentation.model.buildCalendarDayUiModels
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private val today = HistoryDateVO(2026, 10, 8)
    private val october = HistoryMonth(2026, 10)

    private fun state(
        selectedDate: HistoryDateVO = today,
        displayed: HistoryMonth = october,
        meals: List<MealHistoryVO> = emptyList(),
        isLoading: Boolean = false,
        isLoadFailed: Boolean = false,
    ): HistoryUIState {
        // 아이콘 식별자를 비워 두면 CDN 대신 앱에 든 밥 아이콘을 그린다(테스트 앱은 인터넷 권한이 없다).
        val records = mapOf(
            HistoryDateVO(2026, 10, 7) to HistoryCalendarDayVO(date = HistoryDateVO(2026, 10, 7), foodIconIds = listOf(""), mealCount = 1),
        )
        return HistoryUIState(
            displayedYear = displayed.year,
            displayedMonth = displayed.month,
            today = today,
            selectedDate = selectedDate,
            calendarMonths = persistentMapOf(
                october to buildCalendarDayUiModels(2026, 10, records),
                HistoryMonth(2026, 8) to buildCalendarDayUiModels(2026, 8, emptyMap()),
            ),
            selectedDayMeals = persistentListOf<MealHistoryVO>().addAll(meals),
            isLoading = isLoading,
            isLoadFailed = isLoadFailed,
        )
    }

    private fun setScreen(state: HistoryUIState, onIntent: (HistoryIntent) -> Unit = {}) {
        composeRule.setContent { NyummyTheme { HistoryScreen(uiState = state, onIntent = onIntent) } }
    }

    private fun calendar() = composeRule.onNode(hasContentDescription(text(R.string.history_calendar_description, "2026년 10월")))

    @Test
    fun 캘린더를_오른쪽으로_넘기면_지난달을_알린다() {
        val intents = mutableListOf<HistoryIntent>()
        setScreen(state()) { intents += it }

        calendar().performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertTrue(HistoryIntent.ChangeMonth(HistoryMonth(2026, 9)) in intents)
    }

    @Test
    fun 이번_달에서_왼쪽으로_넘겨도_다음_달로_가지_않는다() {
        val intents = mutableListOf<HistoryIntent>()
        setScreen(state()) { intents += it }

        calendar().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertTrue(intents.none { it is HistoryIntent.ChangeMonth && it.month > october })
        composeRule.onNodeWithText("2026년 10월").assertIsDisplayed()
    }

    @Test
    fun 오늘을_보고_있으면_오늘로_이동_버튼이_없다() {
        setScreen(state())

        composeRule.onNodeWithText(text(R.string.history_go_today)).assertDoesNotExist()
    }

    @Test
    fun 오늘이_아닌_날을_고르면_오늘로_이동_버튼이_나오고_누르면_오늘로_간다() {
        val intents = mutableListOf<HistoryIntent>()
        setScreen(state(selectedDate = HistoryDateVO(2026, 10, 7))) { intents += it }

        composeRule.onNodeWithText(text(R.string.history_go_today)).performClick()

        assertEquals(listOf<HistoryIntent>(HistoryIntent.ClickToday), intents)
    }

    @Test
    fun 미래_날짜는_누를_수_없고_지난_날짜는_고를_수_있다() {
        val intents = mutableListOf<HistoryIntent>()
        setScreen(state()) { intents += it }

        composeRule.onNodeWithContentDescription("10월 20일").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("9월 30일").performClick()

        assertEquals(listOf<HistoryIntent>(HistoryIntent.SelectDate(HistoryDateVO(2026, 9, 30))), intents)
    }

    @Test
    fun 그_달_날짜가_있는_주만_그린다() {
        // 2026년 10월은 31일(토)로 끝나 11월만 있는 줄이 없다.
        setScreen(state())
        composeRule.onNodeWithContentDescription("10월 31일").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("11월 1일").assertDoesNotExist()
    }

    @Test
    fun 여섯_주가_필요한_달은_마지막_줄에_다음_달_날짜를_흐리게_둔다() {
        // 2026년 8월은 토요일에 시작해 6주가 필요하다.
        setScreen(state(selectedDate = HistoryDateVO(2026, 8, 8), displayed = HistoryMonth(2026, 8)))
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("8월 31일").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("9월 5일").assertIsDisplayed()
    }

    @Test
    fun 기록_있는_날은_기록_있음으로_읽어_준다() {
        setScreen(state())

        composeRule.onNodeWithContentDescription("10월 7일, ${text(R.string.history_calendar_day_recorded)}").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("10월 8일, ${text(R.string.history_calendar_day_today)}").assertIsDisplayed()
    }

    @Test
    fun 오늘_기록이_없으면_지금_기록하기로_보낸다() {
        val intents = mutableListOf<HistoryIntent>()
        setScreen(state()) { intents += it }

        composeRule.onNodeWithText(text(R.string.history_empty_today_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_empty_today_action)).performScrollTo().performClick()

        assertEquals(listOf<HistoryIntent>(HistoryIntent.ClickRecordMeal), intents)
    }

    @Test
    fun 지난_날에_기록이_없으면_쉬어_간_날로_보여_주고_기록을_권하지_않는다() {
        setScreen(state(selectedDate = HistoryDateVO(2026, 10, 6)))

        composeRule.onNodeWithText(text(R.string.history_empty_past_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_empty_today_action)).assertDoesNotExist()
    }

    @Test
    fun 불러오지_못하면_오류를_보여_주고_다시_불러오기를_보낸다() {
        val intents = mutableListOf<HistoryIntent>()
        setScreen(state(isLoadFailed = true)) { intents += it }

        composeRule.onNodeWithText(text(R.string.history_load_failed_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_load_retry)).performScrollTo().performClick()

        assertEquals(listOf<HistoryIntent>(HistoryIntent.RetryLoad), intents)
    }

    @Test
    fun 식사_줄은_완료_분석_중_분석_실패를_다르게_보여_준다() {
        val intents = mutableListOf<HistoryIntent>()
        val meals = listOf(
            MealHistoryVO(id = "1", name = "비빔밥", recordedAt = "08:10", calorieKcal = 612, orderIndex = 1),
            MealHistoryVO(id = "2", recordedAt = "19:05", status = MealAnalysisStatus.ANALYZING),
            MealHistoryVO(id = "3", recordedAt = "21:20", status = MealAnalysisStatus.FAILED),
        )
        setScreen(state(meals = meals)) { intents += it }

        composeRule.onNodeWithText(text(R.string.history_day_summary, 1)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("첫 끼 오전 8:10").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_analysis_pending_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_analysis_retry)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.history_analysis_delete)).performScrollTo().performClick()
        composeRule.onNodeWithText("비빔밥").performScrollTo().performClick()

        assertEquals(
            listOf(
                HistoryIntent.ClickRetryAnalysis("3"),
                HistoryIntent.ClickDeleteFailedMeal("3"),
                HistoryIntent.ClickMeal("1"),
            ),
            intents,
        )
    }

    @Test
    fun 이름을_비우면_안내하고_저장할_수_없다() {
        val meal = MealHistoryVO(id = "1", name = "비빔밥", recordedAt = "08:10", orderIndex = 1)
        setScreen(
            state(meals = listOf(meal)).copy(
                mealDetail = HistoryMealDetailUiState(meal = meal, mode = HistoryMealDetailMode.EditingName, nameDraft = " "),
            ),
        )

        composeRule.onNodeWithText(text(R.string.history_edit_dialog_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_edit_dialog_confirm)).assertIsNotEnabled()
    }

    @Test
    fun 삭제_요청을_보내는_중에는_닫기와_삭제를_누를_수_없다() {
        val meal = MealHistoryVO(id = "1", name = "비빔밥", recordedAt = "08:10", orderIndex = 1)
        setScreen(
            state(meals = listOf(meal)).copy(
                mealDetail = HistoryMealDetailUiState(
                    meal = meal,
                    mode = HistoryMealDetailMode.ConfirmingDelete,
                    isActionInFlight = true,
                ),
            ),
        )

        composeRule.onNodeWithText(context.getString(com.dandi.nyummy.common.presentation.R.string.nyummy_dialog_dismiss)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.history_delete_dialog_confirm)).assertIsNotEnabled()
    }

    @Test
    fun 삭제_확인은_식사_이름을_묻고_확정을_보낸다() {
        val intents = mutableListOf<HistoryIntent>()
        val meal = MealHistoryVO(id = "1", name = "비빔밥", recordedAt = "08:10", orderIndex = 1)
        setScreen(
            state(meals = listOf(meal)).copy(
                mealDetail = HistoryMealDetailUiState(meal = meal, mode = HistoryMealDetailMode.ConfirmingDelete),
            ),
        ) { intents += it }

        composeRule.onNodeWithText(text(R.string.history_delete_dialog_title, "비빔밥")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_delete_dialog_confirm)).performClick()

        assertEquals(listOf<HistoryIntent>(HistoryIntent.ConfirmDeleteMeal), intents)
    }
}
