package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.entity.InquiryPageVO
import com.dandi.nyummy.mailbox.entity.InquiryVO
import com.dandi.nyummy.mailbox.entity.MailPageVO
import com.dandi.nyummy.mailbox.entity.RewardVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** 우편 API만 흉내 내는 저장소. [error]가 있으면 우편 요청이 그 예외를 던진다. 문의 API는 쓰지 않는다. */
internal class FakeMailboxRepository(
    var mailPage: MailPageVO = MailPageVO.empty,
    var reward: RewardVO = RewardVO.empty,
    var error: Exception? = null,
) : MailboxRepository {
    val mailRequests = mutableListOf<Pair<Long?, Int>>()
    val claimedMailIds = mutableListOf<Long>()
    var claimAllCount = 0
    val deletedMailIds = mutableListOf<Long>()
    val restoredMailIds = mutableListOf<Long>()

    override suspend fun getMails(cursor: Long?, size: Int): MailPageVO {
        mailRequests += cursor to size
        error?.let { throw it }
        return mailPage
    }

    override suspend fun claimMail(mailId: Long): RewardVO {
        claimedMailIds += mailId
        error?.let { throw it }
        return reward
    }

    override suspend fun claimAllMails(): RewardVO {
        claimAllCount++
        error?.let { throw it }
        return reward
    }

    override suspend fun deleteMail(mailId: Long) {
        deletedMailIds += mailId
        error?.let { throw it }
    }

    override suspend fun restoreMail(mailId: Long) {
        restoredMailIds += mailId
        error?.let { throw it }
    }

    override suspend fun getInquiries(cursor: Long?, size: Int): InquiryPageVO = InquiryPageVO.empty
    override suspend fun getInquiry(inquiryId: Long): InquiryVO? = null
    override suspend fun sendInquiry(category: InquiryCategory, title: String, content: String) = Unit
    override suspend fun deleteInquiry(inquiryId: Long) = Unit
    override suspend fun restoreInquiry(inquiryId: Long) = Unit
}

internal class RecordingMessageHelper : MessageHelper {
    data class SnackBarCall(
        val iconType: IconType,
        val messageText: String,
        val callToActionText: String?,
        val onClickCTA: (() -> Unit)?,
    )

    data class DialogCall(val descText: String, val cantIgnore: Boolean, val onClickButton: (() -> Unit)?)

    val snackBars = mutableListOf<SnackBarCall>()
    val dialogs = mutableListOf<DialogCall>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
        snackBars += SnackBarCall(iconType, messageText, callToActionText, onClickCTA)
    }

    override fun showSnackBar(
        iconType: IconType,
        messageRes: Int,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) = Unit

    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) {
        dialogs += DialogCall(descText, cantIgnore, onClickButton)
    }

    override fun showTwoButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        leftButtonText: String,
        onClickLeftButton: (() -> Unit)?,
        rightButtonText: String,
        onClickRightButton: (() -> Unit)?,
    ) = Unit
}

internal class RecordingNavigationHelper : NavigationHelper {
    var initialCount = 0
    var backCount = 0

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) = Unit
    override fun navigateDeepLink(route: NavRoute) = Unit
    override fun navigateToBack() {
        backCount++
    }

    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() {
        initialCount++
    }

    override fun navigateToExternalLink(url: String) = Unit
}

internal object FakeResourceHelper : ResourceHelper {
    override fun getString(resource: StringResource): String = ""
}

internal object FakeTTIHelper : TTIHelper {
    override fun startTTITracking(page: TTIPage) = Unit
    override fun startTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITracking() = Unit
    override fun shotTTILogging() = Unit
    override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
}

/** 서버 에러 바디의 `code` 를 cause 로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
internal fun httpException(code: Int, errorCode: String? = null): HttpResponseException = HttpResponseException(
    status = HttpResponseStatus.create(code),
    rawCode = code,
    errorRequestUrl = "https://test/api/v1/mails",
    msg = "Http Request Failed ($code)",
    cause = errorCode?.let(::Throwable),
)
