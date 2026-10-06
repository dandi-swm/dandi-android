package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** #91 리뷰에서 지적된 접근성, 비활성, 레이아웃 문제를 확인한다. */
@RunWith(AndroidJUnit4::class)
class NyummyReviewFixesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun hasError(message: String) = SemanticsMatcher.expectValue(SemanticsProperties.Error, message)

    @Test
    fun 텍스트_필드는_라벨과_오류를_접근성으로_알린다() {
        composeRule.setContent {
            NyummyTheme { NyummyTextField(value = "cat@", onValueChange = {}, label = "이메일", errorMessage = "이메일 형식을 확인해 주세요") }
        }

        composeRule.onNodeWithContentDescription("이메일").assert(hasError("이메일 형식을 확인해 주세요"))
    }

    @Test
    fun 텍스트_영역은_라벨과_오류를_접근성으로_알린다() {
        composeRule.setContent {
            NyummyTheme { NyummyTextArea(value = "", onValueChange = {}, label = "문의 내용", errorMessage = "내용을 입력해 주세요") }
        }

        composeRule.onNodeWithContentDescription("문의 내용").assert(hasError("내용을 입력해 주세요"))
    }

    @Test
    fun 인증_코드_오류는_접근성으로_알린다() {
        composeRule.setContent {
            NyummyTheme { NyummyCodeInput(value = "123456", onValueChange = {}, isError = true, errorMessage = "코드가 맞지 않아요") }
        }

        composeRule.onNodeWithContentDescription("인증 코드 입력").assert(hasError("코드가 맞지 않아요"))
    }

    @Test
    fun 비활성_세그먼트는_선택을_바꿀_수_없다() {
        var selected = 0
        composeRule.setContent {
            NyummyTheme {
                NyummySegmentedControl(persistentListOf("남성", "여성"), selectedIndex = 0, onSelect = { selected = it }, enabled = false)
            }
        }

        composeRule.onNodeWithText("여성").assertIsNotEnabled().performClick()
        composeRule.onNodeWithText("남성").assertIsSelected()
        assertEquals(0, selected)
    }

    @Test
    fun 휠_피커는_처음_값으로_돌아와도_변경을_알린다() {
        val changes = mutableListOf<Int>()
        composeRule.setContent {
            NyummyTheme {
                var index by remember { mutableIntStateOf(5) }
                NyummyWheelPicker(
                    items = (0..20).map(Int::toString).toImmutableList(),
                    selectedIndex = index,
                    onSelectedIndexChange = { index = it; changes += it },
                    modifier = Modifier.width(160.dp).testTag("wheel"),
                )
            }
        }

        composeRule.onNodeWithTag("wheel").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        val moved = changes.lastOrNull()
        composeRule.onNodeWithTag("wheel").performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertTrue("위로 스크롤하면 값이 바뀐다: $changes", moved != null && moved != 5)
        assertTrue("다시 내리면 또 알린다: $changes", changes.size >= 2 && changes.last() != moved)
    }

    @Test
    fun 상단_바의_긴_제목은_넓은_오른쪽_슬롯과_겹치지_않는다() {
        composeRule.setContent {
            NyummyTheme {
                NyummyTopBar(
                    title = "아주 길고 길어서 화면을 가득 채우는 설정 화면 제목",
                    onBackClick = {},
                    trailing = { NyummyTextButton(text = "문의하기", onClick = {}, modifier = Modifier.testTag("trailing")) },
                    modifier = Modifier.width(360.dp),
                )
            }
        }

        val title = composeRule.onAllNodesWithText("아주 길고", substring = true)[0].getBoundsInRoot()
        val trailing = composeRule.onNodeWithTag("trailing").getBoundsInRoot()
        assertTrue("제목 오른쪽 ${title.right} <= 슬롯 왼쪽 ${trailing.left}", title.right <= trailing.left)
    }

    @Test
    fun 좁은_화면에서도_하단_내비_탭이_겹치지_않는다() {
        composeRule.setContent {
            NyummyTheme { NyummyBottomNav(NyummyMainTabs, 0, {}, modifier = Modifier.width(320.dp)) }
        }

        val bounds = listOf("홈", "기록", "퀘스트", "업적", "상점").map { label ->
            // 탭은 selectable로 병합되어 라벨로 찾으면 탭 전체 영역이 잡힌다.
            composeRule.onNodeWithText(label).getBoundsInRoot()
        }
        bounds.zipWithNext().forEach { (a, b) -> assertTrue("탭이 겹침: $a / $b", a.right <= b.left) }
    }
}
