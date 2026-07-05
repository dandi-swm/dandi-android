package com.swm.dandi.common.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swm.dandi.common.presentation.R
import com.swm.dandi.common.presentation.ui.theme.DesignSystemTheme
import com.swm.dandi.common.presentation.ui.theme.DesignSystemThemeImpl

@Composable
fun BackArrowButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = ImageVector.vectorResource(R.drawable.arrow_back_24px),
            tint = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
            contentDescription = stringResource(R.string.nav_back_button),
        )
    }
}

@Preview
@Composable
private fun BackArrowButtonPreview() {
    DesignSystemTheme {
        BackArrowButton(
            onClick = {},
        )
    }
}