package com.dandi.nyummy.common.data.image

import kotlinx.serialization.Serializable

/**
 * 이미지를 어디에 쓰려고 올리는지. 서버는 용도별 경로에 업로드 URL을 발급하고, 각 도메인의 생성 API가 그 이미지 키를 확정한다.
 * 이름은 서버 `ImagePurpose`와 철자까지 같아야 한다.
 */
@Serializable
enum class ImageUploadPurpose {
    /** 식사 기록 사진 */
    MEAL,

    /** 문의 첨부 이미지 */
    INQUIRY,
}
