package com.dandi.nyummy.tti

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TTIHelperImpl(
    private val reporter: TTIReporter = NoOpTTIReporter,
    private val logger: TTILogger,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TTIHelper {
    private var pageTTIMap = mutableMapOf<String, TTIInfo>()
    private val scope = CoroutineScope(
        SupervisorJob() + dispatcher + CoroutineExceptionHandler { _, e ->
            logger.d(tag = "TTI", msg = "Uncaught exception: ${e.message}")
        }
    )

    companion object {
        const val TTI_TIMEOUT_MILLISECONDS = 20000L
    }

    override fun startTTITracking(page: TTIPage) {
        val ttiInfo = TTIInfo(page)
        pageTTIMap[page.pageName] = ttiInfo
        scope.launch {
            reporter.startView(ttiInfo.ttiKey, page.pageName, emptyMap())
            ttiInfo.recordStartTime(TimelineCategory.TTI_TIME)
            logger.d(
                tag = "TTI",
                msg = "Start TTI Tracking : ${ttiInfo.ttiKey} / ${page.pageName}",
            )
            doTimeoutTTI(ttiInfo)
        }
    }

    private suspend fun doTimeoutTTI(ttiInfo: TTIInfo) {
        delay(TTI_TIMEOUT_MILLISECONDS)
        if (ttiInfo.isCanRecordTimeout()) {
            ttiInfo.allTTIRecordedFlag = true
            val info = ttiInfo.getTTIInfo()
            reporter.stopView(key = ttiInfo.ttiKey, info)
            logger.d(tag = "TTI", msg = "Timeout TTI Tracking $info")
        }
    }

    override fun startTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) {
        scope.launch {
            pageTTIMap[page.pageName]?.let {
                if (it.allTTIRecordedFlag) {
                    return@launch
                }
                it.recordStartTime(timelineCategory)
            }
        }
    }

    override fun endTTITimeline(page: TTIPage, timelineCategory: TimelineCategory) {
        scope.launch {
            pageTTIMap[page.pageName]?.let {
                if (it.allTTIRecordedFlag) {
                    return@launch
                }
                it.recordEndTime(timelineCategory)
            }
        }
    }

    override fun endTTITracking(page: TTIPage) {
        scope.launch {
            pageTTIMap[page.pageName]?.let {
                if (it.cantEndTTITracking()) {
                    return@launch
                }
                it.timeoutFlag = false
                it.allTTIRecordedFlag = true
                it.recordEndTime(TimelineCategory.TTI_TIME)
                logger.d(tag = "TTI", msg = "End TTI Tracking : ${it.ttiKey}")
            }
        }
    }

    override fun shotTTILogging(page: TTIPage) {
        scope.launch {
            pageTTIMap[page.pageName]?.let {
                if (it.isSent) {
                    return@launch
                }

                it.isSent = true
                val info = it.getTTIInfo()
                reporter.stopView(key = it.ttiKey, info)
                logger.d(tag = "TTI", msg = "Shot TTI Logging : ${it.ttiKey} / $info")
            }
        }
    }

    override fun addTTIMetaData(page: TTIPage, metadata: TTIMetaData, value: Any?) {
        pageTTIMap[page.pageName]?.addTTIMetaData(metadata, value)
    }
}
