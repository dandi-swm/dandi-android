package com.dandi.nyummy.cat.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [CatRigView] UI 테스트: 실제 에셋을 읽어 그리고, 시간이 흐르면 그림이 바뀌고(애니메이션 재생), 동작이 끝나면 알리고,
 * 착용 아이템이 그 자리에 그려지는지 화면 픽셀로 확인한다.
 */
@RunWith(AndroidJUnit4::class)
class CatRigViewTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(character: CatRigCharacterVO, motion: CatRigMotion?, onFinished: (Int) -> Unit = {}, onShown: () -> Unit = {}) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            NyummyTheme {
                CatRigView(
                    character = character,
                    motion = motion,
                    onMotionFinished = onFinished,
                    onShown = onShown,
                    modifier = Modifier.size(272.dp).testTag(TAG),
                )
            }
        }
        // 그림은 IO 스레드에서 읽는다. 프레임을 진행하며 다 읽을 때까지 기다린다.
        var shown = false
        rule.waitUntil(LOAD_TIMEOUT) {
            rule.mainClock.advanceTimeBy(FRAME)
            shown = shown || opaquePixels(capture()) > 0
            shown
        }
    }

    private fun capture(): Bitmap = rule.onNodeWithTag(TAG).captureToImage().asAndroidBitmap()

    private fun opaquePixels(b: Bitmap): Int {
        var n = 0
        for (y in 0 until b.height step 4) for (x in 0 until b.width step 4) if ((b.getPixel(x, y) ushr 24) > 200) n++
        return n
    }

    private fun difference(a: Bitmap, b: Bitmap): Int {
        var n = 0
        for (y in 0 until a.height step 2) for (x in 0 until a.width step 2) if (a.getPixel(x, y) != b.getPixel(x, y)) n++
        return n
    }

    @Test
    fun 실제_에셋을_읽어_그리고_처음_보인_뒤_한_번_알린다() {
        var shownCount = 0
        show(RigCapture.character("nyami", "NORMAL"), motion = null, onShown = { shownCount++ })
        rule.mainClock.advanceTimeBy(500)
        assertTrue(opaquePixels(capture()) > MIN_CAT_PIXELS)
        assertEquals(1, shownCount)
    }

    @Test
    fun 시간이_흐르면_그림이_바뀐다_대기_중에도_숨쉬기() {
        show(RigCapture.character("nyami", "NORMAL"), motion = null)
        val a = capture()
        rule.mainClock.advanceTimeBy(1400) // 숨 주기 2.8초의 절반
        val b = capture()
        assertTrue("대기 중에도 숨쉬기로 그림이 바뀐다", difference(a, b) > MIN_CHANGED_PIXELS)
    }

    @Test
    fun 동작이_끝나면_그_재생_번호로_한_번_알린다() {
        val finished = mutableListOf<Int>()
        show(
            RigCapture.character("nyami", "NORMAL"),
            motion = CatRigMotion(motion = "pop", state = CatState.HOME_TAP, playId = 7, restMillis = 200),
            onFinished = { finished += it },
        )
        repeat(120) { rule.mainClock.advanceTimeBy(FRAME) } // 2초: pop 1.03초 + 쉬기 0.2초
        rule.waitForIdle()
        assertEquals(listOf(7), finished)
    }

    @Test
    fun 동작을_바꾸면_새_재생_번호로_다시_튼다() {
        val finished = mutableListOf<Int>()
        var motion by mutableStateOf(CatRigMotion("blink", CatState.HOME_DAY, playId = 1, restMillis = 0))
        rule.mainClock.autoAdvance = false
        rule.setContent {
            NyummyTheme {
                CatRigView(
                    character = RigCapture.character("nyami", "NORMAL"),
                    motion = motion,
                    onMotionFinished = { finished += it },
                    modifier = Modifier.size(200.dp),
                )
            }
        }
        repeat(30) { rule.mainClock.advanceTimeBy(FRAME) }
        motion = CatRigMotion("nod", CatState.HOME_DAY, playId = 2, restMillis = 0)
        repeat(150) { rule.mainClock.advanceTimeBy(FRAME) }
        rule.waitForIdle()
        // blink(0.9초)는 0.5초에 끊겨 알리지 않고, nod만 끝까지 가서 알린다
        assertEquals(listOf(2), finished)
    }

    @Test
    fun 모자를_쓰면_모자_자리에_모자_색이_그려진다() {
        show(RigCapture.character("nyami", "NORMAL", listOf("sample_beanie")), motion = null)
        rule.mainClock.advanceTimeBy(200)
        val withHat = capture()
        // 모자 없는 같은 고양이(같은 렌더러, 오프스크린)에는 그 자리에 빨간 픽셀이 없다
        val hatless = RigCapture.Rig(RigCapture.character("nyami", "NORMAL")).also { it.advance(0.2f) }.bitmap(withHat.width, background = 0)
        // 모자 자리: 정수리 소켓 위쪽(리그 좌표 x 40~88, y 16~34)
        assertTrue(redPixels(withHat, 40f, 16f, 88f, 34f) > MIN_HAT_PIXELS)
        assertEquals(0, redPixels(hatless, 40f, 16f, 88f, 34f))
    }

    private fun redPixels(b: Bitmap, x0: Float, y0: Float, x1: Float, y1: Float): Int {
        val s = b.width / RIG_SIZE
        var n = 0
        for (y in (y0 * s).toInt() until (y1 * s).toInt()) for (x in (x0 * s).toInt() until (x1 * s).toInt()) {
            val p = b.getPixel(x, y)
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            if ((p ushr 24) > 200 && r > 180 && g < 100) n++
        }
        return n
    }

    private companion object {
        const val TAG = "cat_rig"
        const val FRAME = 16L
        const val LOAD_TIMEOUT = 10_000L
        const val MIN_CAT_PIXELS = 500
        const val MIN_CHANGED_PIXELS = 50
        const val MIN_HAT_PIXELS = 200
    }
}
