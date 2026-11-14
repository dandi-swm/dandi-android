package com.dandi.nyummy.shop.presentation.coin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialogType
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySheetTitle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateAction
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateSurface
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.image.NyummyCdn
import com.dandi.nyummy.shop.domain.CoinChargePackages
import com.dandi.nyummy.shop.entity.CoinChargeCurrency
import com.dandi.nyummy.shop.entity.CoinChargePackageVO
import com.dandi.nyummy.shop.presentation.R
import com.dandi.nyummy.shop.presentation.component.ShopTopBar
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * 코인 충전 화면(코인 충전 이벤트, 피그마 `v2 · 40 Shop`의 `Charge · 10~15`).
 * 내 코인 카드 아래 묶음 목록, 묶음을 누르면 충전 확인 시트, "결제하기" 뒤에는 이번 한 번만 무료 화면.
 * [source]는 링크의 `source` 인자(분석용)다.
 */
@Composable
fun ShopCoinChargePage(source: String?, viewModel: ShopCoinChargeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onIntent(ShopCoinChargeIntent.Enter(source)) }
    // 다시 보일 때(상점에서 돌아옴 등) 잔액을 새로 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(ShopCoinChargeIntent.Resume)
        onPauseOrDispose { }
    }
    ShopCoinChargeScreen(uiState = uiState, onIntent = viewModel::onIntent)
}

@Composable
private fun ShopCoinChargeScreen(uiState: ShopCoinChargeUIState, onIntent: (ShopCoinChargeIntent) -> Unit) {
    val grantedCoins = uiState.grantedCoins
    if (grantedCoins != null) {
        BackHandler { onIntent(ShopCoinChargeIntent.ClickBack) }
        CoinChargeDoneScreen(
            grantedCoins = grantedCoins,
            balance = uiState.balance,
            onGoShop = { onIntent(ShopCoinChargeIntent.ClickGoShop) },
            onGoHome = { onIntent(ShopCoinChargeIntent.ClickGoHome) },
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        ShopTopBar(title = stringResource(R.string.shop_charge_title), onBack = { onIntent(ShopCoinChargeIntent.ClickBack) })
        val bodyModifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        when {
            uiState.isLoading -> Box(modifier = bodyModifier, contentAlignment = Alignment.Center) {
                NyummySpinner(contentDescription = stringResource(R.string.shop_loading))
            }
            uiState.isEnded -> Box(modifier = bodyModifier, contentAlignment = Alignment.Center) {
                NyummyStateSurface.Empty(
                    voice = stringResource(R.string.shop_charge_ended_voice),
                    message = stringResource(R.string.shop_charge_ended_body),
                    action = NyummyStateAction(stringResource(R.string.shop_charge_ended_go_shop)) {
                        onIntent(ShopCoinChargeIntent.ClickGoShop)
                    },
                )
            }
            else -> PackageList(uiState = uiState, onIntent = onIntent, modifier = bodyModifier)
        }
    }
    uiState.sheetPackage?.let { pack ->
        CoinChargeConfirmSheet(
            pack = pack,
            balance = uiState.balance,
            isPaying = uiState.isPaying,
            onPay = { onIntent(ShopCoinChargeIntent.ClickPay) },
            onDismiss = { onIntent(ShopCoinChargeIntent.DismissSheet) },
        )
    }
    if (uiState.showsClaimedDialog) {
        NyummyDialog(
            title = stringResource(R.string.shop_charge_claimed_title),
            body = stringResource(R.string.shop_charge_claimed_body),
            confirmText = stringResource(R.string.shop_charge_claimed_confirm),
            type = NyummyDialogType.Alert,
            onConfirm = { onIntent(ShopCoinChargeIntent.DismissClaimedDialog) },
            onDismissRequest = { onIntent(ShopCoinChargeIntent.DismissClaimedDialog) },
        )
    }
}

/** 내 코인 카드, "충전할 코인을 골라 주세요", 묶음 줄, 안내 글(피그마 `Charge · 10`·`10b`). 높이가 모자라면 스크롤한다. */
@Composable
private fun PackageList(uiState: ShopCoinChargeUIState, onIntent: (ShopCoinChargeIntent) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s16),
    ) {
        ShopBalanceCard(balance = uiState.balance)
        NyummyText(
            text = stringResource(R.string.shop_charge_heading),
            style = NyummyTheme.typography.titleS,
            modifier = Modifier.padding(top = NyummyTheme.spacing.s32, bottom = NyummyTheme.spacing.s12),
        )
        Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12)) {
            uiState.packages.forEach { pack ->
                CoinPackageRow(pack = pack, onClick = { onIntent(ShopCoinChargeIntent.ClickPackage(pack.coinPackageId)) })
            }
        }
        NyummyText(
            text = stringResource(R.string.shop_charge_note),
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.tertiary,
            modifier = Modifier.padding(top = NyummyTheme.spacing.s20),
        )
    }
}

