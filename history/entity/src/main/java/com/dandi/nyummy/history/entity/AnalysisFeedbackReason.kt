package com.dandi.nyummy.history.entity

/**
 * AI 영양 분석 평가에서 아쉬웠던 점(하나만 고른다).
 *
 * 고른 이유는 [label] 그대로 평가 이유(reason)에 담겨 서버에서 모아 본다. 집계가 흔들리지 않도록 [label]은 화면 언어와
 * 상관없이 고정하고, 화면 문구는 presentation 문자열 리소스가 따로 정한다.
 */
enum class AnalysisFeedbackReason(val label: String) {
    WRONG_FOOD("음식을 잘못 알아봤어요"),
    WRONG_PORTION("양을 다르게 봤어요"),
    WRONG_NUTRITION("칼로리·영양이 이상해요"),
    MISSING_FOOD("빠진 음식이 있어요"),
}
