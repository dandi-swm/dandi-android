package com.dandi.nyummy.meal.presentation.component.ceremony

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext

/** 개발자 옵션이나 접근성에서 애니메이션을 끈 기기인지. 끈 기기에는 흔들림, 컷신 없이 정지컷만 보여 준다. */
@Composable
internal fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** [painter] 아이콘을 [center] 에 [side] 크기 정사각형으로, [degrees] 만큼 돌려 [tint] 색으로 그린다. */
internal fun DrawScope.drawIcon(
    painter: Painter,
    tint: Color,
    center: Offset,
    side: Float,
    degrees: Float = 0f,
) {
    translate(center.x - side / 2f, center.y - side / 2f) {
        rotate(degrees, Offset(side / 2f, side / 2f)) {
            with(painter) {
                draw(Size(side, side), colorFilter = ColorFilter.tint(tint))
            }
        }
    }
}
