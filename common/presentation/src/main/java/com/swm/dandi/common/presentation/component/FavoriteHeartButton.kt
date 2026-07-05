package com.swm.dandi.common.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.swm.dandi.common.presentation.R
import com.swm.dandi.common.presentation.ui.theme.DesignSystemTheme
import com.swm.dandi.common.presentation.ui.theme.DesignSystemThemeImpl

@Composable
fun FavoriteHeartButton(
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val resource = if (isFavorite) {
        R.drawable.fill_favorite_24dp
    } else {
        R.drawable.outlined_favorite_24dp
    }

    Icon(
        modifier = modifier
            .clip(CircleShape)
            .clickable {
                onToggle(!isFavorite)
            },
        imageVector = ImageVector.vectorResource(resource),
        tint = DesignSystemThemeImpl.designSystemColor.contentFavorite,
        contentDescription = null,
    )
}

@Preview
@Composable
private fun FavoriteHeartButtonFavoritedPreview() {
    DesignSystemTheme {
        FavoriteHeartButton(isFavorite = true, onToggle = {})
    }
}

@Preview
@Composable
private fun FavoriteHeartButtonUnfavoritedPreview() {
    DesignSystemTheme {
        FavoriteHeartButton(isFavorite = false, onToggle = {})
    }
}