/** 묶음 한 줄(피그마 `v2 / Shop / Coin Package`): 그림, 코인 수, 가격 버튼. 줄 어디를 눌러도 시트가 뜬다. */
@Composable
private fun CoinPackageRow(pack: CoinChargePackageVO, onClick: () -> Unit) {
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NyummyTheme.colors.bg.surface)
            .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.subtle, shape)
            .nyummyClickable(onClick = onClick)
            .padding(start = NyummyTheme.spacing.s12, end = NyummyTheme.spacing.s16)
            .padding(vertical = NyummyTheme.spacing.s12),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinPackArt(imageId = pack.imageId, coins = pack.coins, tileSize = RowArtTileSize)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
        ) {
            NyummyText(text = number(pack.coins), style = NyummyTheme.typography.numberL, modifier = Modifier.alignByBaseline())
            NyummyText(
                text = stringResource(R.string.shop_charge_coin_unit),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.secondary,
                modifier = Modifier.alignByBaseline(),
            )
        }
        NyummyButton(
            text = chargePrice(pack),
            onClick = onClick,
            style = NyummyButtonStyle.Tonal,
            size = NyummyButtonSize.S,
        )
    }
}

/** 충전 확인 시트(피그마 `Charge · 11`). 결제할 금액, 충전 후 내 코인, 취소·"(가격) 결제하기". 실제 결제는 하지 않는다. */
@Composable
private fun CoinChargeConfirmSheet(
    pack: CoinChargePackageVO,
    balance: Int,
    isPaying: Boolean,
    onPay: () -> Unit,
    onDismiss: () -> Unit,
) {
    NyummyBottomSheet(onDismissRequest = onDismiss) {
        NyummySheetTitle(text = stringResource(R.string.shop_charge_sheet_title, number(pack.coins)))
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            CoinPackArt(imageId = pack.imageId, coins = pack.coins, tileSize = SheetArtTileSize)
            Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12)) {
                SheetRow(label = stringResource(R.string.shop_charge_sheet_price)) {
                    NyummyText(text = chargePrice(pack), style = NyummyTheme.typography.numberXs)
                }
                SheetRow(label = stringResource(R.string.shop_charge_sheet_balance_after)) {
                    CoinAmount(value = balance + pack.coins, iconSize = SheetCoinSize)
                }
            }
            NyummyText(
                text = stringResource(R.string.shop_charge_sheet_caption),
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
            NyummyButton(
                text = stringResource(R.string.shop_purchase_cancel),
                onClick = onDismiss,
                style = NyummyButtonStyle.Neutral,
                enabled = !isPaying,
                modifier = Modifier.weight(CancelWeight),
            )
            NyummyButton(
                text = stringResource(R.string.shop_charge_sheet_pay, chargePrice(pack)),
                onClick = onPay,
                enabled = !isPaying,
                modifier = Modifier.weight(PayWeight),
            )
        }
    }
}

/**
 * 받은 뒤 화면(피그마 `Charge · 12`). 결제가 되지 않았다는 것(돈이 나가지 않음)과 선물로 넣은 코인을 알리고,
 * 상점 둘러보기·홈으로를 둔다. 높이가 모자라면 내용만 스크롤한다.
 */
@Composable
private fun CoinChargeDoneScreen(grantedCoins: Int, balance: Int, onGoShop: () -> Unit, onGoHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas)
            .padding(horizontal = NyummyTheme.spacing.gutter, vertical = NyummyTheme.spacing.s24),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 큰 글자·분할 화면처럼 높이가 모자라면 내용만 스크롤하고 버튼은 아래에 둔다. 넉넉하면 남는 높이를 위 1 : 아래 2로 나눈다.
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                verticalArrangement = UpperThird,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CoinPackArt(imageId = DoneArtImageId, coins = grantedCoins, tileSize = DoneArtTileSize)
                Spacer(Modifier.height(NyummyTheme.spacing.s32))
                NyummyText(
                    text = stringResource(R.string.shop_charge_done_title),
                    style = NyummyTheme.typography.displayL,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(NyummyTheme.spacing.s12))
                GiftCoinAmount(coins = grantedCoins)
                Spacer(Modifier.height(NyummyTheme.spacing.s12))
                NyummyText(
                    text = stringResource(R.string.shop_charge_done_body),
                    style = NyummyTheme.typography.bodyL,
                    color = NyummyTheme.colors.content.secondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(NyummyTheme.spacing.s16))
                Row(
                    modifier = Modifier
                        .background(NyummyTheme.colors.bg.actionNeutral, RoundedCornerShape(NyummyTheme.radius.full))
                        .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s10),
                    horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NyummyText(
                        text = stringResource(R.string.shop_charge_done_balance),
                        style = NyummyTheme.typography.labelM,
                        color = NyummyTheme.colors.content.tertiary,
                    )
                    CoinAmount(value = balance, iconSize = PillCoinSize, style = NyummyTheme.typography.numberS)
                }
            }
        }
        NyummyButton(
            text = stringResource(R.string.shop_charge_done_go_shop),
            onClick = onGoShop,
            modifier = Modifier.fillMaxWidth(),
        )
        NyummyTextButton(
            text = stringResource(R.string.shop_charge_done_go_home),
            onClick = onGoHome,
            tone = NyummyTextButtonTone.Neutral,
            modifier = Modifier.padding(top = NyummyTheme.spacing.s8),
        )
    }
}

