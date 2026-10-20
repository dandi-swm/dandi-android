package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import kotlinx.coroutines.delay

/**
 * 읽기만 하면 되는 짧은 안내 토스트. 스낵바와 같은 흰 바탕 + 2px 테두리 + float 그림자의 알약이고, 글자 길이만큼만 차지한다.
 * "준비 중인 기능이에요."처럼 누를 것이 없는 안내에 쓴다. 되돌리기처럼 버튼이 있으면 [NyummySnackbar]를 쓴다.
 */
@Composable
fun NyummyToast(
    message: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.full)
    Box(
        modifier = modifier
            .nyummyShadow(shape, NyummyTheme.elevation.float)
            .background(NyummyTheme.colors.bg.surface, shape)
            .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.border.default, shape)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = NyummyTheme.spacing.s20, vertical = NyummyTheme.spacing.s12),
        contentAlignment = Alignment.Center,
    ) {
        NyummyText(
            text = message,
            style = NyummyTheme.typography.bodyM,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** [NyummyToastHost]에 띄운 토스트 하나. 같은 문구를 다시 띄워도 새 토스트로 보고 시간을 새로 센다. */
@Stable
class NyummyToastData internal constructor(val message: String)

/** 토스트를 띄우는 상태. 한 번에 하나만 보이고, 떠 있는 중에 새로 띄우면 바로 바뀐다. */
@Stable
class NyummyToastHostState {
    var currentToast: NyummyToastData? by mutableStateOf(null)
        private set

    fun show(message: String) {
        currentToast = NyummyToastData(message)
    }

    internal fun dismiss(toast: NyummyToastData) {
        if (currentToast === toast) currentToast = null
    }
}

/**
 * [hostState]의 토스트를 화면 하단 가운데에 [NyummyToastDurationMillis]만큼 띄운다. 접근성 설정에서 늘린 시간도 따른다.
 * 좌우 gutter와 최대 너비 480은 스낵바와 같다.
 */
@Composable
fun NyummyToastHost(
    hostState: NyummyToastHostState,
    modifier: Modifier = Modifier,
) {
    val toast = hostState.currentToast
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(toast) {
        if (toast == null) return@LaunchedEffect
        delay(
            accessibilityManager?.calculateRecommendedTimeoutMillis(NyummyToastDurationMillis, containsText = true)
                ?: NyummyToastDurationMillis,
        )
        hostState.dismiss(toast)
    }
    AnimatedContent(
        targetState = toast,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut()).using(null)
        },
        contentAlignment = Alignment.BottomCenter,
        // 같은 문구를 이어서 띄우면 깜빡이지 않고 시간만 새로 센다.
        contentKey = { it?.message },
        label = "NyummyToast",
    ) { shown ->
        if (shown != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                NyummyToast(
                    message = shown.message,
                    modifier = Modifier
                        .padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s12)
                        .widthIn(max = NyummyComponentDimens.ContentMaxWidth),
                )
            }
        }
    }
}

/** 토스트가 떠 있는 시간. 스낵바(4초)보다 짧게 둔다. */
const val NyummyToastDurationMillis = 1_000L

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyToastPreview() {
    NyummyTheme {
        Box(Modifier.padding(NyummyTheme.spacing.gutter)) {
            NyummyToast(message = "준비 중인 기능이에요.")
        }
    }
}
