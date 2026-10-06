package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 끝을 알 수 없는 처리 중 표시. 브랜드색 loader-circle 아이콘(36)을 한 방향으로 계속 돌린다.
 * 진행률을 알 수 있으면 [NyummyLinearProgress]나 [NyummyCircularProgress]를 쓴다.
 *
 * 옆에 같은 내용의 문구가 있으면 [contentDescription]은 null로 두어 두 번 읽히지 않게 한다.
 */
@Composable
fun NyummySpinner(
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val rotation by rememberInfiniteTransition(label = "NyummySpinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(SpinnerRotationMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "NyummySpinnerRotation",
    )
    Icon(
        painter = painterResource(R.drawable.nyummy_ic_loader_circle),
        contentDescription = contentDescription,
        tint = NyummyTheme.colors.content.brand,
        modifier = modifier
            .size(NyummyComponentDimens.SpinnerSize)
            .graphicsLayer { rotationZ = rotation },
    )
}

private const val SpinnerRotationMillis = 900

@Preview(showBackground = true)
@Composable
private fun NyummySpinnerPreview() {
    NyummyTheme {
        NyummySpinner(contentDescription = null)
    }
}
