package com.dandi.nyummy.meal.presentation.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.image.NyummyCdn
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import com.dandi.nyummy.meal.presentation.MealCeremonyResult
import com.dandi.nyummy.meal.presentation.component.ceremony.MealCeremony
import java.io.File
import kotlinx.coroutines.delay

/**
 * 서버 없이 식사 기록 연출만 재생해 보는 디버그 액티비티 (debug 빌드에만 포함).
 *
 * 응답 지연, 상자 등급, 우편함 연출 여부, 실패, 고양이방 배경(CDN 아이템 id)을 인텐트 값으로 바꿔 가며 볼 수 있다.
 *
 * 실행 예:
 * adb shell am start -n com.dandi.nyummy/com.dandi.nyummy.meal.presentation.debug.MealCeremonyDebugActivity \
 *   --es grade GOLD --ez gift true --el delay 2500 --ez fail false --ez pending false --el bg 1
 */
class MealCeremonyDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val grade = RewardBoxGrade.from(intent.getStringExtra(EXTRA_GRADE) ?: RewardBoxGrade.GOLD.name)
        val sendsGift = intent.getBooleanExtra(EXTRA_GIFT, true)
        val responseDelay = intent.getLongExtra(EXTRA_DELAY, DEFAULT_DELAY_MILLIS)
        val fails = intent.getBooleanExtra(EXTRA_FAIL, false)
        val pending = intent.getBooleanExtra(EXTRA_PENDING, false)
        val roomBackgroundUrl = intent.getLongExtra(EXTRA_BACKGROUND, 0L).takeIf { it > 0L }?.let(NyummyCdn::itemUrl)
        val photoPath = copySamplePhoto()

        setContent {
            NyummyTheme {
                var result by remember { mutableStateOf<MealCeremonyResult?>(null) }
                var failed by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(responseDelay)
                    if (fails) {
                        failed = true
                    } else {
                        result = MealCeremonyResult(rewardBoxGrade = grade, isAnalysisPending = pending)
                    }
                }
                MealCeremony(
                    photoPath = photoPath,
                    result = result,
                    isFailed = failed,
                    isMailGiftEnabled = sendsGift,
                    roomBackgroundUrl = roomBackgroundUrl,
                    onCloseClick = { if (result != null) finish() },
                    onFailureShown = { finish() },
                    onDoneClick = { finish() },
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                )
            }
        }
    }

    /** 디버그 에셋의 견본 음식 사진을 캐시에 복사해 실제 촬영본처럼 파일 경로로 넘긴다. */
    private fun copySamplePhoto(): String {
        val file = File(cacheDir, SAMPLE_PHOTO)
        if (!file.exists()) {
            assets.open(SAMPLE_PHOTO).use { input -> file.outputStream().use { input.copyTo(it) } }
        }
        return file.absolutePath
    }

    private companion object {
        const val EXTRA_GRADE = "grade"
        const val EXTRA_GIFT = "gift"
        const val EXTRA_DELAY = "delay"
        const val EXTRA_FAIL = "fail"
        const val EXTRA_PENDING = "pending"
        const val EXTRA_BACKGROUND = "bg"
        const val DEFAULT_DELAY_MILLIS = 2_500L
        const val SAMPLE_PHOTO = "meal_ceremony_sample.png"
    }
}
