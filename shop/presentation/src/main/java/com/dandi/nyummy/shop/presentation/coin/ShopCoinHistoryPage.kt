package com.dandi.nyummy.shop.presentation.coin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRowTrailing
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPose
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPoseImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateAction
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateSurface
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyLoadMoreEffect
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.JankScrollWatcher
import com.dandi.nyummy.shop.entity.CoinHistoryVO
import com.dandi.nyummy.shop.presentation.R
import com.dandi.nyummy.shop.presentation.component.ShopTopBar
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.dandi.nyummy.common.presentation.R as CommonR

/** 코인 내역 화면(피그마 `Shop · 40`·`40b`). 내 코인 카드 아래에 날짜별 내역, 비어 있으면 코인 모으는 법. */
@Composable
fun ShopCoinHistoryPage(viewModel: ShopCoinHistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 처음 들어올 때와 다시 보일 때(식사 기록에서 돌아옴 등) 잔액과 내역을 새로 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(ShopCoinHistoryIntent.Enter)
        onPauseOrDispose { }
    }
    val listState = rememberLazyListState()
    JankScrollWatcher(listState)
    ShopCoinHistoryScreen(uiState = uiState, onIntent = viewModel::onIntent, listState = listState)
}

@Composable
private fun ShopCoinHistoryScreen(
    uiState: ShopCoinHistoryUIState,
    onIntent: (ShopCoinHistoryIntent) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        ShopTopBar(title = stringResource(R.string.shop_coin_title), onBack = { onIntent(ShopCoinHistoryIntent.ClickBack) })
        BalanceCard(
            balance = uiState.balance,
            modifier = Modifier.padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s16),
        )
        val bodyModifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        when {
            uiState.isLoadFailed -> Box(modifier = bodyModifier, contentAlignment = Alignment.Center) {
                NyummyStateSurface.Error(
                    voice = stringResource(R.string.shop_load_failed_voice),
                    message = stringResource(R.string.shop_load_failed_body),
                    retry = NyummyStateAction(stringResource(R.string.shop_retry)) { onIntent(ShopCoinHistoryIntent.Retry) },
                )
            }
            uiState.isLoading -> Box(modifier = bodyModifier, contentAlignment = Alignment.Center) {
                NyummySpinner(contentDescription = stringResource(R.string.shop_loading))
            }
            uiState.isEmpty -> EmptyHistory(onRecordMeal = { onIntent(ShopCoinHistoryIntent.ClickRecordMeal) }, modifier = bodyModifier)
            else -> HistoryList(uiState = uiState, onIntent = onIntent, listState = listState, modifier = bodyModifier)
        }
    }
}

/** "내 코인" 카드. */
@Composable
private fun BalanceCard(balance: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.actionNeutral, RoundedCornerShape(NyummyTheme.radius.m))
            .padding(NyummyTheme.spacing.s20),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        NyummyText(text = stringResource(R.string.shop_cart_balance), style = NyummyTheme.typography.labelS, color = NyummyTheme.colors.content.secondary)
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(CommonR.drawable.nyummy_asset_coin), contentDescription = null, modifier = Modifier.size(BalanceCoinSize))
            NyummyText(text = coins(balance), style = NyummyTheme.typography.numberXl)
        }
    }
}

