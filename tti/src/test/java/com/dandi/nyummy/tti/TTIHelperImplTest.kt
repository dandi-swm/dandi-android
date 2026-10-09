package com.dandi.nyummy.tti

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TTIHelperImplTest {

    private val reporter = RecordingReporter()
    private val logger = RecordingLogger()

    private fun TestScope.helper() = TTIHelperImpl(
        reporter = reporter,
        logger = logger,
        // 실제 앱의 @TtiDispatcher 처럼 한 줄에서 순서대로 실행된다.
        dispatcher = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun `시작 구간 끝 보고를 호출한 순서대로 기록하고 한 번 보고한다`() = runTest {
        val helper = helper()

        helper.startTTITracking(ApiPage)
        helper.startTTITimeline(TimelineCategory.API_RESPONSE_TIME)
        helper.endTTITimeline(TimelineCategory.API_RESPONSE_TIME)
        helper.endTTITracking()
        helper.shotTTILogging()
        runCurrent()

        val report = reporter.stops.single()
        assertEquals("api_page", report["tti.page_name"])
        assertEquals(false, report["tti.is_bounced"])
        assertEquals(false, report["tti.is_timeout"])
        assertTrue((report["tti.tti_time"] as Long) >= 0L)
        assertTrue((report["tti.api_response_time"] as Long) >= 0L)
        assertEquals(-1, report["tti.image_loaded_time"])
    }

    @Test
    fun `트래킹 시작 전 호출은 무시한다`() = runTest {
        val helper = helper()

        helper.startTTITimeline(TimelineCategory.API_RESPONSE_TIME)
        helper.endTTITracking()
        helper.shotTTILogging()
        runCurrent()

        assertTrue(reporter.stops.isEmpty())
    }

    @Test
    fun `두 번째 시작은 로그만 남기고 무시한다`() = runTest {
        val helper = helper()

        helper.startTTITracking(ApiPage)
        helper.startTTITracking(OtherPage)
        helper.shotTTILogging()
        runCurrent()

        assertEquals(1, reporter.starts.size)
        assertEquals("api_page", reporter.stops.single()["tti.page_name"])
        assertTrue(logger.messages.any { "called twice" in it })
    }

    @Test
    fun `마지막 구간이 끝나지 않았으면 TTI 끝을 받지 않고 미완료로 보고한다`() = runTest {
        val helper = helper()

        helper.startTTITracking(ApiPage)
        helper.startTTITimeline(TimelineCategory.API_RESPONSE_TIME)
        helper.endTTITracking()
        helper.shotTTILogging()
        runCurrent()

        val report = reporter.stops.single()
        assertEquals(-1, report["tti.tti_time"])
        assertEquals(true, report["tti.is_bounced"])
    }

    @Test
    fun `보고는 여러 번 불러도 한 번만 나간다`() = runTest {
        val helper = helper()

        helper.startTTITracking(ApiPage)
        helper.shotTTILogging()
        helper.shotTTILogging()
        runCurrent()
        helper.shotTTILogging()
        runCurrent()

        assertEquals(1, reporter.stops.size)
    }

    @Test
    fun `20초 안에 끝나지 않으면 is_timeout 으로 보고하고 이후 보고는 무시한다`() = runTest {
        val helper = helper()

        helper.startTTITracking(ApiPage)
        runCurrent()
        advanceTimeBy(TTIHelperImpl.TTI_TIMEOUT_MILLISECONDS + 1)
        helper.endTTITracking()
        helper.shotTTILogging()
        runCurrent()

        val report = reporter.stops.single()
        assertEquals(true, report["tti.is_timeout"])
        assertEquals(true, report["tti.is_bounced"])
    }

    @Test
    fun `끝을 찍은 측정은 20초가 지나도 타임아웃으로 보고하지 않는다`() = runTest {
        val helper = helper()

        helper.startTTITracking(ApiPage)
        helper.startTTITimeline(TimelineCategory.API_RESPONSE_TIME)
        helper.endTTITimeline(TimelineCategory.API_RESPONSE_TIME)
        helper.endTTITracking()
        runCurrent()
        advanceTimeBy(TTIHelperImpl.TTI_TIMEOUT_MILLISECONDS + 1)
        runCurrent()
        assertTrue(reporter.stops.isEmpty())

        helper.shotTTILogging()
        runCurrent()
        assertEquals(false, reporter.stops.single()["tti.is_timeout"])
    }

    @Test
    fun `같은 화면을 두 번 열면 인스턴스 번호가 다르다`() = runTest {
        val first = helper()
        val second = helper()

        first.startTTITracking(ApiPage)
        second.startTTITracking(ApiPage)
        first.shotTTILogging()
        second.shotTTILogging()
        runCurrent()

        val numbers = reporter.stops.map { it["tti.instance_no"] as Long }
        assertEquals(numbers[0] + 1, numbers[1])
    }

    @Test
    fun `보고 뒤에 넣은 메타데이터는 반영하지 않는다`() = runTest {
        val helper = helper()

        helper.startTTITracking(ApiPage)
        helper.addTTIMetaData(TTIMetaData.USER_WAIT_INCLUDED, true)
        helper.shotTTILogging()
        helper.addTTIMetaData(TTIMetaData.IS_BOUNCED, "late")
        runCurrent()

        val report = reporter.stops.single()
        assertEquals(true, report["tti.user_wait_included"])
        assertEquals(false, report["tti.is_bounced"] == "late")
    }

    @Test
    fun `외부 전송이 실패해도 측정은 이어진다`() = runTest {
        val helper = TTIHelperImpl(
            reporter = FailingReporter,
            logger = logger,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        helper.startTTITracking(ApiPage)
        helper.startTTITimeline(TimelineCategory.API_RESPONSE_TIME)
        helper.endTTITimeline(TimelineCategory.API_RESPONSE_TIME)
        helper.endTTITracking()
        helper.shotTTILogging()
        runCurrent()

        assertTrue(logger.messages.any { it.startsWith("End TTI Tracking") })
        assertTrue(logger.messages.any { it.startsWith("Shot TTI Logging") && "tti.is_bounced=false" in it })
        assertTrue(logger.messages.any { it.startsWith("Report failed") })
    }

    private object FailingReporter : TTIReporter {
        override fun startView(key: String, name: String, attributes: Map<String, Any?>) =
            throw IllegalStateException("firebase down")

        override fun stopView(key: String, attributes: Map<String, Any?>) =
            throw IllegalStateException("firebase down")
    }

    private object ApiPage : TTIPage {
        override val pageName = "api_page"
        override val timelines = listOf(TimelineCategory.API_RESPONSE_TIME)
    }

    private object OtherPage : TTIPage {
        override val pageName = "other_page"
        override val timelines = listOf(TimelineCategory.API_RESPONSE_TIME)
    }

    private class RecordingReporter : TTIReporter {
        val starts = mutableListOf<String>()
        val stops = mutableListOf<Map<String, Any?>>()

        override fun startView(key: String, name: String, attributes: Map<String, Any?>) {
            starts += name
        }

        override fun stopView(key: String, attributes: Map<String, Any?>) {
            stops += attributes
        }
    }

    private class RecordingLogger : TTILogger {
        val messages = mutableListOf<String>()

        override fun d(tag: String, msg: String) {
            messages += msg
        }
    }
}
