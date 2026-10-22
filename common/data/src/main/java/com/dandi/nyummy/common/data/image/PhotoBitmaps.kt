package com.dandi.nyummy.common.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import java.io.InputStream

/*
 * 사진을 업로드용 JPEG로 다시 만들 때 기능마다 같은 비트맵 처리(줄여 풀기, 회전, 투명 배경 채우기).
 * EXIF를 남길지, 얼마나 줄일지는 쓰는 쪽(식사 기록, 문의)이 정한다.
 */

/**
 * [open]이 여는 사진을 [sampleSizeOf]가 정한 배율(inSampleSize)로 줄여 푼다. 이미지로 읽을 수 없으면 null.
 * 그 배율로도 메모리가 모자라면 배율을 두 배씩 키워 다시 푼다.
 *
 * 스트림은 한 번 읽으면 되돌릴 수 없어서 크기를 읽을 때와 풀 때 [open]을 따로 부른다.
 */
fun decodeSampledBitmap(
    open: () -> InputStream,
    sampleSizeOf: (width: Int, height: Int) -> Int,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    // 이미지를 decode할 때 전부다 해버리면 OOM이 생길 수 도 있으니 bounds로 크기, 용량만 확인
    open().use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = sampleSizeOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    while (true) {
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        try {
            return open().use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: OutOfMemoryError) {
            if (sampleSize >= Int.MAX_VALUE / 2) return null
            sampleSize *= 2
        }
    }
}

/**
 * [rotationDegrees]만큼 돌리고 [targetWidth]×[targetHeight](돌리기 전 기준)로 맞춘 비트맵.
 * 바꿀 것이 없으면 그대로 돌려주고, 새로 만들었으면 원래 비트맵은 정리한다.
 */
fun Bitmap.transformed(
    targetWidth: Int = width,
    targetHeight: Int = height,
    rotationDegrees: Int = 0,
): Bitmap {
    if (targetWidth == width && targetHeight == height && rotationDegrees % 360 == 0) return this
    val matrix = Matrix().apply {
        postScale(targetWidth.toFloat() / width, targetHeight.toFloat() / height)
        postRotate(rotationDegrees.toFloat())
    }
    val transformed = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (transformed !== this) recycle()
    return transformed
}

/**
 * 투명한 곳을 흰 바탕으로 채운 비트맵. JPEG는 투명을 담지 못해 그대로 저장하면 투명한 곳이 까맣게 된다.
 * 투명 채널이 없으면 그대로 돌려주고, 새로 만들었으면 원래 비트맵은 정리한다.
 */
fun Bitmap.onWhiteBackground(): Bitmap {
    if (!hasAlpha()) return this
    val opaque = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Canvas(opaque).apply {
        drawColor(Color.WHITE)
        drawBitmap(this@onWhiteBackground, 0f, 0f, null)
    }
    recycle()
    return opaque
}
