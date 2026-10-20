package com.dandi.nyummy.mailbox.presentation.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.image.NyummyCdn
import com.dandi.nyummy.mailbox.presentation.list.BoxOpeningFailure
import com.dandi.nyummy.mailbox.presentation.list.BoxOpeningPhase
import com.dandi.nyummy.mailbox.presentation.list.BoxOpeningUiState
import com.dandi.nyummy.mailbox.presentation.list.BoxRewardUiModel
import com.dandi.nyummy.mailbox.presentation.list.MailboxIntent
import com.dandi.nyummy.mailbox.presentation.list.RewardBoxOpeningDialog
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay

/**
 * 서버 없이 우편함 상자 열기 연출만 재생해 보는 디버그 액티비티 (debug 빌드에만 포함).
 *
 * 상자 번호, 응답 지연, 실패, 받은 코인과 아이템 수를 인텐트 값으로 바꿔 가며 볼 수 있다. 연출이 끝나면 닫힌다.
 *
 * 실행 예:
 * adb shell am start -n com.dandi.nyummy/com.dandi.nyummy.mailbox.presentation.debug.BoxOpeningDebugActivity \
 *   --el box 3 --el delay 1500 --es fail none --ei coin 100 --ei items 2
 *
 * - box: 상자 번호(1 브론즈, 2 실버, 3 골드)
 * - delay: 응답이 오기까지(ms). -1이면 응답 없이 기다림 장면에 머문다.
 * - fail: none, gone(이미 받았거나 기한 지남), error(받지 못함)
 * - coin: 받은 코인(0이면 코인 없음), items: 받은 아이템 수(0~3)
 */
class BoxOpeningDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val boxId = intent.getLongExtra(EXTRA_BOX, 1L)
        val responseDelay = intent.getLongExtra(EXTRA_DELAY, DEFAULT_DELAY_MILLIS)
        val fail = intent.getStringExtra(EXTRA_FAIL) ?: FAIL_NONE
        val coin = intent.getIntExtra(EXTRA_COIN, DEFAULT_COIN)
        val itemCount = intent.getIntExtra(EXTRA_ITEMS, 0).coerceIn(0, SAMPLE_ITEMS.size)

        setContent {
            NyummyTheme {
                var opening by remember {
                    mutableStateOf<BoxOpeningUiState?>(
                        BoxOpeningUiState(
                            mailId = boxId,
                            closedImageUrl = NyummyCdn.rewardBoxClosedUrl(boxId),
                            openedImageUrl = NyummyCdn.rewardBoxOpenedUrl(boxId),
                            mailTitle = SAMPLE_TITLE,
                        ),
                    )
                }
                LaunchedEffect(Unit) {
                    if (responseDelay < 0L) return@LaunchedEffect
                    delay(responseDelay)
                    opening = opening?.let { current ->
                        when (fail) {
                            FAIL_GONE -> current.copy(phase = BoxOpeningPhase.Failed, failure = BoxOpeningFailure.AlreadyGone)
                            FAIL_ERROR -> current.copy(phase = BoxOpeningPhase.Failed, failure = BoxOpeningFailure.Error)
                            else -> current.copy(phase = BoxOpeningPhase.Opening, rewards = sampleRewards(coin, itemCount))
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(NyummyTheme.colors.bg.canvas),
                )
                opening?.let { current ->
                    key(current.mailId) {
                        RewardBoxOpeningDialog(opening = current) { intent ->
                            when (intent) {
                                MailboxIntent.SkipBoxOpening,
                                MailboxIntent.BoxOpeningBurstFinished,
                                -> if (current.phase == BoxOpeningPhase.Opening) {
                                    opening = current.copy(phase = BoxOpeningPhase.Revealed)
                                }
                                MailboxIntent.BoxOpeningFailureShown,
                                MailboxIntent.CloseBoxOpening,
                                -> {
                                    opening = null
                                    finish()
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sampleRewards(coin: Int, itemCount: Int) = buildList {
        if (coin > 0) add(BoxRewardUiModel.Coin(imageUrl = NyummyCdn.coinUrl(1L), quantity = coin))
        SAMPLE_ITEMS.take(itemCount).forEach { (itemId, name) ->
            add(BoxRewardUiModel.Item(imageUrl = NyummyCdn.itemUrl(itemId), name = name, quantity = 1))
        }
    }.toImmutableList()

    private companion object {
        const val EXTRA_BOX = "box"
        const val EXTRA_DELAY = "delay"
        const val EXTRA_FAIL = "fail"
        const val EXTRA_COIN = "coin"
        const val EXTRA_ITEMS = "items"
        const val FAIL_NONE = "none"
        const val FAIL_GONE = "gone"
        const val FAIL_ERROR = "error"
        const val DEFAULT_DELAY_MILLIS = 1_500L
        const val DEFAULT_COIN = 100
        const val SAMPLE_TITLE = "식사 기록 상자"
        val SAMPLE_ITEMS = listOf(2L to "쿠션 침대", 3L to "캣타워", 1L to "기본 방")
    }
}
