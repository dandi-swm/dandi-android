package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.settings.entity.InquiryCategory
import com.dandi.nyummy.settings.entity.InquiryPageVO
import com.dandi.nyummy.settings.entity.InquiryVO

interface InquiryRepository {

    /** 내가 보낸 문의 목록 한 묶음(최신순). 첫 묶음은 [cursor] 없이, 다음부터는 직전 묶음의 nextCursor로 읽는다. */
    suspend fun getInquiries(cursor: Long?, size: Int): InquiryPageVO

    /** 문의 하나. 없으면 null. */
    suspend fun getInquiry(inquiryId: Long): InquiryVO?

    /**
     * 고른 사진(content URI)을 문의에 붙일 JPEG로 바꿔 앱 캐시에 두고 그 경로를 돌려준다.
     * 사진 정보(EXIF)는 남기지 않는다. 사진을 읽지 못하면 [InquiryImageLoadException]을 던진다.
     */
    suspend fun prepareInquiryImage(photoUri: String): String

    /** [imagePath]의 사진을 문의 첨부용으로 올리고, 문의를 보낼 때 넘길 이미지 키를 돌려준다. */
    suspend fun uploadInquiryImage(imagePath: String): String

    /** 문의를 보낸다. 사진이 없으면 [imageKey]는 null. 서버는 만든 문의를 돌려주지 않는다(본문 없는 201). */
    suspend fun sendInquiry(category: InquiryCategory, title: String, content: String, imageKey: String?)

    /** 문의를 문의 내역에서 지운다(서버는 소프트 삭제). */
    suspend fun deleteInquiry(inquiryId: Long)

    /** 지운 문의를 되살린다. */
    suspend fun restoreInquiry(inquiryId: Long)
}
