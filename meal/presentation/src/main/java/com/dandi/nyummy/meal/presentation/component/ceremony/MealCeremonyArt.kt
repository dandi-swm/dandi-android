package com.dandi.nyummy.meal.presentation.component.ceremony

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteView
import com.dandi.nyummy.meal.presentation.R
import kotlin.math.roundToInt

/**
 * 연출에 쓰는 냐미 그림 한 장면. 정지 그림이면 움직임은 화면 코드가 붙이고(확대, 흔들림, 통통 튀기),
 * 프레임 시트면 시트를 재생한다. 두 방식을 바꿔 끼울 수 있게 장면마다 하나씩 정한다.
 */
@Immutable
internal sealed interface CeremonyArt {

    /** 정지 그림 한 장. */
    data class Still(@DrawableRes val resId: Int) : CeremonyArt

    /**
     * 같은 크기 칸이 한 줄에 [columns] 칸씩 놓인 시트. 0번 칸부터 행 순서로 재생한다.
     * 매끄러운 그림을 줄여 그리므로 픽셀 보존 없이 부드럽게 샘플링한다.
     */
    data class Frames(
        @DrawableRes val resId: Int,
        val cellWidth: Int,
        val cellHeight: Int,
        val frameCount: Int,
        val frameMillis: Int,
        val columns: Int = SHEET_COLUMNS,
    ) : CeremonyArt {
        val sheet: NyummySpriteSheet
            get() = NyummySpriteSheet(
                imageRes = resId,
                frameWidth = cellWidth,
                frameHeight = cellHeight,
                totalFrames = frameCount,
                framesPerRow = columns,
                frameDurationMillis = frameMillis,
            )
    }
}

/** 장면별 냐미 그림. */
internal object MealCeremonyArt {

    /** 숟가락을 까딱이고 침을 꼴깍 삼키며 기다리는 냐미(6프레임 반복). 식탁 뒤에 앉아 있다. */
    val waiting = CeremonyArt.Frames(
        resId = R.drawable.meal_ceremony_nyummy_wait_grid,
        cellWidth = 455,
        cellHeight = 405,
        frameCount = 6,
        frameMillis = 140,
    )

    /**
     * 띠욧 클로즈업(6프레임). 장면이 프레임을 직접 고른다.
     * 0, 1 = 눈 감고 오물오물 / 2 = 멈칫 눈 뜸 / 3 = 별눈 펑 / 4, 5 = 별눈 반짝 반복.
     */
    val tasting = CeremonyArt.Frames(
        resId = R.drawable.meal_ceremony_nyummy_tiyot_grid,
        cellWidth = 491,
        cellHeight = 471,
        frameCount = 6,
        frameMillis = 120,
    )

    /** 만세하며 펄쩍 뛰는 냐미. 뛰기는 화면 코드가 붙인다. */
    val cheering: CeremonyArt = CeremonyArt.Still(R.drawable.meal_ceremony_nyummy_cheer)

    /**
     * 기다리는 냐미 앞을 가리는 나무 식탁 앞면. 윗판 아래로 판자가 길게 이어져, 윗선부터 화면 아래 끝까지 덮는다.
     * 냐미가 앉는 높이는 윗판과 첫 판자까지의 높이([TableFrontAspect])로 정한다.
     */
    val tableFront: Int = R.drawable.meal_ceremony_table_front_tall

    /** 윗판과 첫 판자까지(냐미 몸 아래쪽을 가리는 높이)의 폭 / 높이. */
    const val TableFrontAspect = 1401f / 389f
}

/**
 * [art] 를 폭 [width] 로 그린다.
 * 시트는 [frame] 을 주면 그 칸만 그리고, 없으면 스스로 재생한다([loop] 이면 반복, 아니면 한 번 재생 후 마지막 칸에 머문다).
 */
@Composable
internal fun CeremonyArtView(
    art: CeremonyArt,
    width: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    frame: Int? = null,
    loop: Boolean = true,
) {
    when (art) {
        is CeremonyArt.Still -> Image(
            painter = painterResource(art.resId),
            contentDescription = contentDescription,
            contentScale = ContentScale.FillWidth,
            modifier = modifier.width(width),
        )

        is CeremonyArt.Frames -> if (frame != null) {
            SheetFrame(
                art = art,
                image = ImageBitmap.imageResource(art.resId),
                frame = frame,
                contentDescription = contentDescription,
                modifier = modifier.width(width),
            )
        } else {
            NyummySpriteView(
                sheet = art.sheet,
                displayWidth = width,
                iterations = if (loop) null else 1,
                pixelArt = false,
                contentDescription = contentDescription,
                modifier = modifier,
            )
        }
    }
}

/** 시트의 [frame] 번 칸 하나를 폭에 맞춰 그린다. */
@Composable
private fun SheetFrame(
    art: CeremonyArt.Frames,
    image: ImageBitmap,
    frame: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val index = frame.coerceIn(0, art.frameCount - 1)
    val column = index % art.columns
    val row = index / art.columns
    Canvas(
        modifier = modifier
            .aspectRatio(art.cellWidth.toFloat() / art.cellHeight)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
    ) {
        drawImage(
            image = image,
            srcOffset = IntOffset(column * art.cellWidth, row * art.cellHeight),
            srcSize = IntSize(art.cellWidth, art.cellHeight),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}

private const val SHEET_COLUMNS = 4
