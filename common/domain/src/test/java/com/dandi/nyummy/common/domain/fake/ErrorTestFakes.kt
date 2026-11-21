package com.dandi.nyummy.common.domain.fake

import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.lang.reflect.Proxy
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/** 문구 대신 [StringResource] 이름을 돌려준다. 어떤 안내를 골랐는지 이름으로 확인한다. */
internal object StringNameResourceHelper : ResourceHelper {
    override fun getString(resource: StringResource): String = resource.name
}

/** 띄운 다이얼로그, 스낵바, 토스트를 기록한다. 여러 스레드에서 불러도 된다. */
internal class FakeMessageHelper : MessageHelper {
    /** [pinned]는 다른 다이얼로그가 덮지 못하는 고정 다이얼로그([MessageHelper.showPinnedDialog])인지. */
    data class Dialog(val descText: String, val cantIgnore: Boolean, val onClickButton: (() -> Unit)?, val pinned: Boolean = false)

    val dialogs: MutableList<Dialog> = Collections.synchronizedList(mutableListOf())
    val snackBars: MutableList<String> = Collections.synchronizedList(mutableListOf())

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showToast(messageRes: Int) = Unit
    override fun showSnackBar(iconType: IconType, messageText: String, callToActionText: String?, onClickCTA: (() -> Unit)?) {
        snackBars += messageText
    }

    override fun showSnackBar(iconType: IconType, messageRes: Int, callToActionText: String?, onClickCTA: (() -> Unit)?) = Unit
    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) {
        dialogs += Dialog(descText, cantIgnore, onClickButton)
    }

    override fun showPinnedDialog(descText: String, buttonText: String, onClickButton: () -> Unit) {
        dialogs += Dialog(descText, cantIgnore = true, onClickButton = onClickButton, pinned = true)
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

/** 초기(로그인) 화면 이동 횟수를 센다. */
internal class FakeNavigationHelper : NavigationHelper {
    private val initial = AtomicInteger()
    val initialCount: Int get() = initial.get()

    /** 이동 직전에 부른다. 이동이 정리보다 먼저 일어나지 않았는지 확인할 때 쓴다. */
    var onNavigateToInitial: () -> Unit = {}

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) = Unit
    override fun navigateToAppLink(link: String, asRoot: Boolean) = Unit
    override fun navigateToAppLinkAfterEntry(link: String, waitForTabRoot: Boolean) = Unit
    override fun navigateToBack() = Unit
    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() {
        onNavigateToInitial()
        initial.incrementAndGet()
    }

    override fun navigateToExternalLink(url: String) = Unit
}

/** 불린 메서드 이름을 차례로 남기는 기록장. 여러 대역이 한 기록장을 같이 쓰면 부른 순서를 확인할 수 있다. */
internal class CallLog {
    val calls: MutableList<String> = Collections.synchronizedList(mutableListOf())
    fun count(call: String): Int = synchronized(calls) { calls.count { it == call } }
}

/**
 * 인터페이스 [T]의 모든 메서드를 "인터페이스이름.메서드이름"으로 [log]에 남기고 아무것도 하지 않는 대역.
 * suspend 함수도 바로 끝난다. [failingMethod] 이름의 메서드는 예외를 던진다(정리 하나가 실패해도 나머지가 도는지 볼 때 쓴다).
 */
internal inline fun <reified T : Any> recordingFake(log: CallLog, failingMethod: String? = null): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        log.calls += "${T::class.java.simpleName}.${method.name}"
        if (method.name == failingMethod) throw IllegalStateException("${method.name} failed")
        Unit
    } as T

/** 테스트에서 쓰지 않는 의존성(TTIHelper 등)의 빈 구현. */
internal inline fun <reified T : Any> noOpFake(): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ -> null } as T
