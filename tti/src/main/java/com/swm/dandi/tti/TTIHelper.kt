package com.swm.dandi.tti

interface TTIHelper {
    fun startTTITracking(page: TTIPage)
    fun startTTITimeline(page: TTIPage, timelineCategory: TimelineCategory)
    fun endTTITimeline(page: TTIPage, timelineCategory: TimelineCategory)
    fun endTTITracking(page: TTIPage)
    fun shotTTILogging(page: TTIPage)
    fun addTTIMetaData(page: TTIPage, metadata: TTIMetaData, value: Any?)
}
