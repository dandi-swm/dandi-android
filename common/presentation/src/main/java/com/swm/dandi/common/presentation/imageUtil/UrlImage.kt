package com.swm.dandi.common.presentation.imageUtil

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage

private const val DANDI_DRAWABLE_URI_PREFIX = "dandi://drawable/"

@Composable
fun UrlImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val drawableResourceId = remember(imageUrl, context) {
        imageUrl.toDrawableResourceId(context)
    }

    when {
        drawableResourceId != null -> {
            Image(
                painter = painterResource(drawableResourceId),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        }

        imageUrl.isBlank() || imageUrl.startsWith(DANDI_DRAWABLE_URI_PREFIX) -> fallback()

        else -> {
            AsyncImage(
                model = imageUrl,
                imageLoader = rememberImageLoader(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        }
    }
}

private fun String.toDrawableResourceId(context: Context): Int? {
    if (!startsWith(DANDI_DRAWABLE_URI_PREFIX)) return null

    val drawableName = removePrefix(DANDI_DRAWABLE_URI_PREFIX)
        .substringBefore('?')
        .substringBefore('#')
        .takeIf { it.isNotBlank() }
        ?: return null

    return context.resources
        .getIdentifier(drawableName, "drawable", context.packageName)
        .takeIf { it != 0 }
}
