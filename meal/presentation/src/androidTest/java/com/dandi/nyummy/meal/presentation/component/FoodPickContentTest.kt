package com.dandi.nyummy.meal.presentation.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.meal.entity.FoundFoodVO
import com.dandi.nyummy.meal.presentation.R
import com.dandi.nyummy.meal.presentation.model.FoodPickDraft
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** 찾은 음식 고르기 화면이 고른 개수에 따라 버튼과 추가 줄을 바꾸는지 확인합니다. */
class FoodPickContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val foods = listOf("밥", "된장찌개", "불고기", "시금치나물", "배추김치").mapIndexed { index, name ->
        FoundFoodVO(mealFoodId = index + 1L, name = name)
    }

    private fun text(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    /** 화면이 보낸 동작을 그대로 반영해 실제 화면처럼 움직이게 한다. */
    private fun setContent(
        initial: FoodPickDraft,
        isSubmitting: Boolean = false,
        singleClick: Boolean = true,
        onSubmit: (FoodPickDraft) -> Unit = {},
    ) {
        composeRule.setContent {
            NyummyTheme(singleClick = singleClick) {
                var draft by remember { mutableStateOf(initial) }
                FoodPickContent(
                    draft = draft,
                    isSubmitting = isSubmitting,
                    onClose = {},
                    onToggle = { draft = draft.toggle(it) },
                    onOpenNameInput = { draft = draft.openNameInput() },
                    onNameInputChange = { draft = draft.changeNameInput(it) },
                    onConfirmNameInput = { draft = draft.confirmNameInput() },
                    onSubmit = { onSubmit(draft) },
                )
            }
        }
    }

    @Test
    fun 버튼_문구는_체크한_개수를_따른다() {
        setContent(FoodPickDraft.of(foods))

        composeRule.onNodeWithTag(FoodPickSubmitTag).assertTextEquals(text(R.string.meal_food_pick_submit, 5))
        composeRule.onNodeWithText("배추김치").performClick()
        composeRule.onNodeWithTag(FoodPickSubmitTag).assertTextEquals(text(R.string.meal_food_pick_submit, 4))
    }

    @Test
    fun 하나도_고르지_않으면_가져갈_수_없다() {
        setContent(FoodPickDraft.of(foods.take(1)))

        composeRule.onNodeWithTag(FoodPickSubmitTag).assertIsEnabled()
        composeRule.onNodeWithText("밥").performClick()
        composeRule.onNodeWithTag(FoodPickSubmitTag).assertIsNotEnabled()
    }

    @Test
    fun 이름을_적어_추가하면_줄이_생기고_개수가_는다() {
        var submitted: FoodPickDraft? = null
        setContent(FoodPickDraft.of(foods), singleClick = false) { submitted = it }

        composeRule.onNodeWithText(text(R.string.meal_food_pick_add)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.meal_food_pick_add_confirm)).assertIsNotEnabled()
        composeRule.onNodeWithTag(FoodPickNameInputTag).performTextInput("김밥")
        composeRule.onNodeWithText(text(R.string.meal_food_pick_add_confirm)).performClick()

        composeRule.onNodeWithText("김밥").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(FoodPickSubmitTag).assertTextEquals(text(R.string.meal_food_pick_submit, 6))
        composeRule.onNodeWithTag(FoodPickSubmitTag).performClick()
        assertEquals(6, submitted?.toFoods()?.size)
    }

    @Test
    fun 열_줄이면_음식_추가가_없다() {
        setContent(FoodPickDraft.of(foods + foods.map { it.copy(mealFoodId = it.mealFoodId + 10, name = it.name + "2") }))

        composeRule.onNodeWithText(text(R.string.meal_food_pick_add)).assertDoesNotExist()
    }

    @Test
    fun 보내는_중에는_버튼을_누를_수_없다() {
        setContent(FoodPickDraft.of(foods), isSubmitting = true)

        composeRule.onNodeWithTag(FoodPickSubmitTag).assertIsNotEnabled()
    }
}
