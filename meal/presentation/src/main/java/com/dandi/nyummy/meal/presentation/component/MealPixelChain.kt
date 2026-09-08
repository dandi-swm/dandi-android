package com.dandi.nyummy.meal.presentation.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.math.roundToInt

/** 촬영본의 픽셀화 단계 비트맵 준비 상태입니다. */
sealed interface MealPixelChainResult {

    /** 디코드·다운스케일이 진행 중입니다. */
    data object Loading : MealPixelChainResult

    /** 체인이 준비되어 세리머니를 재생할 수 있습니다. */
    data class Ready(val chain: MealPixelChain) : MealPixelChainResult

    /** 디코드에 실패해 픽셀화 없이 진행해야 합니다. */
    data object Failed : MealPixelChainResult
}

/** [levels] 는 `[0]` 선명한 베이스(긴 변 ≤512px)부터 마지막 크런치 레벨까지 해상도 내림차순입니다. */
class MealPixelChain(val levels: List<ImageBitmap>)

/**
 * 촬영본을 픽셀화 단계 체인으로 디코드합니다.
 *
 * 촬영 확인 단계부터 미리 호출해 두면(프리웜) 세리머니 시작 지연이 없고, 제출 시
 * 업로드 파이프라인이 같은 파일을 재작성하기 전에 조용한 파일을 읽을 수 있습니다.
 */
@Composable
fun rememberMealPixelChain(photoPath: String?): MealPixelChainResult {
    var result by remember(photoPath) {
        mutableStateOf<MealPixelChainResult>(MealPixelChainResult.Loading)
    }
    LaunchedEffect(photoPath) {
        if (photoPath == null) return@LaunchedEffect
        result = withContext(Dispatchers.Default) {
            runCatching { buildPixelChain(photoPath) }.getOrNull()
                ?.let { MealPixelChainResult.Ready(it) }
                ?: MealPixelChainResult.Failed
        }
    }
    return result
}

private fun buildPixelChain(photoPath: String): MealPixelChain? {
    // 파일을 통째로 읽어 두면 이후 업로드 준비 과정의 in-place 재작성과 경합하지 않는다.
    val bytes = File(photoPath).readBytes()

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
    if (longEdge <= 0) return null

    var sampleSize = 1
    while (longEdge / (sampleSize * 2) >= BaseLongEdgePx) sampleSize *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null

    // coil 은 EXIF 회전을 자동 반영하지만 수동 디코드는 직접 픽셀에 구워야 한다.
    val base = decoded.rotatedBy(exifRotationDegrees(bytes))

    val levels = mutableListOf(base)
    var previous = base
    CrunchLongEdgesPx.forEach { targetEdge ->
        val previousEdge = maxOf(previous.width, previous.height)
        previous = if (previousEdge <= targetEdge) {
            previous
        } else {
            val scale = targetEdge / previousEdge.toFloat()
            Bitmap.createScaledBitmap(
                previous,
                (previous.width * scale).roundToInt().coerceAtLeast(1),
                (previous.height * scale).roundToInt().coerceAtLeast(1),
                // 연쇄 바이리니어 축소가 박스 필터에 가까워 모자이크 셀이 깔끔해진다.
                true,
            )
        }
        levels += previous
    }
    return MealPixelChain(levels.map(Bitmap::asImageBitmap))
}

private fun exifRotationDegrees(bytes: ByteArray): Int =
    runCatching { ExifInterface(ByteArrayInputStream(bytes)).rotationDegrees }.getOrDefault(0)

private fun Bitmap.rotatedBy(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private const val BaseLongEdgePx = 512

/** 크런치 단계별 긴 변 픽셀. 개수가 세리머니 스텝 수를 결정한다. */
internal val CrunchLongEdgesPx = intArrayOf(256, 144, 96, 64, 40, 24)
