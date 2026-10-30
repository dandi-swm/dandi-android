package com.dandi.nyummy.cat.entity

/**
 * 대사 은행의 대사 한 줄.
 *
 * @property text 대사. 고양이 이름 자리는 `{name}`이고, 화면이 사용자가 지어 준 이름으로 바꾼다.
 * @property tags 이 대사를 쓸 상황(morning, day, night, tap, greet, welcome_back, after_meal)
 */
data class CatLineVO(
    val text: String,
    val tags: Set<String>,
)
