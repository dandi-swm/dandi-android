package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummyActionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun 버튼을_누르면_onClick이_한_번_호출된다() {
        var clicks = 0
        composeRule.setContent {
            NyummyTheme { NyummyButton(text = "밥 주기", onClick = { clicks++ }) }
        }

        composeRule.onNodeWithText("밥 주기").assert(hasRole(Role.Button)).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun 비활성_버튼은_눌러도_onClick이_호출되지_않는다() {
        var clicks = 0
        composeRule.setContent {
            NyummyTheme { NyummyButton(text = "밥 주기", onClick = { clicks++ }, enabled = false) }
        }

        composeRule.onNodeWithText("밥 주기").assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun 버튼_크기별_높이는_Figma와_같다() {
        composeRule.setContent {
            NyummyTheme {
                androidx.compose.foundation.layout.Column {
                    NyummyButton(text = "L 버튼", onClick = {}, size = NyummyButtonSize.L)
                    NyummyButton(text = "M 버튼", onClick = {}, size = NyummyButtonSize.M)
                    NyummyButton(text = "S 버튼", onClick = {}, size = NyummyButtonSize.S)
                }
            }
        }

        composeRule.onNodeWithText("L 버튼").assertHeightIsEqualTo(56.dp)
        composeRule.onNodeWithText("M 버튼").assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithText("S 버튼").assertHeightIsEqualTo(36.dp)
    }

    @Test
    fun 작은_버튼도_터치_영역은_48dp다() {
        composeRule.setContent {
            NyummyTheme { NyummyButton(text = "받기", onClick = {}, size = NyummyButtonSize.S) }
        }

        composeRule.onNodeWithText("받기").assertTouchHeightIsEqualTo(48.dp)
    }

    @Test
    fun 텍스트_버튼은_누르면_호출되고_비활성이면_호출되지_않는다() {
        var clicks = 0
        composeRule.setContent {
            NyummyTheme {
                androidx.compose.foundation.layout.Column {
                    NyummyTextButton(text = "더보기", onClick = { clicks++ })
                    NyummyTextButton(text = "접기", onClick = { clicks++ }, enabled = false)
                }
            }
        }

        composeRule.onNodeWithText("더보기").assertIsEnabled().performClick()
        composeRule.onNodeWithText("접기").assertIsNotEnabled().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun 아이콘_버튼은_설명이_있고_48dp_원형_터치_영역이다() {
        var clicks = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyIconButton(
                    icon = R.drawable.nyummy_ic_bell,
                    contentDescription = "알림",
                    onClick = { clicks++ },
                    style = NyummyIconButtonStyle.Filled,
                )
            }
        }

        composeRule.onNodeWithContentDescription("알림")
            .assert(hasRole(Role.Button))
            .assertTouchWidthIsEqualTo(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun 칩은_선택_상태를_접근성에_알린다() {
        composeRule.setContent {
            NyummyTheme {
                var selected by remember { mutableStateOf("아침") }
                androidx.compose.foundation.layout.Row {
                    listOf("아침", "점심").forEach { meal ->
                        NyummyChip(text = meal, selected = meal == selected, onClick = { selected = meal })
                    }
                }
            }
        }

        composeRule.onNodeWithText("아침").assertIsSelected()
        composeRule.onNodeWithText("점심").assertIsNotSelected().performClick()
        composeRule.onNodeWithText("점심").assertIsSelected()
        composeRule.onNodeWithText("아침").assertIsNotSelected()
    }

    @Test
    fun 하단_CTA는_보조와_주_버튼을_각각_호출한다() {
        var primary = 0
        var secondary = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyBottomCta(
                    primaryText = "확인",
                    onPrimaryClick = { primary++ },
                    secondaryText = "닫기",
                    onSecondaryClick = { secondary++ },
                )
            }
        }

        composeRule.onNodeWithText("확인").performClick()
        composeRule.onNodeWithText("닫기").performClick()

        assertEquals(1, primary)
        assertEquals(1, secondary)
    }

    @Test
    fun 하단_CTA_주_버튼을_비활성화할_수_있다() {
        composeRule.setContent {
            NyummyTheme { NyummyBottomCta(primaryText = "확인", onPrimaryClick = {}, primaryEnabled = false) }
        }

        composeRule.onNodeWithText("확인").assertIsNotEnabled()
    }
}
