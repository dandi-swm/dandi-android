package com.dandi.nyummy.meal.entity

/**
 * 식사 사진을 어디서 가져왔는지. 업로드 전 EXIF 처리가 다르다.
 *
 * - [CAMERA]: 앱에서 방금 찍었다. EXIF 촬영 시각을 쓰지 않는 기기면 앱이 채워 넣는다.
 * - [GALLERY]: 갤러리에서 첨부했다. EXIF가 없으면 없는 그대로 보낸다.
 *
 * 어느 쪽이든 크기 상한을 넘어 다시 압축할 때 원본 EXIF는 그대로 남긴다.
 */
enum class MealPhotoSource {
    CAMERA,
    GALLERY,
}
