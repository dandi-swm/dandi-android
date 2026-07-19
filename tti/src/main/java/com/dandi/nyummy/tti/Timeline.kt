package com.dandi.nyummy.tti

data class Timeline(
    val startTime: Long = 0L,
    val endTime: Long = 0L,
) {
    fun isCompleted(): Boolean = startTime != 0L && endTime != 0L
}
