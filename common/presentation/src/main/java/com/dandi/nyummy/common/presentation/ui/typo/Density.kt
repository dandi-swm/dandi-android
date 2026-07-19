package com.dandi.nyummy.common.presentation.ui.typo

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

private fun Int.textDp(density: Density): TextUnit = with(density) {
    this@textDp.dp.toSp()
}

private fun Float.textDp(density: Density): TextUnit = with(density) {
    this@textDp.dp.toSp()
}

internal val Int.textDp: TextUnit
    @Composable get() = this.textDp(density = LocalDensity.current)

internal val Float.textDp: TextUnit
    @Composable get() = this.textDp(density = LocalDensity.current)

@Composable
internal fun Dp.toPx(): Float = LocalDensity.current.run { this@toPx.toPx() }

@Composable
internal operator fun TextUnit.plus(other: TextUnit): TextUnit = (this.value + other.value).textDp

@Composable
internal operator fun TextUnit.minus(other: TextUnit): TextUnit = (this.value - other.value).textDp
