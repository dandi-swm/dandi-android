package com.dandi.nyummy.settings.data.image

import org.junit.Assert.assertEquals
import org.junit.Test

class InquiryImageScaleTest {

    @Test
    fun `긴 변이 상한 이하인 스크린샷은 줄이지 않는다`() {
        assertEquals(1, inquiryImageSampleSize(1080, 2400))
        assertEquals(1080 to 2400, inquiryImageTargetSize(1080, 2400))
    }

    @Test
    fun `긴 변이 상한을 넘으면 비율을 지켜 긴 변을 상한에 맞춘다`() {
        assertEquals(2560 to 1920, inquiryImageTargetSize(4000, 3000))
        assertEquals(1920 to 2560, inquiryImageTargetSize(3000, 4000))
    }

    @Test
    fun `디코드 샘플은 줄인 뒤에도 긴 변이 상한 이상으로 남는 가장 큰 2의 거듭제곱이다`() {
        assertEquals(1, inquiryImageSampleSize(4000, 3000))
        assertEquals(2, inquiryImageSampleSize(5120, 3840))
        assertEquals(4, inquiryImageSampleSize(9000, 12000))
    }

    @Test
    fun `아주 가는 사진도 짧은 변이 0이 되지 않는다`() {
        assertEquals(2560 to 1, inquiryImageTargetSize(10000, 1))
    }
}
