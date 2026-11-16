package com.dandi.nyummy.history.presentation.detail.v2

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.image.NyummyFoodIconCategory
import com.dandi.nyummy.history.entity.MealFoodVO
import com.dandi.nyummy.history.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 음식 고치기 시트. 실제 화면처럼 입력을 상태에 반영하며 확인합니다. */
@RunWith(AndroidJUnit4::class)
class MealFoodEditSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private val beef = MealFoodVO(3, "불고기", 1.5, true, 150, 6, 9, 9, foodIconId = "14")

    private var sheet by mutableStateOf(MealFoodSheetUiState.of(beef, canDelete = true))

    /** ViewModel의 reduce가 하는 대로 시트 상태를 바꾼다. */
    private fun showSheet(initial: MealFoodSheetUiState = sheet) {
        sheet = initial
        composeRule.setContent {
            NyummyTheme {
                Column {
                    MealFoodEditSheetContent(sheet = sheet, isSaving = false) { intent ->
                        sheet = when (intent) {
                            is MealDetailV2Intent.SelectIconCategory -> sheet.copy(iconCategory = intent.category)
                            is MealDetailV2Intent.SelectIcon -> sheet.copy(iconId = intent.iconId)
                            is MealDetailV2Intent.StepPortion -> sheet.withPortionStep(intent.increase)
                            is MealDetailV2Intent.SetManualNutrition -> sheet.withManualNutrition(intent.enabled)
                            is MealDetailV2Intent.ChangeNutrient -> sheet.withNutrient(intent.nutrient, intent.text)
                            else -> sheet
                        }
                    }
                }
            }
        }
    }

    @Test
    fun 직접_설정하기가_꺼져_있으면_영양_칸에_입력할_수_없고_켜면_입력할_수_있다() {
        showSheet()
        val calorie = nutrientFieldTagOf(MealFoodNutrient.CALORIE)

        composeRule.onNodeWithTag(calorie).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.history_food_sheet_manual_off_hint)).assertIsDisplayed()

        composeRule.onNodeWithTag(MealFoodSheetManualTag).performClick()

        composeRule.onNodeWithTag(calorie).assertIsEnabled()
        composeRule.onNodeWithText(text(R.string.history_food_sheet_manual_on_hint)).assertIsDisplayed()
    }

    @Test
    fun 반_인분에서는_더_줄일_수_없다() {
        showSheet(MealFoodSheetUiState.of(beef.copy(portion = 0.5), canDelete = true))

        composeRule.onNodeWithContentDescription(text(R.string.history_food_sheet_portion_decrease))
            .performScrollTo().performClick()

        assertEquals(0.5, sheet.portion, 0.0)
        composeRule.onNodeWithTag(MealFoodSheetPortionTag).assertTextContains(text(R.string.history_detail_v2_portion, "0.5"))
    }

    @Test
    fun 다섯_인분에서는_더_늘릴_수_없고_줄이면_반_인분씩_준다() {
        showSheet(MealFoodSheetUiState.of(beef.copy(portion = 5.0), canDelete = true))

        composeRule.onNodeWithContentDescription(text(R.string.history_food_sheet_portion_increase))
            .performScrollTo().performClick()
        assertEquals(5.0, sheet.portion, 0.0)

        composeRule.onNodeWithContentDescription(text(R.string.history_food_sheet_portion_decrease)).performClick()
        composeRule.onNodeWithTag(MealFoodSheetPortionTag).assertTextContains(text(R.string.history_detail_v2_portion, "4.5"))
    }

    @Test
    fun 음식이_한_줄_남으면_지우기가_없다() {
        showSheet(MealFoodSheetUiState.of(beef, canDelete = false))

        composeRule.onAllNodesWithText(text(R.string.history_food_sheet_delete)).assertCountEquals(0)
    }

    @Test
    fun 처음에는_전체_분류라_1번_아이콘부터_보이고_분류를_바꾸면_그_분류_아이콘만_보인다() {
        showSheet()
        assertNull(sheet.iconCategory)
        val firstOfAll = text(R.string.history_food_sheet_icon_description, "14")
        composeRule.onNodeWithContentDescription(firstOfAll).assertIsDisplayed()

        composeRule.onNodeWithText(text(R.string.history_food_sheet_category_fruit)).performClick()

        assertEquals(NyummyFoodIconCategory.FRUIT, sheet.iconCategory)
        val fruit = NyummyFoodIconCategory.iconIdsOf(NyummyFoodIconCategory.FRUIT)
        composeRule.onNodeWithContentDescription(text(R.string.history_food_sheet_icon_description, fruit.first())).assertIsDisplayed()
        // 과일이 아닌 아이콘은 격자에 없다.
        val notFruit = NyummyFoodIconCategory.allIconIds.first { it !in fruit }
        composeRule.onAllNodesWithContentDescription(text(R.string.history_food_sheet_icon_description, notFruit)).assertCountEquals(0)
    }

    @Test
    fun 아이콘을_누르면_그_아이콘이_골라진다() {
        showSheet()
        composeRule.onNodeWithText(text(R.string.history_food_sheet_category_fruit)).performClick()
        val target = NyummyFoodIconCategory.iconIdsOf(NyummyFoodIconCategory.FRUIT).first()

        composeRule.onNodeWithContentDescription(text(R.string.history_food_sheet_icon_description, target)).performClick()

        assertEquals(target, sheet.iconId)
    }
}
