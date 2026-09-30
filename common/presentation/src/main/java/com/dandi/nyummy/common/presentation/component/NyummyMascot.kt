package com.dandi.nyummy.common.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme

/**
 * 수채화 냐미 일러스트의 포즈.
 *
 * 픽셀 스프라이트(`NyummySpriteView`)와는 별개의 정지 일러스트 계열로,
 * 캐릭터 탭·빈 상태처럼 픽셀 연출이 없는 화면에서 사용한다.
 */
enum class NyummyMascotPose(@DrawableRes internal val drawableRes: Int) {
    Welcome(R.drawable.nyummy_character_welcome),
    Eating(R.drawable.nyummy_character_eating),
    Sleeping(R.drawable.nyummy_character_sleeping),

    /** 카드 윗변에 앞발을 걸치고 매달린 포즈. 카드 위로 반쯤 튀어나오게 배치할 때 쓴다. */
    Hanging(R.drawable.nyummy_character_hanging),
}

/**
 * 수채화 냐미 일러스트 1컷을 그린다. 크기는 호출부에서 [modifier]의 `size()`로 지정한다.
 */
@Composable
fun NyummyMascot(
    pose: NyummyMascotPose,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(pose.drawableRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun NyummyMascotPreview() {
    DesignSystemTheme {
        Column {
            NyummyMascotPose.entries.forEach { pose ->
                NyummyMascot(pose = pose, modifier = Modifier.size(120.dp))
            }
        }
    }
}
