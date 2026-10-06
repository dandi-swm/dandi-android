package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasSetTextAction
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummyOverlaysTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 확인_다이얼로그는_닫기와_주_버튼을_각각_호출한다() {
        var confirmed = 0
        var dismissed = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyDialog(
                    title = "기록을 그만둘까요?",
                    body = "지금 나가면 사진이 사라져요.",
                    confirmText = "나가기",
                    onConfirm = { confirmed++ },
                    onDismissRequest = { dismissed++ },
                )
            }
        }

        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNodeWithText("기록을 그만둘까요?").assertIsDisplayed()
        composeRule.onNodeWithText("나가기").performClick()
        composeRule.onNodeWithText("닫기").performClick()

        assertEquals(1, confirmed)
        assertEquals(1, dismissed)
    }

    @Test
    fun 알림_다이얼로그는_닫기_버튼이_없다() {
        composeRule.setContent {
            NyummyTheme {
                NyummyDialog(
                    title = "전송하지 못했어요",
                    confirmText = "다시 시도",
                    onConfirm = {},
                    onDismissRequest = {},
                    type = NyummyDialogType.Alert,
                )
            }
        }

        composeRule.onNodeWithText("다시 시도").assertIsDisplayed()
        composeRule.onNodeWithText("닫기").assertDoesNotExist()
    }

    @Test
    fun 다이얼로그는_뒤로_가기로_닫힌다() {
        composeRule.setContent {
            NyummyTheme {
                var open by remember { mutableStateOf(true) }
                if (open) {
                    NyummyDialog(title = "제목", confirmText = "확인", onConfirm = {}, onDismissRequest = { open = false })
                }
            }
        }

        composeRule.onNode(isDialog()).assertExists()
        Espresso.pressBack()
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun 입력_다이얼로그는_내용_슬롯을_보여주고_주_버튼을_비활성화할_수_있다() {
        composeRule.setContent {
            NyummyTheme {
                var name by remember { mutableStateOf("비빔밥") }
                NyummyDialog(
                    title = "음식 이름 수정",
                    confirmText = "저장",
                    onConfirm = {},
                    onDismissRequest = {},
                    confirmEnabled = name.isNotBlank(),
                ) {
                    NyummyTextField(value = name, onValueChange = { name = it }, label = "음식 이름")
                }
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextReplacement("")
        composeRule.onNodeWithText("저장").assertIsNotEnabled()
    }

    @Test
    fun 바텀_시트는_내용을_보여준다() {
        composeRule.setContent {
            NyummyTheme {
                NyummyBottomSheet(onDismissRequest = {}) {
                    NyummySheetTitle("오늘 식사 요약")
                    NyummyButton(text = "밥 주기", onClick = {})
                }
            }
        }

        composeRule.onNodeWithText("오늘 식사 요약").assertIsDisplayed()
        composeRule.onNodeWithText("밥 주기").assertIsDisplayed()
    }

    @Test
    fun 확인_시트는_주_행동과_글자_행동을_각각_호출한다() {
        var primary = 0
        var secondary = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyConfirmSheet(
                    title = "기록을 그만둘까요?",
                    body = "냐미가 밥을 기다리고 있어요",
                    primaryText = "계속 기록하기",
                    onPrimary = { primary++ },
                    secondaryText = "그만두기",
                    onSecondary = { secondary++ },
                    onDismissRequest = {},
                )
            }
        }

        composeRule.onNodeWithText("냐미가 밥을 기다리고 있어요").assertIsDisplayed()
        composeRule.onNodeWithText("계속 기록하기").performClick()
        composeRule.onNodeWithText("그만두기").performClick()

        assertEquals(1, primary)
        assertEquals(1, secondary)
    }

    @Test
    fun 스낵바_행동을_누르면_호출된다() {
        var undo = 0
        composeRule.setContent {
            NyummyTheme { NyummySnackbar(message = "식사 기록을 삭제했어요", actionLabel = "실행 취소", onAction = { undo++ }) }
        }

        composeRule.onNodeWithText("식사 기록을 삭제했어요").assertIsDisplayed()
        composeRule.onNodeWithText("실행 취소").performClick()

        assertEquals(1, undo)
    }

    @Test
    fun 스낵바_호스트는_띄운_메시지를_냐미_스낵바로_그린다() {
        composeRule.setContent {
            NyummyTheme {
                val hostState = remember { SnackbarHostState() }
                LaunchedEffect(Unit) { hostState.showSnackbar("저장했어요") }
                NyummySnackbarHost(hostState = hostState)
            }
        }

        composeRule.onNodeWithText("저장했어요").assertIsDisplayed()
    }
}
