package com.dandi.nyummy.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNoticeDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyNoticePage
import com.dandi.nyummy.home.entity.HomeNoticeVO
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * 홈 첫 진입 때 띄운 새 소식 공지.
 *
 * @property pages 넘겨 볼 공지 장. JSON 순서다.
 * @property links 장마다 버튼이 여는 앱 경로. 버튼이 없는 장은 null이다.
 * @property isHideTodayChecked "오늘 하루 보지 않기"를 체크했다. 닫을 때 적용한다.
 */
@Immutable
data class HomeNoticeUiState(
    val pages: ImmutableList<NyummyNoticePage>,
    val links: ImmutableList<String?>,
    val isHideTodayChecked: Boolean = false,
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
)

/** 새 소식 공지 다이얼로그. 띄울 공지가 없으면 그리지 않는다. */
@Composable
internal fun HomeNoticeDialog(notice: HomeNoticeUiState?, onIntent: (HomeIntent) -> Unit) {
    notice ?: return
    NyummyNoticeDialog(
        pages = notice.pages,
        todayChecked = notice.isHideTodayChecked,
        onTodayCheckedChange = { onIntent(HomeIntent.NoticeHideTodayChanged(it)) },
        onClose = { onIntent(HomeIntent.NoticeClosed) },
        onCtaClick = { onIntent(HomeIntent.NoticeCtaClicked(it)) },
    )
}
