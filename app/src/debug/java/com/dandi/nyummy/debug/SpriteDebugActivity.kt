package com.dandi.nyummy.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.R
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummySpriteSheet
import com.dandi.nyummy.common.presentation.component.NyummySpriteView
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 냐미 스프라이트 시트 연속 재생 확인용 디버그 액티비티 (debug 빌드에만 포함).
 *
 * doze → sleep_loop(3회) → wake 순서로 [NyummySpriteView]를 이어 재생하며,
 * 하단 라벨을 탭하면 해당 구간으로 바로 이동한다.
 *
 * 실행: adb shell am start -n com.dandi.nyummy/.debug.SpriteDebugActivity
 */
class SpriteDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DesignSystemTheme {
                SpriteDebugScreen()
            }
        }
    }
}

/** 재생 구간 하나: 시트 + 반복 횟수. 프레임 타이밍은 디자이너 프리뷰 GIF와 동일하게 맞춘다. */
private data class SpriteSegment(
    val label: String,
    val sheet: NyummySpriteSheet,
    val cycles: Int,
)

private val segments = listOf(
    SpriteSegment(
        label = "졸음 doze",
        // 544x680 격자(4x5)지만 실제 프레임은 17개 — 마지막 줄 3칸은 빈 칸
        sheet = NyummySpriteSheet(
            imageRes = R.drawable.nyami_doze_grid_136,
            frameWidth = 136,
            frameHeight = 136,
            totalFrames = 17,
            framesPerRow = 4,
            frameDurationMillis = 150,
        ),
        cycles = 1,
    ),
    SpriteSegment(
        label = "잠 sleep_loop",
        sheet = NyummySpriteSheet(
            imageRes = R.drawable.nyami_sleep_loop_grid_136,
            frameWidth = 136,
            frameHeight = 136,
            totalFrames = 8,
            framesPerRow = 4,
            frameDurationMillis = 100,
        ),
        cycles = 3,
    ),
    SpriteSegment(
        label = "기상 wake",
        sheet = NyummySpriteSheet(
            imageRes = R.drawable.nyami_wake_grid_136,
            frameWidth = 136,
            frameHeight = 136,
            totalFrames = 17,
            framesPerRow = 4,
            frameDurationMillis = 150,
        ),
        cycles = 1,
    ),
)

@Composable
private fun SpriteDebugScreen() {
    var segmentIndex by remember { mutableIntStateOf(0) }
    val segment = segments[segmentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // key: 구간이 바뀌면 애니메이션을 새로 만들어 0번 프레임부터 재시작
        key(segmentIndex) {
            NyummySpriteView(
                sheet = segment.sheet,
                displayWidth = 300.dp,
                iterations = segment.cycles,
                onAnimationEnd = { segmentIndex = (segmentIndex + 1) % segments.size },
                contentDescription = segment.label,
            )
        }

        DandiText(
            text = segment.label,
            modifier = Modifier.padding(top = 24.dp),
            style = DesignSystemThemeImpl.typeScale.textStrongM,
        )

        Row(
            modifier = Modifier.padding(top = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            segments.forEachIndexed { index, item ->
                DandiText(
                    text = item.label,
                    modifier = Modifier
                        .clickable { segmentIndex = index }
                        .padding(8.dp),
                    color = if (index == segmentIndex) {
                        DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0
                    } else {
                        DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1
                    },
                )
            }
        }

        // 재생이 끝나기를 기다리지 않고 다음 구간으로 바로 넘기는 버튼.
        // key(segmentIndex)가 바뀌며 진행 중이던 애니메이션은 버려지고 새 구간이 0번 프레임부터 돈다.
        NyummyButton(
            label = "다음 구간 ▶",
            modifier = Modifier.padding(top = 24.dp),
            onClick = { segmentIndex = (segmentIndex + 1) % segments.size },
        )
    }
}
