package com.dandi.nyummy.mailbox.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.dandi.nyummy.mailbox.domain.InquiryImageLoadException
import java.io.File
import java.io.InputStream

/**
 * 갤러리에서 고른 사진(content URI)을 문의에 붙일 JPEG 파일로 만드는 로컬 데이터소스.
 *
 * 선택기가 준 URI의 읽기 권한은 잠깐뿐이라, 고르자마자 앱 캐시에 다시 인코딩해 두고 보낼 때 그 파일을 올린다.
 * 다시 인코딩하면서 회전은 픽셀에 반영하고, 위치 같은 사진 정보(EXIF)는 남기지 않는다.
 * PNG 스크린샷이나 HEIC 사진도 모두 JPEG로 바꾼다(서버는 JPEG와 PNG만 받는다).
 */
class InquiryImageDataSource(
    private val context: Context,
) {

    /** [photoUri]를 JPEG로 바꿔 앱 캐시에 저장하고 그 절대 경로를 돌려준다. 전에 붙였던 사진 파일은 지운다. */
    fun prepare(photoUri: String): String {
        val uri = Uri.parse(photoUri)
        val file = File(context.cacheDir, "$FILE_PREFIX${System.currentTimeMillis()}.jpg")
        return runCatching {
            val bitmap = decode(uri)
            try {
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            } finally {
                bitmap.recycle()
            }
            deleteOthers(keep = file)
            file.absolutePath
        }.getOrElse { cause ->
            file.delete()
            throw cause as? InquiryImageLoadException ?: InquiryImageLoadException(cause)
        }
    }

    /** 긴 변을 [INQUIRY_IMAGE_MAX_LONG_SIDE_PX] 이하로 줄이고 바로 세운 비트맵. 투명한 곳은 흰 바탕으로 채운다. */
    private fun decode(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw InquiryImageLoadException()

        val options = BitmapFactory.Options().apply {
            inSampleSize = inquiryImageSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = open(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw InquiryImageLoadException()
        val rotationDegrees = runCatching { open(uri).use { ExifInterface(it).rotationDegrees } }.getOrDefault(0)

        val (targetWidth, targetHeight) = inquiryImageTargetSize(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            postScale(targetWidth.toFloat() / decoded.width, targetHeight.toFloat() / decoded.height)
            postRotate(rotationDegrees.toFloat())
        }
        val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (transformed !== decoded) decoded.recycle()
        return transformed.onWhite()
    }

    private fun Bitmap.onWhite(): Bitmap {
        if (!hasAlpha()) return this
        val opaque = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply {
            drawColor(Color.WHITE)
            drawBitmap(this@onWhite, 0f, 0f, null)
        }
        recycle()
        return opaque
    }

    private fun open(uri: Uri): InputStream =
        runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            ?: throw InquiryImageLoadException()

    private fun deleteOthers(keep: File) {
        context.cacheDir.listFiles { file -> file.name.startsWith(FILE_PREFIX) && file != keep }
            ?.forEach { it.delete() }
    }

    private companion object {
        const val FILE_PREFIX = "inquiry_image_"
        const val JPEG_QUALITY = 90
    }
}
