package com.swm.dandi.common.presentation.imageUtil

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.bitmapConfig
import coil3.request.crossfade

@Composable
fun rememberImageLoader(): ImageLoader {
    val context = LocalPlatformContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .bitmapConfig(Bitmap.Config.RGB_565)
            .crossfade(true)
            .build()
    }
}