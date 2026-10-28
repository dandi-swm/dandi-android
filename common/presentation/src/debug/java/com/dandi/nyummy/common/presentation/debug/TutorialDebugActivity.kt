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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import com.dandi.nyummy.common.presentation.coachmark.CoachMark
import com.dandi.nyummy.common.presentation.coachmark.CoachMarkContent
import com.dandi.nyummy.common.presentation.coachmark.CoachMarkKey
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBubbleTail
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyHealthStoryDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyHealthStoryPage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNoticeDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNoticePage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialFinishDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialLesson
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialSkipDialog
import kotlinx.collections.immutable.toImmutableList
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialCard
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialExpression
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 서버 없이 튜토리얼 공통 컴포넌트를 띄워 피그마 `v2 · 25 Tutorial`과 비교하는 디버그 액티비티 (debug 빌드에만 포함).
 * 새 소식 공지 다이얼로그도 띄워 `v2 · 20 Home & My Room`의 Notice · 01~03과 비교한다.
 *
 * 실행 예:
 * adb shell am start -n com.dandi.nyummy/com.dandi.nyummy.common.presentation.debug.TutorialDebugActivity --es scene cards
 *
 * - scene: cards(안내 카드 꼬리 세 가지), intro(대상 없는 소개), bottom(바텀 네비 카메라), mail(오른쪽 위 우편 버튼,
 *   건너뛰기 숨김), back(왼쪽 위 뒤로 가기, 냐미 가운데), skip(건너뛰기 확인), story0~story2(냐미 건강 이야기), finish(마침),
 *   notice(새 소식 세 장: 업데이트, 사진 찍는 법, 이벤트), notice1(새 소식 한 장)
 * - images: notice 장면의 사진 주소를 쉼표로 나눠 장 순서대로 준다. 없으면 연초록 바탕만 보인다.
 *   debug 빌드는 평문 HTTP를 허용하므로 `adb reverse tcp:8765 tcp:8765` 뒤 `http://localhost:8765/a.png`처럼 쓴다.
 */
class TutorialDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scene = intent.getStringExtra(EXTRA_SCENE) ?: SCENE_CARDS
        val images = intent.getStringExtra(EXTRA_IMAGES)?.split(',')?.map { it.trim() }.orEmpty()
        setContent {
            NyummyTheme {
                when (scene) {
                    SCENE_CARDS -> CardsScene()
                    "notice", "notice1" -> NoticeScene(single = scene == "notice1", images = images)
                    "skip", "finish", "story0", "story1", "story2" -> DialogScene(scene)
                    else -> CoachMarkScene(scene)
                }
            }
        }
    }

    private companion object {
        const val EXTRA_SCENE = "scene"
        const val EXTRA_IMAGES = "images"
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

@Composable
private fun DialogScene(scene: String) {
    Box(Modifier.fillMaxSize().background(NyummyTheme.colors.bg.surfaceSunken)) {
        when (scene) {
            "skip" -> NyummyTutorialSkipDialog(
                remaining = NyummyTutorialLesson.entries.toImmutableList(),
                onContinue = {},
                onSkip = {},
            )
            "finish" -> NyummyTutorialFinishDialog(
                title = "이제 준비 끝!\n다음 끼니를 기다릴게",
                body = "밥 먹을 때 가운데 카메라 버튼만 눌러 줘.\n기록할 때마다 보상 상자가 우편함으로 가.",
                buttonText = "시작하기",
                onStart = {},
            )
            else -> {
                val index = scene.removePrefix("story").toInt()
                val (title, body) = listOf(
                    "꾸준히 기록하면\n우리 둘 다 건강해!" to "집사가 끼니를 기록할 때마다 나도 같이 먹어.\n잘 챙겨 주면 지금처럼 딱 좋은 몸이야.",
                    "끼니를 거르면\n내가 홀쭉해져" to "집사가 밥을 굶으면 나도 같이 굶어서\n점점 말라 가. 다시 챙겨 주면 금방 돌아와!",
                    "너무 많이 먹으면\n통통 뚱냥이가 돼" to "집사가 과식하면 나도 같이 많이 먹어서\n동글동글해져. 알맞게 먹는 게 둘 다 좋아.",
                )[index]
                NyummyHealthStoryDialog(
                    page = NyummyHealthStoryPage.entries[index],
                    pageIndex = index,
                    title = title,
                    body = body,
                    buttonText = if (index == 2) "알겠어!" else "다음",
                    onNext = {},
                )
            }
        }
    }
}

@Composable
private fun NoticeScene(single: Boolean, images: List<String>) {
    var todayChecked by remember { mutableStateOf(false) }
    val pages = listOf(
        NyummyNoticePage(
            badge = "업데이트",
            imageUrl = images.getOrNull(0),
            title = "냐미가 새단장을 했어요!",
            body = "화면을 더 보기 편하게 새로 꾸몄어요.\n새로 들어온 기능도 확인해 보세요.",
        ),
        NyummyNoticePage(
            badge = "사진 팁",
            imageUrl = images.getOrNull(1),
            title = "냐미는 진짜 밥상을 좋아해요",
            body = "지금 먹을 음식을 바로 찍어 주세요.\n글로 쓴 식단이나 화면 속 음식 사진은\n냐미가 알아보기 어려워요.",
        ),
        NyummyNoticePage(
            badge = "이벤트",
            imageUrl = images.getOrNull(2),
            meta = "10월 28일 ~ 11월 10일",
            title = "가을 한 상 이벤트가 열렸어요",
            body = "이벤트 페이지에서 자세히 알아보세요.",
            ctaText = "자세히 보기",
        ),
    ).let { if (single) it.take(1) else it }.toImmutableList()
    Box(Modifier.fillMaxSize().background(NyummyTheme.colors.bg.surfaceSunken)) {
        NyummyNoticeDialog(
            pages = pages,
            todayChecked = todayChecked,
            onTodayCheckedChange = { todayChecked = it },
            onClose = {},
            onCtaClick = {},
        )
    }
}