/**
 * 받은 선물 코인(피그마 `Charge · 12-2`의 `Gift Amount`). 연초록 바탕에 큰 코인 아이콘과 브랜드색 "+3,000 코인"을 둬
 * 받은 코인이 화면에서 가장 먼저 눈에 띄게 한다. 읽을 때는 한 덩어리("+3,000 코인")로 읽힌다.
 */
@Composable
private fun GiftCoinAmount(coins: Int) {
    Row(
        modifier = Modifier
            .background(NyummyTheme.colors.bg.brandSubtle, RoundedCornerShape(NyummyTheme.radius.full))
            .padding(horizontal = NyummyTheme.spacing.s20, vertical = NyummyTheme.spacing.s12)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painter = painterResource(CommonR.drawable.nyummy_asset_coin), contentDescription = null, modifier = Modifier.size(GiftCoinSize))
        NyummyText(
            text = stringResource(R.string.shop_charge_done_amount, number(coins)),
            style = NyummyTheme.typography.numberXl,
            color = NyummyTheme.colors.content.brand,
            maxLines = 1,
        )
        NyummyText(
            text = stringResource(R.string.shop_charge_coin_unit),
            style = NyummyTheme.typography.titleS,
            color = NyummyTheme.colors.content.brand,
            maxLines = 1,
        )
    }
}

/** 묶음 그림 칸(CDN `coins/{imageId}.png`). 받지 못하면 코인 아이콘을 그린다. */
@Composable
private fun CoinPackArt(imageId: Long, coins: Int, tileSize: Dp) {
    val fallback = painterResource(CommonR.drawable.nyummy_asset_coin)
    Box(
        modifier = Modifier
            .size(tileSize)
            .clip(RoundedCornerShape(NyummyTheme.radius.s))
            .background(NyummyTheme.colors.bg.surfaceSunken),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = NyummyCdn.coinUrl(imageId),
            contentDescription = stringResource(R.string.shop_charge_pack_description, number(coins)),
            placeholder = fallback,
            error = fallback,
            modifier = Modifier.size(tileSize * ArtImageFraction),
        )
    }
}

@Composable
private fun SheetRow(label: String, value: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NyummyText(
            text = label,
            style = NyummyTheme.typography.bodyM,
            color = NyummyTheme.colors.content.secondary,
            modifier = Modifier.weight(1f),
        )
        value()
    }
}

@Composable
private fun CoinAmount(value: Int, iconSize: Dp, style: TextStyle = NyummyTheme.typography.numberXs) {
    Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4), verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(CommonR.drawable.nyummy_asset_coin), contentDescription = null, modifier = Modifier.size(iconSize))
        NyummyText(text = number(value), style = style, maxLines = 1)
    }
}

/** 남는 높이를 위 1 : 아래 2로 나눠 내용을 화면 위쪽 1/3 지점에 둔다(구매 완료 화면과 같다). */
private val UpperThird = object : Arrangement.Vertical {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        var y = (totalSize - sizes.sum()).coerceAtLeast(0) / 3
        sizes.forEachIndexed { index, size ->
            outPositions[index] = y
            y += size
        }
    }
}

private fun number(value: Int): String = NumberFormat.getNumberInstance(Locale.KOREA).format(value)

/**
 * 묶음 가격을 기기 지역 통화로 보인다. 원화 기준 가격을 고정 환율 표로 바꾼 값이다([CoinChargePackages.priceOf]).
 * 실제 결제 전 A/B 테스트라 환율을 따라가지 않는다.
 */
@Composable
private fun chargePrice(pack: CoinChargePackageVO): String {
    val locale = LocalConfiguration.current.locales[0]
    val price = CoinChargePackages.priceOf(pack, CoinChargePackages.currencyOf(locale.country, locale.language))
    val digits = price.currency.fractionDigits
    val amount = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }.format(BigDecimal.valueOf(price.amount, digits))
    val format = when (price.currency) {
        CoinChargeCurrency.KRW -> R.string.shop_charge_price_krw
        CoinChargeCurrency.JPY -> R.string.shop_charge_price_jpy
        CoinChargeCurrency.USD -> R.string.shop_charge_price_usd
    }
    return stringResource(format, amount)
}

/** 받은 뒤 화면 그림. 이벤트 코인은 묶음과 상관없이 같아서 가장 큰 묶음 그림(바구니)을 쓴다. */
private const val DoneArtImageId = 3L
private const val ArtImageFraction = 0.875f
private const val CancelWeight = 1f
private const val PayWeight = 1.8f
private val RowArtTileSize = 64.dp
private val SheetArtTileSize = 64.dp
private val DoneArtTileSize = 120.dp
private val SheetCoinSize = 18.dp
private val PillCoinSize = 24.dp
private val GiftCoinSize = 32.dp
