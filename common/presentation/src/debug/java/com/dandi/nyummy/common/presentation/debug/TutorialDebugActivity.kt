package com.dandi.nyummy.common.presentation.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import com.dandi.nyummy.common.presentation.coachmark.CoachMark
import com.dandi.nyummy.common.presentation.coachmark.CoachMarkContent
import com.dandi.nyummy.common.presentation.coachmark.CoachMarkKey
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBubbleTail
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialCard
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialExpression
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 서버 없이 튜토리얼 공통 컴포넌트를 띄워 피그마 `v2 · 25 Tutorial`과 비교하는 디버그 액티비티 (debug 빌드에만 포함).
 *
 * 실행 예:
 * adb shell am start -n com.dandi.nyummy/com.dandi.nyummy.common.presentation.debug.TutorialDebugActivity --es scene cards
 *
 * - scene: cards(안내 카드 꼬리 세 가지), intro(대상 없는 소개), bottom(바텀 네비 카메라), mail(오른쪽 위 우편 버튼,
 *   건너뛰기 숨김), back(왼쪽 위 뒤로 가기, 냐미 가운데)
 */
class TutorialDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scene = intent.getStringExtra(EXTRA_SCENE) ?: SCENE_CARDS
        setContent {
            NyummyTheme {
                when (scene) {
                    SCENE_CARDS -> CardsScene()
                    else -> CoachMarkScene(scene)
                }
            }
        }
    }

    private companion object {
        const val EXTRA_SCENE = "scene"
        const val SCENE_CARDS = "cards"
    }
}

@Composable
private fun CardsScene() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.surfaceSunken)
            .statusBarsPadding()
            .padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s16),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s24),
    ) {
        NyummyTutorialCard(
            title = "안녕, 집사! 나는 냐미야",
            body = "집사가 밥을 기록하면 나도 같이 먹을 수 있어",
            expression = NyummyTutorialExpression.Greet,
            showHint = true,
        )
        NyummyTutorialCard(
            title = "여기를 눌러 첫 기록을 해 보자!",
            body = "밥을 먹을 때마다 사진 한 장이면 기록 끝이야",
            expression = NyummyTutorialExpression.Point,
            tail = NyummyBubbleTail.Bottom,
            tailCenterX = 175.dp,
        )
        NyummyTutorialCard(
            title = "열기를 눌러 볼까?",
            body = "상자 안에 뭐가 들었을까",
            expression = NyummyTutorialExpression.Point,
            tail = NyummyBubbleTail.Top,
            tailCenterX = 296.dp,
        )
    }
}

@Composable
private fun CoachMarkScene(scene: String) {
    val density = LocalDensity.current
    fun dpRect(x: Int, y: Int, w: Int, h: Int) = with(density) { Rect(x.dp.toPx(), y.dp.toPx(), (x + w).dp.toPx(), (y + h).dp.toPx()) }
    val statusBar = 44
    val (mark, bounds) = when (scene) {
        "bottom" -> CoachMark(
            title = "여기를 눌러 첫 기록을 해 보자!",
            body = "밥을 먹을 때마다 사진 한 장이면 기록 끝이야",
            expression = NyummyTutorialExpression.Point,
            target = CoachMarkKey("camera"),
            onSkip = {},
        ) to dpRect(165, 760, 60, 60)
        "mail" -> CoachMark(
            title = "보상 상자가 도착했어!",
            body = "우편함에서 같이 열어 보자",
            expression = NyummyTutorialExpression.Surprise,
            target = CoachMarkKey("mail"),
            onSkip = {},
        ) to dpRect(282, statusBar + 8, 40, 40)
        "back" -> CoachMark(
            title = "보상을 받았어! 이제 방을 꾸며 볼까?",
            body = "뒤로 가서 마이룸으로 가 보자",
            expression = NyummyTutorialExpression.Cheer,
            target = CoachMarkKey("back"),
            onSkip = {},
        ) to dpRect(12, statusBar + 8, 40, 40)
        else -> CoachMark(
            title = "안녕, 집사! 나는 냐미야",
            body = "집사가 밥을 기록하면 나도 같이 먹을 수 있어",
            expression = NyummyTutorialExpression.Greet,
            onSkip = {},
        ) to null
    }
    Box(Modifier.fillMaxSize().background(NyummyTheme.colors.bg.surfaceSunken)) {
        CoachMarkContent(mark = mark, targetBounds = bounds, anchorBounds = null, onTargetClick = {})
    }
}
