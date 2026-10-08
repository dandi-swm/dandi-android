package com.dandi.nyummy.common.presentation.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageLoadReportTest {

    @Test
    fun `주소 모양으로 이미지 종류를 나눈다`() {
        assertEquals(ImageKind.FOOD_ICON, ImageKind.of("https://cdn.nyummy.co.kr/icons/12.jpeg"))
        assertEquals(ImageKind.CAT_SPRITE, ImageKind.of("https://cdn.nyummy.co.kr/cats/normal/relaxed_1.png"))
        assertEquals(
            ImageKind.MEAL_PHOTO,
            ImageKind.of("https://bucket.s3.amazonaws.com/meals/1.jpg?X-Amz-Signature=abc&X-Amz-Expires=600"),
        )
        assertEquals(ImageKind.OTHER_REMOTE, ImageKind.of("https://example.com/banner.png"))
        assertEquals(ImageKind.LOCAL, ImageKind.of("file:///cache/meal_1.jpeg"))
        assertEquals(ImageKind.LOCAL, ImageKind.of(2131230800))
        assertEquals(ImageKind.LOCAL, ImageKind.of(null))
    }

    @Test
    fun `메모리 캐시 적중만 일부만 보낸다`() {
        assertTrue(RemoteImageLoadReport.shouldSend(ImageSource.NETWORK) { 0.99 })
        assertTrue(RemoteImageLoadReport.shouldSend(ImageSource.DISK) { 0.99 })
        assertTrue(RemoteImageLoadReport.shouldSend(null) { 0.99 })
        assertTrue(RemoteImageLoadReport.shouldSend(ImageSource.MEMORY_CACHE) { 0.05 })
        assertFalse(RemoteImageLoadReport.shouldSend(ImageSource.MEMORY_CACHE) { 0.5 })
    }
}
