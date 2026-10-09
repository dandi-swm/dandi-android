package com.dandi.nyummy.meal.data.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JpegExifSegmentTest {

    @Test
    fun `JFIF 뒤에 있는 EXIF 세그먼트를 마커째 읽는다`() {
        val jpeg = jpegOf(JFIF_SEGMENT, EXIF_SEGMENT)

        assertArrayEquals(EXIF_SEGMENT, readJpegExifSegment(jpeg.inputStream()))
    }

    @Test
    fun `EXIF 가 없는 JPEG 는 null`() {
        assertNull(readJpegExifSegment(jpegOf(JFIF_SEGMENT).inputStream()))
    }

    @Test
    fun `Exif 머리말이 아닌 APP1(XMP 등)은 EXIF 로 보지 않는다`() {
        val xmp = segment(0xE1, "http://ns.adobe.com/xap/1.0/\u0000".toByteArray())

        assertNull(readJpegExifSegment(jpegOf(xmp).inputStream()))
    }

    @Test
    fun `JPEG 가 아니면 null`() {
        assertNull(readJpegExifSegment(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47).inputStream()))
    }

    @Test
    fun `새 JPEG 의 JFIF 를 빼고 SOI 바로 뒤에 EXIF 를 넣는다`() {
        val encoded = jpegOf(JFIF_SEGMENT)

        val result = insertJpegExifSegment(encoded, EXIF_SEGMENT)

        assertArrayEquals(jpegOf(EXIF_SEGMENT), result)
        assertArrayEquals(EXIF_SEGMENT, readJpegExifSegment(result.inputStream()))
    }

    private companion object {
        val EXIF_HEADER = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00)
        val EXIF_SEGMENT = segment(0xE1, EXIF_HEADER + byteArrayOf(0x4D, 0x4D, 0x00, 0x2A, 0x01, 0x02, 0x03))
        val JFIF_SEGMENT = segment(0xE0, byteArrayOf(0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01))

        // SOS 부터 EOI 까지 이미지 데이터 자리.
        val IMAGE_DATA = byteArrayOf(0xFF.toByte(), 0xDA.toByte(), 0x00, 0x04, 0x11, 0x22, 0xFF.toByte(), 0xD9.toByte())

        fun segment(type: Int, payload: ByteArray): ByteArray {
            val length = payload.size + 2
            return byteArrayOf(0xFF.toByte(), type.toByte(), (length shr 8).toByte(), length.toByte()) + payload
        }

        fun jpegOf(vararg segments: ByteArray): ByteArray =
            segments.fold(byteArrayOf(0xFF.toByte(), 0xD8.toByte())) { acc, it -> acc + it } + IMAGE_DATA
    }
}
