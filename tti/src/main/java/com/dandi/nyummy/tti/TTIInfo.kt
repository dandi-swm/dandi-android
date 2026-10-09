package com.dandi.nyummy.tti

const val TTI_LOG_VERSION = 1
const val TTI_PREFIX = "tti."

class TTIInfo(
    private val page: TTIPage,
    val instanceNo: Long = 1L,
) {
    private val ttiTimelineMap = mutableMapOf<String, Timeline?>()
    var timeoutFlag = true
    var allTTIRecordedFlag = false
    var isSent = false

    // 20초 안에 끝나지 않아 타임아웃으로 보고됐는지. 결과의 is_timeout 으로 나간다.
    var isTimeout = false

    // 같은 페이지에 대한 넘버링을 추가하여 구분할 수 있도록 한다.
    val ttiKey: String = "${page.pageName}#${instanceNo}_${System.currentTimeMillis()}"
    private val additionalMetaData = mutableMapOf<String, Any?>()

    fun recordStartTime(ttiTime: TimelineCategory) {
        ttiTimelineMap[ttiTime.categoryName]?.let { timeline ->
            if (timeline.startTime == 0L) {
                ttiTimelineMap[ttiTime.categoryName] = timeline.copy(
                    startTime = currentMonotonicTimeInNano(),
                )
            }
        } ?: run {
            ttiTimelineMap[ttiTime.categoryName] = Timeline(
                startTime = currentMonotonicTimeInNano(),
            )
        }
    }

    fun recordEndTime(ttiTime: TimelineCategory) {
        ttiTimelineMap[ttiTime.categoryName]?.let { timeline ->
            if (timeline.endTime == 0L) {
                ttiTimelineMap[ttiTime.categoryName] = timeline.copy(
                    endTime = currentMonotonicTimeInNano(),
                )
            }
        } ?: run {
            ttiTimelineMap[ttiTime.categoryName] = Timeline(
                endTime = currentMonotonicTimeInNano(),
            )
        }
    }

    fun getTTIInfo(): Map<String, Any?> {
        val ttiInfo = mutableMapOf<String, Any?>()
        initTTIData(ttiInfo)
        updateTTITimeline(ttiInfo)
        ttiInfo.putAll(additionalMetaData)
        return ttiInfo
    }

    private fun initTTIData(ttiInfo: MutableMap<String, Any?>) {
        ttiInfo[TTI_PREFIX + TTIMetaData.PAGE_NAME.metadataName] = page.pageName
        ttiInfo[TTI_PREFIX + TTIMetaData.INSTANCE_NO.metadataName] = instanceNo
        ttiInfo[TTI_PREFIX + TTIMetaData.IS_BOUNCED.metadataName] = false
        ttiInfo[TTI_PREFIX + TTIMetaData.IS_TIMEOUT.metadataName] = isTimeout
        ttiInfo[TTI_PREFIX + TTIMetaData.TTI_LOG_VERSION.metadataName] = TTI_LOG_VERSION
        ttiInfo[TTI_PREFIX + TimelineCategory.TTI_TIME.categoryName] = -1
        ttiInfo[TTI_PREFIX + TimelineCategory.VIEW_CREATION_TIME.categoryName] = -1
        ttiInfo[TTI_PREFIX + TimelineCategory.API_REQUEST_READY_TIME.categoryName] = -1
        ttiInfo[TTI_PREFIX + TimelineCategory.API_RESPONSE_TIME.categoryName] = -1
        ttiInfo[TTI_PREFIX + TimelineCategory.VIEW_BINDING_TIME.categoryName] = -1
        ttiInfo[TTI_PREFIX + TimelineCategory.IMAGE_LOADED_TIME.categoryName] = -1
    }

    private fun updateTTITimeline(ttiInfo: MutableMap<String, Any?>) {
        // 개수로 세면 자동으로 들어가는 TTI_TIME 때문에 중간 구간이 빠져도 개수가 맞을 수 있다. 구간마다 기록됐는지 본다.
        ttiInfo[TTI_PREFIX + TTIMetaData.IS_BOUNCED.metadataName] =
            page.timelines.any { ttiTimelineMap[it.categoryName] == null }

        val timelineEntries = ttiTimelineMap.entries.toList()
        for ((timelineKey, timeline) in timelineEntries) {
            timeline?.let {
                val duration = it.endTime - it.startTime
                // 시작이나 끝 중 하나라도 없으면 구간 값을 믿을 수 없다(단조 시계는 기준점이 임의라 end - 0 도 의미가 없다).
                if (!it.isCompleted() || duration < 0L) {
                    ttiInfo[TTI_PREFIX + TTIMetaData.IS_BOUNCED.metadataName] = true
                    ttiInfo[TTI_PREFIX + timelineKey] = -1
                } else {
                    ttiInfo[TTI_PREFIX + timelineKey] = duration
                }
            }
        }
    }

    fun isCanRecordTimeout(): Boolean {
        return timeoutFlag && allTTIRecordedFlag.not()
    }

    fun cantEndTTITracking(): Boolean {
        return allTTIRecordedFlag || isRecordedLastTimeline().not()
    }

    private fun isRecordedLastTimeline(): Boolean {
        val lastTimelineForPage = page.timelines.last().categoryName
        ttiTimelineMap[lastTimelineForPage]?.let {
            return it.endTime != 0L && it.startTime != 0L
        }
        return false
    }

    fun addTTIMetaData(metadata: TTIMetaData, value: Any?) {
        additionalMetaData[TTI_PREFIX + metadata.metadataName] = value
    }
}

// 구간 길이(end - start)만 쓰므로 벽시계가 아닌 단조 시계를 쓴다.
// 측정 중에 기기 시각이 바뀌어도(자동 시간 동기화 등) 구간 값이 틀어지지 않는다.
private fun currentMonotonicTimeInNano(): Long = System.nanoTime()
