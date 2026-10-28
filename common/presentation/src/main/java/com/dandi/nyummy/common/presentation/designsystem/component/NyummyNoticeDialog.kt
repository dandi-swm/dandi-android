package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * 새 소식 공지 다이얼로그(피그마 `v2 · 20 Home & My Room`의 Notice · 01~03).
 * 카드 하나에 10:9 사진(위 왼쪽 배지), 제목, 설명, 버튼(있는 장만)을 두고, 장이 여럿이면 카드는 그대로 둔 채 안의 내용만
 * 가로로 넘긴다. 카드 높이는 가장 긴 장에 맞춰 넘겨도 바뀌지 않는다. 카드 위 닫기(X)는 없고, 카드 아래에
 * "오늘 하루 보지 않기" 체크와 "닫기"를 둔다. 바깥을 누르거나 뒤로 가기를 하면 [onClose]다.
 *
 * 폭은 최대 334다. 세로가 모자라면 사진을 자르지 않고 폭을 줄이고(사진이 10:9라 함께 작아진다), 최소 폭(240)에서도
 * 모자라면 사진 아래 글만 스크롤한다.
 */
@Composable
fun NyummyNoticeDialog(
    pages: ImmutableList<NyummyNoticePage>,
    todayChecked: Boolean,
    onTodayCheckedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onCtaClick: (pageIndex: Int) -> Unit,
) {
    if (pages.isEmpty()) return
    NyummyDialogWindow(onDismissRequest = onClose, screenMargin = NyummyComponentDimens.NoticeDialogScreenMargin) {
        NyummyNoticeContent(
            pages = pages,
            todayChecked = todayChecked,
            onTodayCheckedChange = onTodayCheckedChange,
            onClose = onClose,
            onCtaClick = onCtaClick,
        )
    }
}

/** 창 없이 그린 다이얼로그 본체(카드와 아래 줄). 받은 제약 안에서 크기 규칙을 지킨다. 프리뷰와 테스트에서 쓴다. */
@Composable
internal fun NyummyNoticeContent(
    pages: ImmutableList<NyummyNoticePage>,
    todayChecked: Boolean,
    onTodayCheckedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onCtaClick: (pageIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { pages.size }
    SubcomposeLayout(modifier) { constraints ->
        val maxWidth = min(constraints.maxWidth, NyummyComponentDimens.NoticeDialogMaxWidth.roundToPx())
        val minWidth = min(maxWidth, NyummyComponentDimens.NoticeDialogMinWidth.roundToPx())
        // 실제 배치처럼 하나씩 반올림해야 1px도 넘치지 않는다.
        val below = NyummyComponentDimens.NoticeDismissRowGap.roundToPx() + NyummyComponentDimens.NoticeDismissRowHeight.roundToPx()
        val maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
        // 모든 장의 글 칸을 그리지 않고 재기만 해서, 폭마다 가장 긴 장의 높이를 구한다.
        // 재기만 하는 칸은 화면 읽기와 테스트에 잡히지 않게 시맨틱을 지운다.
        val bodies = subcompose(SlotMeasure) {
            pages.forEach {
                NoticeBody(
                    page = it,
                    pageCount = pages.size,
                    pageIndex = 0,
                    onCtaClick = {},
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
        fun bodyHeight(width: Int) = bodies.maxOf { it.maxIntrinsicHeight(width) }

        var width = maxWidth
        var bodyHeight = bodyHeight(width)
        // 넘친 만큼 사진 높이를 줄인다. 사진이 10:9라 폭도 함께 줄고, 글이 다시 줄바꿈되므로 몇 번 다시 잰다.
        for (attempt in 0 until MaxFitAttempts) {
            val overflow = imageHeight(width) + bodyHeight + below - maxHeight
            if (overflow <= 0 || width == minWidth) break
            width = max(minWidth, width - ceil(overflow / ImageRatio).toInt())
            bodyHeight = bodyHeight(width)
        }
        // 가장 작은 폭에서도 넘치면 글 칸을 남은 높이로 묶고 글만 스크롤한다.
        val fittedBody = min(bodyHeight, maxHeight - imageHeight(width) - below).coerceAtLeast(0)
        val content = subcompose(SlotContent) {
            NoticeCardWithDismissRow(
                pages = pages,
                pagerState = pagerState,
                imageHeight = imageHeight(width).toDp(),
                bodyHeight = fittedBody.toDp(),
                todayChecked = todayChecked,
                onTodayCheckedChange = onTodayCheckedChange,
                onClose = onClose,
                onCtaClick = onCtaClick,
            )
        }.first().measure(Constraints.fixedWidth(width))
        layout(content.width, content.height) { content.place(0, 0) }
    }
}

@Composable
private fun NoticeCardWithDismissRow(
    pages: ImmutableList<NyummyNoticePage>,
    pagerState: PagerState,
    imageHeight: Dp,
    bodyHeight: Dp,
    todayChecked: Boolean,
    onTodayCheckedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onCtaClick: (pageIndex: Int) -> Unit,
) {
    val title = pages[pagerState.currentPage.coerceIn(pages.indices)].title
    Column(Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NyummyTheme.radius.l))
                .background(NyummyTheme.colors.bg.surface)
                .semantics { paneTitle = title },
        ) { index ->
            Column {
                NoticeHero(page = pages[index], modifier = Modifier.fillMaxWidth().height(imageHeight))
                NoticeBody(
                    page = pages[index],
                    pageCount = pages.size,
                    pageIndex = index,
                    onCtaClick = { onCtaClick(index) },
                    modifier = Modifier.height(bodyHeight),
                    fillHeight = true,
                )
            }
        }
        Spacer(Modifier.height(NyummyComponentDimens.NoticeDismissRowGap))
        NoticeDismissRow(todayChecked = todayChecked, onTodayCheckedChange = onTodayCheckedChange, onClose = onClose)
    }
}

/** 사진 칸 너비 : 높이 = 10 : 9. */
private const val ImageRatio = 10f / 9f

private fun imageHeight(width: Int): Int = (width / ImageRatio).toInt()

private const val MaxFitAttempts = 3

private const val SlotMeasure = "measure"
private const val SlotContent = "content"

@Preview(showBackground = true, backgroundColor = 0xFF5B6B5E, widthDp = 390, heightDp = 844)
@Composable
private fun NyummyNoticeDialogPreview() {
    NyummyTheme {
        Box(Modifier.fillMaxSize().padding(NyummyComponentDimens.NoticeDialogScreenMargin), contentAlignment = Alignment.Center) {
            NyummyNoticeContent(
                pages = persistentListOf(
                    NyummyNoticePage(
                        badge = "업데이트",
                        imageUrl = null,
                        title = "냐미가 새단장을 했어요!",
                        body = "화면을 더 보기 편하게 새로 꾸몄어요.\n새로 들어온 기능도 확인해 보세요.",
                    ),
                    NyummyNoticePage(
                        badge = "이벤트",
                        imageUrl = null,
                        meta = "10월 28일 ~ 11월 10일",
                        title = "가을 한 상 이벤트가 열렸어요",
                        body = "이벤트 페이지에서 자세히 알아보세요.",
                        ctaText = "자세히 보기",
                    ),
                ),
                todayChecked = false,
                onTodayCheckedChange = {},
                onClose = {},
                onCtaClick = {},
            )
        }
    }
}
