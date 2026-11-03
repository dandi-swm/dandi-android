package com.dandi.nyummy.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNoticeDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNoticePage
import com.dandi.nyummy.home.entity.HomeNoticeVO
import com.dandi.nyummy.shop.domain.ShopCoinChargePage
import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 홈 첫 진입 때 띄운 새 소식 공지.
 *
 * @property pages 넘겨 볼 공지 장. JSON 순서다.
 * @property links 장마다 버튼이 여는 앱 경로. 버튼이 없는 장은 null이다.
 * @property hideKeys 장마다 "다시 보지 않기"로 숨길 때 남기는 값([HomeNoticeVO.hideKey]). JSON 순서다.
 * @property isDontShowAgainChecked "다시 보지 않기"를 체크했다. 닫을 때 적용한다.
 */
@Immutable
data class HomeNoticeUiState(
    val pages: ImmutableList<NyummyNoticePage>,
    val links: ImmutableList<String?>,
    val hideKeys: ImmutableList<String> = persistentListOf(),
    val isDontShowAgainChecked: Boolean = false,
)

internal fun List<HomeNoticeVO>.toNoticeUiState() = HomeNoticeUiState(
    pages = map {
        NyummyNoticePage(
            badge = it.badge,
            imageUrl = it.imageUrl,
            title = it.title,
            body = it.body,
            meta = it.meta,
            ctaText = it.ctaText,
        )
    }.toImmutableList(),
    links = map { it.ctaLink }.toImmutableList(),
    hideKeys = map { it.hideKey }.toImmutableList(),
)

/**
 * 코인 충전 이벤트 공지를 홈 공지 한 장으로 바꾼다. 구분값은 회차 구분값이라 "다시 보지 않기"가 회차 단위로 걸리고,
 * 버튼은 늘 코인 충전 화면(공지에서 왔다는 표시 포함)을 연다. 공지가 없는 이벤트면 null이다.
 */
internal fun CoinChargeEventVO.toHomeNotice(): HomeNoticeVO? = notice?.let {
    HomeNoticeVO(
        id = eventId,
        badge = it.badge,
        imageUrl = it.imageUrl,
        title = it.title,
        body = it.body,
        ctaText = it.ctaText,
        ctaLink = ShopCoinChargePage.NOTICE_LINK,
    )
}

/** 새 소식 공지 다이얼로그. 띄울 공지가 없으면 그리지 않는다. */
@Composable
internal fun HomeNoticeDialog(notice: HomeNoticeUiState?, onIntent: (HomeIntent) -> Unit) {
    notice ?: return
    NyummyNoticeDialog(
        pages = notice.pages,
        dontShowAgainChecked = notice.isDontShowAgainChecked,
        onDontShowAgainChange = { onIntent(HomeIntent.NoticeDontShowAgainChanged(it)) },
        onClose = { onIntent(HomeIntent.NoticeClosed) },
        onCtaClick = { onIntent(HomeIntent.NoticeCtaClicked(it)) },
    )
}
