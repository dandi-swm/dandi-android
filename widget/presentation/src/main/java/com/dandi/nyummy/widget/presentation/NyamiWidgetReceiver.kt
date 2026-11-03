package com.dandi.nyummy.widget.presentation

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.dandi.nyummy.widget.domain.GetWidgetCardUseCase
import com.dandi.nyummy.widget.entity.WidgetCardVO
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 냐미 위젯(4x2). 남겨 둔 값과 기기 시계로 카드를 정해 그리고, 다음에 바뀌는 순간 다시 그리도록 예약한다.
 *
 * 다시 그리는 때: 위젯 추가·재부팅·주기 갱신(onUpdate), 앱이 값을 바꿈([ACTION_REFRESH]),
 * 예약한 알람([ACTION_REFRESH]), 시각·타임존 변경, 앱 업데이트.
 */
@AndroidEntryPoint
class NyamiWidgetReceiver : AppWidgetProvider() {

    @Inject
    lateinit var getWidgetCard: GetWidgetCardUseCase

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> renderAsync(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        renderAsync(context)
    }

    override fun onDisabled(context: Context) {
        NyamiWidgetScheduler.cancel(context)
    }

    private fun renderAsync(context: Context) {
        val appContext = context.applicationContext
        val pending = goAsync()
        scope.launch {
            try {
                render(appContext)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 그리지 못하면 이전 화면을 둔다. 다음 예약이나 주기 갱신에서 다시 그린다.
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun render(context: Context) {
        val ids = widgetIds(context)
        if (ids.isEmpty()) {
            NyamiWidgetScheduler.cancel(context)
            return
        }
        val card = getWidgetCard(variantCount = NyamiWidgetCatalog::variantCount)
        show(context, ids, card)
        NyamiWidgetScheduler.schedule(context, card.plan.nextChangeAtMillis)
    }

    companion object {
        const val ACTION_REFRESH = "com.dandi.nyummy.widget.action.REFRESH"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun refreshIntent(context: Context): Intent =
            Intent(context, NyamiWidgetReceiver::class.java).setAction(ACTION_REFRESH)

        /** 홈 화면에 냐미 위젯이 있으면 다시 그리게 한다. 없으면 아무것도 하지 않는다. */
        fun requestRefresh(context: Context) {
            if (widgetIds(context).isEmpty()) return
            context.sendBroadcast(refreshIntent(context))
        }

        internal fun widgetIds(context: Context): IntArray =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, NyamiWidgetReceiver::class.java))

        internal fun show(context: Context, ids: IntArray, card: WidgetCardVO) {
            AppWidgetManager.getInstance(context).updateAppWidget(ids, NyamiWidgetRenderer.render(context, card))
        }
    }
}
