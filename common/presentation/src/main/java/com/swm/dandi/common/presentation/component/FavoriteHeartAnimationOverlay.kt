package com.swm.dandi.common.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimatable
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.swm.dandi.common.presentation.R

private val HEART_ANIMATION_SIZE = 240.dp

@Composable
fun FavoriteHeartAnimationOverlay(
    modifier: Modifier = Modifier,
    favoriteTriggerKey: Int,
    clearTriggerKey: Int = 0,
) {
    val favoriteSpec = remember { LottieCompositionSpec.RawRes(R.raw.heart_animated) }
    val clearSpec = remember { LottieCompositionSpec.RawRes(R.raw.clear_heart_animated) }
    val favoriteComposition by rememberLottieComposition(favoriteSpec)
    val clearComposition by rememberLottieComposition(clearSpec)
    val animatable = rememberLottieAnimatable()
    var playingComposition by remember { mutableStateOf<LottieComposition?>(null) }

    PlayLottieOnTriggerEffect(
        triggerKey = favoriteTriggerKey,
        composition = favoriteComposition,
        animatable = animatable,
        onPlayingCompositionChange = { playingComposition = it },
    )

    PlayLottieOnTriggerEffect(
        triggerKey = clearTriggerKey,
        composition = clearComposition,
        animatable = animatable,
        onPlayingCompositionChange = { playingComposition = it },
    )

    val current = playingComposition
    if (current != null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LottieAnimation(
                composition = current,
                progress = { animatable.progress },
                modifier = Modifier.size(HEART_ANIMATION_SIZE),
            )
        }
    }
}

@Composable
private fun PlayLottieOnTriggerEffect(
    triggerKey: Int,
    composition: LottieComposition?,
    animatable: LottieAnimatable,
    onPlayingCompositionChange: (LottieComposition?) -> Unit,
) {
    LaunchedEffect(triggerKey, composition) {
        if (triggerKey > 0 && composition != null) {
            onPlayingCompositionChange(composition)
            animatable.animate(
                composition = composition,
                iterations = 1,
                continueFromPreviousAnimate = false,
            )
            onPlayingCompositionChange(null)
        }
    }
}
