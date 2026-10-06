package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummyInputsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun 텍스트_필드는_라벨을_보여주고_입력을_전달한다() {
        composeRule.setContent {
            NyummyTheme {
                var value by remember { mutableStateOf("") }
                NyummyTextField(value = value, onValueChange = { value = it }, label = "이메일", placeholder = "example@nyummy.com")
            }
        }

        composeRule.onNodeWithText("이메일").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performTextInput("cat@nyummy.com")
        composeRule.onNodeWithText("cat@nyummy.com").assertIsDisplayed()
    }

    @Test
    fun 텍스트_필드_오류_문구는_도움말_대신_보인다() {
        composeRule.setContent {
            NyummyTheme {
                NyummyTextField(
                    value = "cat@",
                    onValueChange = {},
                    label = "이메일",
                    helperText = "도움말",
                    errorMessage = "이메일 형식을 확인해 주세요",
                )
            }
        }

        composeRule.onNodeWithText("이메일 형식을 확인해 주세요").assertIsDisplayed()
        composeRule.onNodeWithText("도움말").assertDoesNotExist()
    }

    @Test
    fun 비활성_텍스트_필드는_입력을_받지_않는다() {
        composeRule.setContent {
            NyummyTheme { NyummyTextField(value = "", onValueChange = {}, label = "이메일", enabled = false) }
        }

        // 비활성 입력칸은 SetText 액션이 없으므로 EditableText 속성으로 찾는다.
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsEditable, false))
    }

    @Test
    fun 비밀번호_보기_토글은_설명이_바뀐다() {
        composeRule.setContent {
            NyummyTheme {
                var visible by remember { mutableStateOf(false) }
                NyummyPasswordVisibilityToggle(visible = visible, onToggle = { visible = !visible })
            }
        }

        composeRule.onNodeWithContentDescription("비밀번호 보기").assert(hasRole(Role.Button)).performClick()
        composeRule.onNodeWithContentDescription("비밀번호 숨기기").assertIsDisplayed()
    }

    @Test
    fun 텍스트_영역은_최대_글자_수를_넘지_않고_글자_수를_보여준다() {
        var latest = ""
        composeRule.setContent {
            NyummyTheme {
                var value by remember { mutableStateOf("") }
                NyummyTextArea(value = value, onValueChange = { value = it; latest = it }, label = "문의 내용", maxLength = 5)
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("냐미랑밥먹자")

        assertEquals("냐미랑밥먹", latest)
        composeRule.onNodeWithText("5/5").assertIsDisplayed()
    }

    @Test
    fun 텍스트_영역은_천_단위_쉼표로_최대_글자_수를_표시한다() {
        composeRule.setContent {
            NyummyTheme { NyummyTextArea(value = "", onValueChange = {}, label = "문의 내용") }
        }

        composeRule.onNodeWithText("0/1,000").assertIsDisplayed()
    }

    @Test
    fun 인증_코드는_숫자만_받고_자릿수를_넘지_않는다() {
        var latest = ""
        composeRule.setContent {
            NyummyTheme {
                var value by remember { mutableStateOf("") }
                NyummyCodeInput(value = value, onValueChange = { value = it; latest = it }, length = 6)
            }
        }

        composeRule.onNodeWithContentDescription("인증 코드 입력").performTextReplacement("12a34567")

        assertEquals("123456", latest)
    }

    @Test
    fun 세그먼트는_하나만_선택되고_누르면_바뀐다() {
        composeRule.setContent {
            NyummyTheme {
                var selected by remember { mutableIntStateOf(0) }
                NyummySegmentedControl(
                    options = persistentListOf("남성", "여성"),
                    selectedIndex = selected,
                    onSelect = { selected = it },
                )
            }
        }

        composeRule.onNodeWithText("남성").assertIsSelected()
        composeRule.onNodeWithText("여성").assertIsNotSelected().assert(hasRole(Role.RadioButton)).performClick()
        composeRule.onNodeWithText("여성").assertIsSelected()
        composeRule.onNodeWithText("남성").assertIsNotSelected()
    }

    @Test
    fun 휠_피커는_선택_값을_상태로_알리고_스크롤하면_값이_바뀐다() {
        var selected = 32
        composeRule.setContent {
            NyummyTheme {
                var index by remember { mutableIntStateOf(32) }
                NyummyWheelPicker(
                    items = (140..200).map(Int::toString).toImmutableList(),
                    selectedIndex = index,
                    onSelectedIndexChange = { index = it; selected = it },
                    unit = "cm",
                    contentDescription = "키",
                    modifier = Modifier.width(160.dp).testTag("wheel"),
                )
            }
        }

        composeRule.onNodeWithContentDescription("키")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "172cm"))
        composeRule.onNodeWithTag("wheel").performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assertNotEquals(32, selected)
    }

    @Test
    fun 체크박스는_토글되고_역할이_체크박스다() {
        composeRule.setContent {
            NyummyTheme {
                var checked by remember { mutableStateOf(false) }
                NyummyCheckbox(checked = checked, onCheckedChange = { checked = it }, modifier = Modifier.testTag("checkbox"))
            }
        }

        composeRule.onNodeWithTag("checkbox").assert(hasRole(Role.Checkbox)).assertIsOff().performClick()
        composeRule.onNodeWithTag("checkbox").assertIsOn()
    }

    @Test
    fun 라디오는_누르면_선택된다() {
        composeRule.setContent {
            NyummyTheme {
                var selected by remember { mutableStateOf(false) }
                NyummyRadio(selected = selected, onClick = { selected = true }, modifier = Modifier.testTag("radio"))
            }
        }

        composeRule.onNodeWithTag("radio").assert(hasRole(Role.RadioButton)).assertIsNotSelected().performClick()
        composeRule.onNodeWithTag("radio").assertIsSelected()
    }

    @Test
    fun 스위치는_토글되고_비활성이면_바뀌지_않는다() {
        composeRule.setContent {
            NyummyTheme {
                Column {
                    var on by remember { mutableStateOf(false) }
                    NyummySwitch(checked = on, onCheckedChange = { on = it }, modifier = Modifier.testTag("switch"))
                    NyummySwitch(checked = false, onCheckedChange = {}, enabled = false, modifier = Modifier.testTag("disabled"))
                }
            }
        }

        composeRule.onNodeWithTag("switch").assert(hasRole(Role.Switch)).assertIsOff().performClick()
        composeRule.onNodeWithTag("switch").assertIsOn()
        composeRule.onNodeWithTag("disabled").assertIsNotEnabled()
    }

    @Test
    fun 콜백이_없으면_표시_전용이라_클릭할_수_없다() {
        composeRule.setContent {
            NyummyTheme {
                Column {
                    NyummyCheckbox(checked = true, onCheckedChange = null, modifier = Modifier.testTag("checkbox"))
                    NyummyRadio(selected = true, onClick = null, modifier = Modifier.testTag("radio"))
                    NyummySwitch(checked = true, onCheckedChange = null, modifier = Modifier.testTag("switch"))
                }
            }
        }

        composeRule.onNodeWithTag("checkbox").assertHasNoClickAction()
        composeRule.onNodeWithTag("radio").assertHasNoClickAction()
        composeRule.onNodeWithTag("switch").assertHasNoClickAction()
    }
}
