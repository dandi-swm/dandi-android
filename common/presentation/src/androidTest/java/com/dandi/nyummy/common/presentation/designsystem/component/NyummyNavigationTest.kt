package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummyNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun 상단_바는_제목을_헤딩으로_알리고_뒤로가기를_호출한다() {
        var back = 0
        composeRule.setContent {
            NyummyTheme { NyummyTopBar(title = "히스토리", onBackClick = { back++ }) }
        }

        composeRule.onNodeWithText("히스토리").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithContentDescription("뒤로 가기").performClick()

        assertEquals(1, back)
    }

    @Test
    fun 뒤로가기가_없으면_버튼을_그리지_않는다() {
        composeRule.setContent {
            NyummyTheme { NyummyTopBar(title = "설정") }
        }

        composeRule.onNodeWithContentDescription("뒤로 가기").assertDoesNotExist()
    }

    @Test
    fun 하단_내비는_5탭을_순서대로_보여준다() {
        composeRule.setContent {
            NyummyTheme { NyummyBottomNav(items = NyummyMainTabs, selectedIndex = 0, onSelect = {}) }
        }

        listOf("홈", "기록", "퀘스트", "업적", "상점").forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
        composeRule.onAllNodesWithText("홈").assertCountEquals(1)
    }

    @Test
    fun 하단_내비_탭을_누르면_선택이_바뀐다() {
        var selected = 0
        composeRule.setContent {
            NyummyTheme {
                var index by remember { mutableIntStateOf(0) }
                NyummyBottomNav(items = NyummyMainTabs, selectedIndex = index, onSelect = { index = it; selected = it })
            }
        }

        composeRule.onNodeWithText("홈").assert(hasRole(Role.Tab)).assertIsSelected()
        composeRule.onNodeWithText("상점").assertIsNotSelected().performClick()

        composeRule.onNodeWithText("상점").assertIsSelected()
        composeRule.onNodeWithText("홈").assertIsNotSelected()
        assertEquals(4, selected)
    }

    @Test
    fun 카테고리_탭은_하나만_선택된다() {
        composeRule.setContent {
            NyummyTheme {
                var index by remember { mutableIntStateOf(0) }
                NyummyTabs(tabs = persistentListOf("전체", "모자", "옷"), selectedIndex = index, onSelect = { index = it })
            }
        }

        composeRule.onNodeWithText("전체").assertIsSelected()
        composeRule.onNodeWithText("옷").assert(hasRole(Role.Tab)).performClick()
        composeRule.onNodeWithText("옷").assertIsSelected()
        composeRule.onNodeWithText("전체").assertIsNotSelected()
    }

    @Test
    fun 단계_표시는_현재_단계와_전체_단계를_보여준다() {
        composeRule.setContent {
            NyummyTheme { NyummyStepIndicator(currentStep = 2, totalSteps = 3) }
        }

        composeRule.onNodeWithText("2 / 3").assertIsDisplayed()
    }
}