@Composable
private fun HistoryList(
    uiState: ShopCoinHistoryUIState,
    onIntent: (ShopCoinHistoryIntent) -> Unit,
    listState: LazyListState,
    modifier: Modifier,
) {
    NyummyLoadMoreEffect(listState = listState, itemCount = uiState.entries.size) { onIntent(ShopCoinHistoryIntent.LoadMore) }
    val locale = LocalConfiguration.current.locales[0]
    val datePattern = stringResource(R.string.shop_coin_date_pattern)
    val dateFormatter = remember(datePattern, locale) { DateTimeFormatter.ofPattern(datePattern, locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern(TimePattern, locale) }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s8),
    ) {
        items(uiState.entries, key = { entry ->
            when (entry) {
                is CoinHistoryEntry.DateHeader -> "date-${entry.date}"
                is CoinHistoryEntry.Row -> "row-${entry.history.coinHistoryId}"
            }
        }) { entry ->
            when (entry) {
                is CoinHistoryEntry.DateHeader -> NyummyText(
                    text = entry.date.format(dateFormatter),
                    style = NyummyTheme.typography.labelS,
                    color = NyummyTheme.colors.content.tertiary,
                    modifier = Modifier.padding(top = NyummyTheme.spacing.s16, bottom = NyummyTheme.spacing.s4),
                )
                is CoinHistoryEntry.Row -> HistoryRow(history = entry.history, time = entry.history.createdAt.format(timeFormatter))
            }
        }
        if (uiState.isLoadingMore) {
            item(key = "loading-more") {
                Box(modifier = Modifier.fillMaxWidth().padding(NyummyTheme.spacing.s16), contentAlignment = Alignment.Center) {
                    NyummySpinner(contentDescription = stringResource(R.string.shop_loading))
                }
            }
        }
    }
}

/** 내역 한 줄(피그마 `v2 / Shop / Coin History Row`). 얻은 코인은 초록 "+n", 쓴 코인은 기본색 "−n". */
@Composable
private fun HistoryRow(history: CoinHistoryVO, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NyummyTheme.spacing.s12),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2)) {
            NyummyText(text = history.title, style = NyummyTheme.typography.bodyM, maxLines = 1)
            NyummyText(text = time, style = NyummyTheme.typography.bodyS, color = NyummyTheme.colors.content.tertiary)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2)) {
            val earned = history.quantity > 0
            NyummyText(
                text = signedCoins(history.quantity),
                style = NyummyTheme.typography.numberS,
                color = if (earned) NyummyTheme.colors.content.brand else NyummyTheme.colors.content.primary,
            )
            NyummyText(
                text = stringResource(R.string.shop_coin_balance_after, coins(history.balanceAfter)),
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
            )
        }
    }
}

/** 빈 내역(피그마 `Shop · 40b`): 안내, 코인 모으는 법, "식사 기록하기". */
@Composable
private fun EmptyHistory(onRecordMeal: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 큰 글자·작은 화면에서 안내가 길어지면 안내만 스크롤하고 "식사 기록하기"는 늘 아래에 보이게 둔다.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NyummyPoseImage(pose = NyummyPose.Hello, size = NyummyTheme.size.characterM)
            NyummyText(
                text = stringResource(R.string.shop_coin_empty_title),
                style = NyummyTheme.typography.titleM,
                modifier = Modifier.padding(top = NyummyTheme.spacing.s16),
            )
            NyummyText(
                text = stringResource(R.string.shop_coin_empty_body),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.secondary,
                modifier = Modifier.padding(top = NyummyTheme.spacing.s8),
            )
            Column(
                modifier = Modifier
                    .padding(top = NyummyTheme.spacing.s24)
                    .fillMaxWidth()
                    .background(NyummyTheme.colors.bg.surfaceSubtle, RoundedCornerShape(NyummyTheme.radius.m))
                    .padding(vertical = NyummyTheme.spacing.s8),
            ) {
                NyummyText(
                    text = stringResource(R.string.shop_coin_how_to_title),
                    style = NyummyTheme.typography.labelM,
                    modifier = Modifier.padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s8),
                )
                NyummyListRow(
                    title = stringResource(R.string.shop_coin_how_to_meal),
                    subtitle = stringResource(R.string.shop_coin_how_to_meal_body),
                    leadingIcon = CommonR.drawable.nyummy_ic_camera,
                    trailing = NyummyListRowTrailing.None,
                )
            }
        }
        NyummyButton(
            text = stringResource(R.string.shop_purchase_record_meal),
            onClick = onRecordMeal,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun coins(value: Int): String = NumberFormat.getNumberInstance(Locale.KOREA).format(value)

/** "+120", "−1,620"(마이너스 기호). */
private fun signedCoins(value: Int): String = when {
    value > 0 -> "+${coins(value)}"
    value < 0 -> "−${coins(-value)}"
    else -> coins(0)
}

private const val TimePattern = "a h:mm"
private val BalanceCoinSize = 28.dp
