package com.swm.dandi.common.presentation.searchList

// 카카오 검색 API datetime: "2024-03-11T15:23:22.000+09:00" 형태(ISO 8601).
private val ISO_DATE = Regex("""^(\d{4})-(\d{2})-(\d{2})""")

/**
 * ISO 8601 문자열에서 날짜만 뽑아 "2024년 3월 11일" 형식으로 변환한다.
 */
fun formatKakaoDateTime(raw: String): String {
    val match = ISO_DATE.find(raw) ?: return raw
    val (year, month, day) = match.destructured
    return "${year}년 ${month.toInt()}월 ${day.toInt()}일"
}
