package com.dandi.nyummy.meal.presentation.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.image.NyummyCdn
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import com.dandi.nyummy.meal.presentation.MealCameraPhase
import com.dandi.nyummy.meal.presentation.MealCeremonyStage
import com.dandi.nyummy.meal.presentation.MealSubmitStatus
import com.dandi.nyummy.meal.presentation.accepted
import com.dandi.nyummy.meal.presentation.component.ceremony.MealCeremony
import com.dandi.nyummy.meal.presentation.isAccepted
import com.dandi.nyummy.meal.presentation.nextStage
import com.dandi.nyummy.meal.presentation.rewardBoxGrade
import com.dandi.nyummy.meal.presentation.sendsRewardBox
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 서버 없이 식사 기록 연출만 재생해 보는 디버그 액티비티 (debug 빌드에만 포함).
 *
 * 응답 지연, 상자 등급, 우편함 연출 여부, 실패, 고양이방 배경(CDN 아이템 id)을 인텐트 값으로 바꿔 가며 볼 수 있다.
 * 실제 화면처럼 연출 상태를 ViewModel 에 두므로, 재생 중 다크 모드를 바꾸는 식으로 화면을 다시 만들어 이어지는지도 볼 수 있다.
 *
 * 실행 예:
 * adb shell am start -n com.dandi.nyummy/com.dandi.nyummy.meal.presentation.debug.MealCeremonyDebugActivity \
 *   --es grade GOLD --ez gift true --el delay 2500 --ez fail false --el bg 1
 */
class MealCeremonyDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val grade = RewardBoxGrade.from(intent.getStringExtra(EXTRA_GRADE) ?: RewardBoxGrade.GOLD.name)
        val sendsGift = intent.getBooleanExtra(EXTRA_GIFT, true)
        val responseDelay = intent.getLongExtra(EXTRA_DELAY, DEFAULT_DELAY_MILLIS)
        val fails = intent.getBooleanExtra(EXTRA_FAIL, false)
        val roomBackgroundUrl = intent.getLongExtra(EXTRA_BACKGROUND, 0L).takeIf { it > 0L }?.let(NyummyCdn::itemUrl)
        val viewModel = ViewModelProvider(this)[CeremonyDebugViewModel::class.java]
        viewModel.start(copySamplePhoto(), grade, responseDelay, fails)

        setContent {
            NyummyTheme {
                val ceremony = viewModel.ceremony
                // 실제 화면처럼 기록하지 못하면 곧장 닫는다.
                if (ceremony.submit == MealSubmitStatus.Failed) {
                    LaunchedEffect(Unit) { finish() }
                }
                MealCeremony(
                    photoPath = ceremony.photoPath,
                    stage = ceremony.stage,
                    rewardBoxGrade = ceremony.rewardBoxGrade,
                    sendsRewardBox = ceremony.sendsRewardBox(sendsGift),
                    roomBackgroundUrl = roomBackgroundUrl,
                    onStageFinished = { if (!viewModel.advance(it, sendsGift)) finish() },
                    onSkipClick = { if (!viewModel.skip(sendsGift)) finish() },
                    onCloseClick = { if (ceremony.isAccepted) finish() },
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
        const val EXTRA_BACKGROUND = "bg"
        const val DEFAULT_DELAY_MILLIS = 2_500L
        const val SAMPLE_PHOTO = "meal_ceremony_sample.png"
    }
}

/** 실제 화면의 ViewModel 처럼 연출 상태를 들고, 같은 장면 규칙([nextStage])으로 넘긴다. */
internal class CeremonyDebugViewModel : ViewModel() {

    var ceremony by mutableStateOf(MealCameraPhase.Ceremony(photoPath = "", source = MealPhotoSource.CAMERA))
        private set

    private var started = false

    /** 가짜 응답은 처음 한 번만 기다린다. 화면이 다시 만들어지면 지금 상태를 이어서 쓴다. */
    fun start(photoPath: String, grade: RewardBoxGrade, delayMillis: Long, fails: Boolean) {
        if (started) return
        started = true
        ceremony = ceremony.copy(photoPath = photoPath)
        viewModelScope.launch {
            delay(delayMillis)
            ceremony = if (fails) ceremony.copy(submit = MealSubmitStatus.Failed) else ceremony.accepted(grade)
        }
    }

    /** 장면 [finished] 가 끝나면 다음 장면으로 넘긴다. 연출을 마칠 차례면 false 다. */
    fun advance(finished: MealCeremonyStage, isMailGiftEnabled: Boolean): Boolean {
        if (ceremony.stage != finished) return true
        val next = ceremony.nextStage(isMailGiftEnabled) ?: return false
        ceremony = ceremony.copy(stage = next)
        return true
    }

    /** 띠욧 컷신만 건너뛴다. 연출을 마칠 차례면 false 다. */
    fun skip(isMailGiftEnabled: Boolean): Boolean =
        !ceremony.stage.isSkippable || advance(ceremony.stage, isMailGiftEnabled)
}
