package com.dandi.nyummy.common.presentation.designsystem.component

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.fetch.Fetcher
import coil3.request.Options
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import coil3.Uri as CoilUri

private const val PendingScheme = "pending"

@RunWith(AndroidJUnit4::class)
class NyummySpriteAnimationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val frame = NyummySpriteFrame(width = 136, height = 136, framesPerRow = 4, durationMs = 100)

    /** `pending://` 주소는 영원히 받는 중으로 두는 이미지 로더. 네트워크 상태와 상관없이 "받는 중"을 재현한다. */
    @Before
    fun setUpImageLoader() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(context)
                .components { add(PendingFetcherFactory()) }
                .build(),
        )
    }

    @After
    fun resetImageLoader() {
        SingletonImageLoader.reset()
    }

    private class PendingFetcherFactory : Fetcher.Factory<CoilUri> {
        override fun create(data: CoilUri, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (data.scheme == PendingScheme) Fetcher { awaitCancellation() } else null
    }

    private fun clip(frames: Int, loop: Boolean = false) = NyummySpriteClip(url = "https://cdn/test.png", frames = frames, loop = loop)

    private fun images(count: Int): ImmutableList<ImageBitmap> =
        persistentListOf<ImageBitmap>().builder().apply { repeat(count) { add(ImageBitmap(136 * 4, 136 * 2)) } }.build()

    /** [clips]를 재생하고 onFinished가 불린 횟수를 돌려주는 함수를 준다. 시계는 테스트가 직접 움직인다. */
    private fun play(clips: ImmutableList<NyummySpriteClip>, restMillis: Long): () -> Int {
        var finished = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NyummyTheme {
                NyummySpriteAnimation(
                    images = images(clips.size),
                    clips = clips,
                    frame = frame,
                    restMillis = restMillis,
                    onFinished = { finished++ },
                    contentDescription = "냐미",
                    modifier = Modifier.size(136.dp),
                )
            }
        }
        return { finished }
    }

    @Test
    fun 중간의_되풀이_구간은_세_번_돌고_쉬는_시간이_지나면_끝났다고_알린다() {
        // 8프레임 한 번(800) + 되풀이 8프레임 세 번(2400) + 8프레임 한 번(800) + 쉬기(1000) = 5000ms
        val finished = play(persistentListOf(clip(8), clip(8, loop = true), clip(8)), restMillis = 1_000L)

        composeRule.mainClock.advanceTimeBy(4_800L)
        assertEquals(0, finished())

        composeRule.mainClock.advanceTimeBy(400L)
        assertEquals(1, finished())

        // 다음 동작은 부른 쪽이 정하므로 혼자 다시 끝나지 않는다.
        composeRule.mainClock.advanceTimeBy(10_000L)
        assertEquals(1, finished())
    }

    @Test
    fun 마지막_클립이_되풀이_구간이면_쉬는_동안_한_바퀴씩_이어서_돈다() {
        // 되풀이 8프레임 한 번(800) 뒤, 쉬는 2000ms 동안 한 바퀴(800)씩 세 바퀴(2400) = 3200ms
        val finished = play(persistentListOf(clip(8, loop = true)), restMillis = 2_000L)

        composeRule.mainClock.advanceTimeBy(3_000L)
        assertEquals(0, finished())

        composeRule.mainClock.advanceTimeBy(400L)
        assertEquals(1, finished())
    }

    @Test
    fun 설명을_전달한다() {
        play(persistentListOf(clip(8)), restMillis = 1_000L)

        composeRule.onNodeWithContentDescription("냐미").assertIsDisplayed()
    }

    @Test
    fun 재생_번호가_바뀌면_같은_클립도_처음부터_다시_재생한다() {
        var finished = 0
        var playId by mutableIntStateOf(0)
        val clips = persistentListOf(clip(8))
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NyummyTheme {
                NyummySpriteAnimation(
                    images = images(1),
                    clips = clips,
                    frame = frame,
                    restMillis = 1_000L,
                    onFinished = { finished++ },
                    playId = playId,
                    modifier = Modifier.size(136.dp),
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(1_000L)
        playId = 1
        composeRule.mainClock.advanceTimeBy(1_000L)
        assertEquals(0, finished)

        composeRule.mainClock.advanceTimeBy(1_000L)
        assertEquals(1, finished)
    }

    @Test
    fun 쉬는_시간이_바뀌면_처음부터_다시_재생한다() {
        var finished = 0
        var restMillis by mutableStateOf(1_000L)
        val clips = persistentListOf(clip(8))
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NyummyTheme {
                NyummySpriteAnimation(
                    images = images(1),
                    clips = clips,
                    frame = frame,
                    restMillis = restMillis,
                    onFinished = { finished++ },
                    modifier = Modifier.size(136.dp),
                )
            }
        }

        // 처음 값대로라면 1800ms에 끝난다. 1000ms에 바꾸면 그때부터 800 + 2000ms를 다시 센다.
        composeRule.mainClock.advanceTimeBy(1_000L)
        restMillis = 2_000L
        composeRule.mainClock.advanceTimeBy(1_000L)
        assertEquals(0, finished)

        composeRule.mainClock.advanceTimeBy(2_000L)
        assertEquals(1, finished)
    }

    @Test
    fun 다른_동작의_시트를_받는_동안_직전_프레임을_그대로_보여준다() {
        // 앱 캐시에 시트 한 장을 만들어 두고, 다음 동작은 끝나지 않는 주소로 둔다.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sheet = File(context.cacheDir, "sprite-test.png")
        sheet.outputStream().use { out ->
            Bitmap.createBitmap(136 * 4, 136 * 2, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val loaded = persistentListOf(NyummySpriteClip(url = Uri.fromFile(sheet).toString(), frames = 8))
        val pending = persistentListOf(NyummySpriteClip(url = "$PendingScheme://never.png", frames = 8))
        var clips by mutableStateOf(loaded)
        composeRule.setContent {
            NyummyTheme {
                NyummySpriteAnimation(
                    clips = clips,
                    frame = frame,
                    restMillis = 60_000L,
                    onFinished = {},
                    contentDescription = "냐미",
                    modifier = Modifier.size(136.dp),
                    placeholder = { Box(Modifier.size(10.dp).testTag("placeholder")) },
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(hasContentDescription("냐미")).fetchSemanticsNodes().isNotEmpty()
        }

        // 첫 시트가 몇 프레임 그려지게 한다.
        composeRule.mainClock.advanceTimeBy(300L)
        clips = pending
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("냐미").assertIsDisplayed()
        composeRule.onNodeWithTag("placeholder").assertDoesNotExist()
    }

    @Test
    fun 처음_받는_동안에는_자리만_잡아_둔다() {
        composeRule.setContent {
            NyummyTheme {
                NyummySpriteAnimation(
                    clips = persistentListOf(NyummySpriteClip(url = "$PendingScheme://never.png", frames = 8)),
                    frame = frame,
                    restMillis = 1_000L,
                    onFinished = {},
                    contentDescription = "냐미",
                    modifier = Modifier.size(136.dp),
                    placeholder = { Box(Modifier.size(10.dp).testTag("placeholder")) },
                )
            }
        }

        composeRule.onNodeWithTag("placeholder").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("냐미").assertDoesNotExist()
    }

    @Test
    fun 시트를_받지_못하면_오류_자리를_보여준다() {
        composeRule.setContent {
            NyummyTheme {
                NyummySpriteAnimation(
                    clips = persistentListOf(NyummySpriteClip(url = "file:///nyummy/not-found.png", frames = 8)),
                    frame = frame,
                    restMillis = 1_000L,
                    onFinished = {},
                    modifier = Modifier.size(136.dp),
                    error = { Box(Modifier.size(10.dp).testTag("error")) },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(hasTestTag("error")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("error").assertIsDisplayed()
    }
}
