package com.dandi.nyummy.history.presentation.detail.v2

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealFoodVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealDetailV2ScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private val meal = MealHistoryVO(
        id = "1",
        name = "한정식",
        recordedAt = "08:10",
        calorieKcal = 612,
        carbohydrateGram = 82,
        proteinGram = 24,
        fatGram = 18,
        orderIndex = 1,
        date = HistoryDateVO(2026, 11, 8),
        catComment = "골고루 먹었네!",
    )

    private val foods = persistentListOf(
        MealFoodVO(1, "밥", 1.0, true, 300, 66, 6, 1, foodIconId = "5"),
        MealFoodVO(2, "된장찌개", 1.0, true, 110, 8, 7, 5, foodIconId = "15"),
        MealFoodVO(3, "불고기", 1.5, true, 150, 6, 9, 9, foodIconId = "14"),
    )

    private fun setScreen(state: MealDetailV2UIState, onIntent: (MealDetailV2Intent) -> Unit = {}) {
        composeRule.setContent { NyummyTheme { MealDetailV2Screen(uiState = state, onIntent = onIntent) } }
    }

    @Test
    fun 음식_줄을_모두_보여_주고_대표는_첫_줄에만_붙는다() {
        setScreen(MealDetailV2UIState(meal = meal, foods = foods, isLoading = false))

        foods.forEach { composeRule.onNodeWithText(it.name).performScrollTo().assertIsDisplayed() }
        composeRule.onAllNodesWithText(text(R.string.history_detail_v2_main_badge)).assertCountEquals(1)
        composeRule.onNodeWithText(text(R.string.history_detail_v2_portion, "1.5")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun 식사_합계_영양을_보여_준다() {
        setScreen(MealDetailV2UIState(meal = meal, foods = foods, isLoading = false))

        composeRule.onNodeWithText(text(R.string.history_nutrition_kcal, "612")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun 연필과_음식_추가와_휴지통이_입력을_보낸다() {
        val intents = mutableListOf<MealDetailV2Intent>()
        setScreen(MealDetailV2UIState(meal = meal, foods = foods, isLoading = false)) { intents += it }

        composeRule.onNodeWithContentDescription(text(R.string.history_detail_v2_edit_food_description, "불고기"))
            .performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.history_detail_v2_add_food)).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(text(R.string.history_detail_v2_edit_name_description)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.history_detail_delete)).performClick()

        assertEquals(
            listOf(
                MealDetailV2Intent.ClickEditFood(3),
                MealDetailV2Intent.ClickAddFood,
                MealDetailV2Intent.ClickEditName,
                MealDetailV2Intent.ClickDelete,
            ),
            intents,
        )
    }

    @Test
    fun 계산_중이면_안내를_보여_주고_연필이_눌리지_않는다() {
        val intents = mutableListOf<MealDetailV2Intent>()
        setScreen(
            MealDetailV2UIState(
                meal = meal,
                foods = persistentListOf(MealFoodVO(1, "김치찌개", 1.0, hasNutrition = false)),
                isLoading = false,
                isCalculating = true,
            ),
        ) { intents += it }

        composeRule.onNodeWithTag(MealDetailV2CalculatingTag).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_detail_v2_food_pending)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.history_detail_v2_edit_food_description, "김치찌개"))
            .performScrollTo().performClick()

        assertEquals(emptyList<MealDetailV2Intent>(), intents)
    }

    @Test
    fun 계산_중_안내는_마지막_음식_줄과_음식_추가_사이에_뜬다() {
        setScreen(MealDetailV2UIState(meal = meal, foods = foods, isLoading = false, isCalculating = true))

        val notice = composeRule.onNodeWithTag(MealDetailV2CalculatingTag).performScrollTo().getUnclippedBoundsInRoot()
        val lastFood = composeRule.onNodeWithText("불고기").getUnclippedBoundsInRoot()
        val add = composeRule.onNodeWithText(text(R.string.history_detail_v2_add_food)).getUnclippedBoundsInRoot()

        assertTrue(lastFood.bottom <= notice.top)
        assertTrue(notice.bottom <= add.top)
    }

    @Test
    fun 음식이_한_줄이면_순서_손잡이와_안내가_없다() {
        setScreen(MealDetailV2UIState(meal = meal, foods = foods.take(1).toImmutableList(), isLoading = false))

        composeRule.onAllNodesWithText(text(R.string.history_detail_v2_foods_hint)).assertCountEquals(0)
    }

    @Test
    fun 음식이_열_줄이면_음식_추가가_없다() {
        val ten = List(MealFoodVO.MAX_FOODS) { MealFoodVO(it + 1L, "음식${it + 1}", 1.0, true, 100, 1, 1, 1) }
        setScreen(MealDetailV2UIState(meal = meal, foods = ten.toImmutableList(), isLoading = false))

        composeRule.onAllNodesWithText(text(R.string.history_detail_v2_add_food)).assertCountEquals(0)
    }
}
