package com.dandi.nyummy.meal.data.util

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream

/*
 * JPEG 의 EXIF 는 APP1 세그먼트(`FF E1` + 길이 + "Exif\0\0" + TIFF 데이터) 하나에 통째로 들어 있다.
 * 다시 압축할 때 이 세그먼트를 바이트 그대로 옮겨 붙이면, ExifInterface 가 모르는 태그(제조사 MakerNote 등)와
 * 썸네일까지 하나도 잃지 않는다.
 */

private const val MARKER_PREFIX = 0xFF
private const val SOI = 0xD8
private const val EOI = 0xD9
private const val SOS = 0xDA
private const val APP0 = 0xE0
private const val APP1 = 0xE1
private const val TEM = 0x01
private val RST_MARKERS = 0xD0..0xD7
private val EXIF_HEADER = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00) // "Exif\0\0"

/**
 * JPEG 스트림에서 EXIF APP1 세그먼트를 마커와 길이까지 포함해 읽는다.
 * JPEG 가 아니거나, 이미지 데이터(SOS) 전까지 EXIF 가 없거나, 헤더가 깨져 있으면 null.
 */
internal fun readJpegExifSegment(input: InputStream): ByteArray? = try {
    val data = DataInputStream(input)
    if (data.readUnsignedByte() != MARKER_PREFIX || data.readUnsignedByte() != SOI) {
        null
    } else {
        findExifSegment(data)
    }
} catch (_: EOFException) {
    null
}

private fun findExifSegment(data: DataInputStream): ByteArray? {
    while (true) {
        if (data.readUnsignedByte() != MARKER_PREFIX) return null
        var type = data.readUnsignedByte()
        while (type == MARKER_PREFIX) type = data.readUnsignedByte() // 채움 바이트
        when {
            type == SOS || type == EOI -> return null
            type == TEM || type in RST_MARKERS -> continue // 길이 없는 마커
        }
        val length = data.readUnsignedShort()
        if (length < 2) return null
        val payload = ByteArray(length - 2)
        data.readFully(payload)
        if (type == APP1 && payload.startsWith(EXIF_HEADER)) {
            return byteArrayOf(MARKER_PREFIX.toByte(), APP1.toByte(), (length shr 8).toByte(), length.toByte()) + payload
        }
    }
}

/**
 * 새로 인코딩한 [jpeg] 의 SOI 바로 뒤에 [exifSegment] 를 넣는다.
 * 인코더가 붙인 앞쪽 APP0(JFIF)·APP1 은 EXIF 와 겹치므로 빼고 넣는다.
 */
internal fun insertJpegExifSegment(jpeg: ByteArray, exifSegment: ByteArray): ByteArray {
    require(jpeg.size >= 2 && jpeg.unsigned(0) == MARKER_PREFIX && jpeg.unsigned(1) == SOI) { "JPEG 가 아니에요" }
    var offset = 2
    while (offset + 4 <= jpeg.size && jpeg.unsigned(offset) == MARKER_PREFIX && jpeg.unsigned(offset + 1) in APP0..APP1) {
        val length = (jpeg.unsigned(offset + 2) shl 8) or jpeg.unsigned(offset + 3)
        offset += 2 + length
    }
    return ByteArrayOutputStream(jpeg.size + exifSegment.size).apply {
        write(jpeg, 0, 2)
        write(exifSegment)
        write(jpeg, offset, jpeg.size - offset)
    }.toByteArray()
}

private fun ByteArray.unsigned(index: Int): Int = this[index].toInt() and 0xFF

private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
