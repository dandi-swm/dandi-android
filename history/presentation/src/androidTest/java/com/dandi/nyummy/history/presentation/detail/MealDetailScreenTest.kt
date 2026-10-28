package com.dandi.nyummy.history.presentation.detail

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private val meal = MealHistoryVO(
        id = "1",
        name = "비빔밥",
        recordedAt = "08:10",
        orderIndex = 1,
        date = HistoryDateVO(2026, 10, 8),
        catComment = "채소가 듬뿍이라 냐미도 기분 좋아!",
    )

    private fun setScreen(state: MealDetailUIState, onIntent: (MealDetailIntent) -> Unit = {}) {
        composeRule.setContent { NyummyTheme { MealDetailScreen(uiState = state, onIntent = onIntent) } }
    }

    @Test
    fun 받은_식사의_이름과_냐미_한마디를_보여_주고_수정과_삭제를_보낸다() {
        val intents = mutableListOf<MealDetailIntent>()
        setScreen(MealDetailUIState(meal = meal, isLoading = false)) { intents += it }

        composeRule.onNodeWithText("비빔밥").assertIsDisplayed()
        composeRule.onNodeWithText("채소가 듬뿍이라 냐미도 기분 좋아!").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_detail_edit_name)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.history_detail_delete)).performScrollTo().performClick()

        assertEquals(listOf(MealDetailIntent.ClickEditName, MealDetailIntent.ClickDelete), intents)
    }

    @Test
    fun 받지_못하면_안내하고_다시_불러오기를_보낸다() {
        val intents = mutableListOf<MealDetailIntent>()
        setScreen(MealDetailUIState(isLoading = false, isLoadFailed = true)) { intents += it }

        composeRule.onNodeWithText(text(R.string.history_load_failed_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_load_retry)).performClick()

        assertEquals(listOf<MealDetailIntent>(MealDetailIntent.RetryLoad), intents)
    }

    @Test
    fun 뒤로를_누르면_뒤로_가기를_보낸다() {
        val intents = mutableListOf<MealDetailIntent>()
        setScreen(MealDetailUIState(meal = meal, isLoading = false)) { intents += it }

        composeRule.onNodeWithContentDescription(
            context.getString(com.dandi.nyummy.common.presentation.R.string.nyummy_back_description),
        ).performClick()

        assertEquals(listOf<MealDetailIntent>(MealDetailIntent.ClickBack), intents)
    }

    @Test
    fun 이름을_비우면_안내하고_저장할_수_없다() {
        setScreen(MealDetailUIState(meal = meal, isLoading = false, mode = MealDetailMode.EditingName, nameDraft = " "))

        composeRule.onNodeWithText(text(R.string.history_edit_dialog_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_edit_dialog_confirm)).assertIsNotEnabled()
    }

    @Test
    fun 삭제_확인은_식사_이름을_묻고_확정을_보낸다() {
        val intents = mutableListOf<MealDetailIntent>()
        setScreen(MealDetailUIState(meal = meal, isLoading = false, mode = MealDetailMode.ConfirmingDelete)) { intents += it }

        composeRule.onNodeWithText(text(R.string.history_delete_dialog_title, "비빔밥")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_delete_dialog_confirm)).performClick()

        assertEquals(listOf<MealDetailIntent>(MealDetailIntent.ConfirmDelete), intents)
    }
}
