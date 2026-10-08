package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 냐미 포즈(매끄러운 일러스트).
 * 포즈별 쓰임은 정해져 있다: 빈 상태 Sleep, 오류 Worry, 권한 Ask, 분석 대기 Taste, 오늘 기록 기다림 Gasp.
 * 다이얼로그에는 캐릭터를 쓰지 않는다.
 */
enum class NyummyPose(@DrawableRes internal val drawable: Int) {
    Hello(R.drawable.nyummy_pose_hello),
    Shy(R.drawable.nyummy_pose_shy),
    Sleep(R.drawable.nyummy_pose_sleep),
    Worry(R.drawable.nyummy_pose_worry),
    Ask(R.drawable.nyummy_pose_ask),
    Taste(R.drawable.nyummy_pose_taste),
    Gasp(R.drawable.nyummy_pose_gasp),
}

/**
 * 정사각형 냐미 포즈. 크기는 `NyummyTheme.size.character*`(xs 48, s 72, m 120, l 160, hero 224)로 준다.
 * 장식이면 [contentDescription]을 null로 둔다.
 */
@Composable
fun NyummyPoseImage(
    pose: NyummyPose,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(pose.drawable),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

@Preview(showBackground = true)
@Composable
private fun NyummyPosePreview() {
    NyummyTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
            NyummyPose.entries.forEach { NyummyPoseImage(pose = it, size = NyummyTheme.size.characterS) }
        }
    }
}
