package com.swm.dandi.sprite.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swm.dandi.common.presentation.component.ArchiText
import com.swm.dandi.common.presentation.ui.theme.DesignSystemTheme
import com.swm.dandi.common.presentation.ui.theme.DesignSystemThemeImpl
import com.swm.dandi.sprite.domain.SpriteFrameCalculator
import com.swm.dandi.sprite.entity.SpriteAlignElement
import com.swm.dandi.sprite.entity.SpriteVO
import kotlin.math.roundToInt

private const val MinFrameDurationMillis = 60L
private const val MaxFrameDurationMillis = 300L
private const val FrameDurationStepMillis = 10L
private const val FrameDurationSliderSteps = 23

/**
 * sprite 모듈 사용법을 확인하기 위한 sample 화면.
 *
 * 실제 앱 기능 화면이 아니라 렌더러 동작을 검증하는 예제다. 애니메이션 종류,
 * 시트 배치 방식, 재생/정지, loop, frame speed 를 바꿔 [SpriteView] 와
 * [SpriteFrameCalculator] 조합을 확인한다.
 */
@Composable
fun SpritePage(
    viewModel: SpriteViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SpritePageContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
private fun SpritePageContent(
    uiState: SpriteUIState,
    onIntent: (SpriteIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SpriteFrameTimingLogEffect(uiState)

    val selectedSpec = uiState.selectedSpec

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SpriteSampleHeader()
            SpriteAnimationStage(
                uiState = uiState,
                onFrameChanged = { frameIndex ->
                    onIntent(SpriteIntent.FrameChanged(frameIndex))
                },
            )
            SpriteSheetFramePreview(
                spec = selectedSpec,
                currentFrame = uiState.currentFrame,
                modifier = Modifier.fillMaxWidth(),
            )
            SpriteAnimationSelector(
                selectedAnimation = uiState.selectedAnimation,
                onSelect = { animation ->
                    onIntent(SpriteIntent.SelectAnimation(animation))
                },
            )
            SpriteAlignElementDropdown(
                selectedAlignElement = uiState.selectedAlignElement,
                onSelect = { alignElement ->
                    onIntent(SpriteIntent.SelectAlignElement(alignElement))
                },
            )
            SpriteFrameSpeedSlider(
                frameDurationMillis = uiState.frameDurationMillis,
                onFrameDurationChanged = { frameDurationMillis ->
                    onIntent(SpriteIntent.FrameDurationChanged(frameDurationMillis))
                },
            )
            SpriteLoopToggle(
                loop = uiState.loop,
                onLoopChanged = { loop ->
                    onIntent(SpriteIntent.LoopChanged(loop))
                },
            )
            SpritePlaybackControls(
                isPlaying = uiState.isPlaying,
                onPreviousFrame = {
                    onIntent(SpriteIntent.PreviousFrame)
                },
                onTogglePlayback = {
                    onIntent(SpriteIntent.TogglePlayback)
                },
                onNextFrame = {
                    onIntent(SpriteIntent.NextFrame)
                },
            )
            SpriteFrameCounter(
                currentFrame = uiState.currentFrame,
                totalFrames = selectedSpec.totalFrames,
            )
        }
    }
}

@Composable
private fun SpriteSampleHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ArchiText(
            text = stringResource(R.string.sprite_sample_title),
            style = DesignSystemThemeImpl.typeScale.titleStrongL,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        ArchiText(
            text = stringResource(R.string.sprite_sample_description),
            style = DesignSystemThemeImpl.typeScale.textRegularM,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

@Composable
private fun SpriteAnimationStage(
    uiState: SpriteUIState,
    onFrameChanged: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(196.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2),
        contentAlignment = Alignment.Center,
    ) {
        SpriteView(
            spec = uiState.selectedSpec.copy(loop = uiState.loop),
            modifier = Modifier.size(156.dp),
            playing = uiState.isPlaying,
            frameIndex = uiState.currentFrame,
            onFrameChanged = onFrameChanged,
        )
    }
}

@Composable
private fun SpriteSheetFramePreview(
    spec: SpriteVO,
    currentFrame: Int,
    modifier: Modifier = Modifier,
) {
    val spriteSheet = ImageBitmap.imageResource(spec.spriteSheetRes)
    val safeFrame = currentFrame.coerceIn(0, spec.totalFrames - 1)
    val spriteFrame = SpriteFrameCalculator.frameAt(
        spec = spec,
        frameIndex = safeFrame,
    )
    val highlightColor = DesignSystemThemeImpl.designSystemColor.contentFavorite
    val shape = RoundedCornerShape(8.dp)

    Canvas(
        modifier = modifier
            .height(260.dp)
            .clip(shape)
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
                ),
                shape = shape,
            ),
    ) {
        val targetWidth = size.width.roundToInt()
        val targetHeight = size.height.roundToInt()
        if (targetWidth <= 0 || targetHeight <= 0) return@Canvas

        // 원본 시트가 LeftRight/TopDown 처럼 극단적인 비율이어도 preview 영역 안에
        // 전체가 보이도록 fit-center 로 배치한다.
        val imageAspectRatio = spriteSheet.width.toFloat() / spriteSheet.height.toFloat()
        val canvasAspectRatio = size.width / size.height
        val drawWidth: Float
        val drawHeight: Float
        if (imageAspectRatio >= canvasAspectRatio) {
            drawWidth = size.width
            drawHeight = size.width / imageAspectRatio
        } else {
            drawWidth = size.height * imageAspectRatio
            drawHeight = size.height
        }
        val drawLeft = (size.width - drawWidth) / 2f
        val drawTop = (size.height - drawHeight) / 2f

        drawImage(
            image = spriteSheet,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(spriteSheet.width, spriteSheet.height),
            dstOffset = IntOffset(drawLeft.roundToInt(), drawTop.roundToInt()),
            dstSize = IntSize(drawWidth.roundToInt(), drawHeight.roundToInt()),
            filterQuality = FilterQuality.None,
        )

        // frame 좌표는 원본 이미지 pixel 기준이므로, preview 에 그린 축소/확대 비율을
        // 곱해 highlight 위치를 맞춘다.
        val scaleX = drawWidth / spriteSheet.width.toFloat()
        val scaleY = drawHeight / spriteSheet.height.toFloat()
        val topLeft = Offset(
            x = drawLeft + spriteFrame.srcX * scaleX,
            y = drawTop + spriteFrame.srcY * scaleY,
        )
        val frameSize = Size(
            width = spriteFrame.widthPx * scaleX,
            height = spriteFrame.heightPx * scaleY,
        )

        drawRect(
            color = highlightColor.copy(alpha = 0.16f),
            topLeft = topLeft,
            size = frameSize,
        )
        drawRect(
            color = highlightColor,
            topLeft = topLeft,
            size = frameSize,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Composable
private fun SpriteAlignElementDropdown(
    selectedAlignElement: SpriteAlignElement,
    onSelect: (SpriteAlignElement) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2)
                .border(
                    border = BorderStroke(
                        width = 1.dp,
                        color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
                    ),
                    shape = shape,
                )
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ArchiText(
                    text = stringResource(R.string.sprite_sample_align_mode),
                    style = DesignSystemThemeImpl.typeScale.textRegularM,
                    color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3,
                    maxLines = 1,
                )
                ArchiText(
                    text = stringResource(selectedAlignElement.labelRes),
                    style = DesignSystemThemeImpl.typeScale.textStrongM,
                    color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                    maxLines = 1,
                )
            }
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Filled.ArrowDropDown,
                tint = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
                contentDescription = stringResource(R.string.sprite_sample_align_mode),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SpriteAlignElement.entries.forEach { alignElement ->
                DropdownMenuItem(
                    text = {
                        ArchiText(
                            text = stringResource(alignElement.labelRes),
                            style = DesignSystemThemeImpl.typeScale.textStrongM,
                            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                            maxLines = 1,
                        )
                    },
                    trailingIcon = {
                        if (alignElement == selectedAlignElement) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                imageVector = Icons.Filled.Check,
                                tint = DesignSystemThemeImpl.designSystemColor.contentAccent,
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(alignElement)
                    },
                )
            }
        }
    }
}

@Composable
private fun SpriteFrameSpeedSlider(
    frameDurationMillis: Long,
    onFrameDurationChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderValue = frameDurationMillis.toSliderValue()
    val framesPerSecond = (1_000f / frameDurationMillis).roundToInt()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ArchiText(
                text = stringResource(R.string.sprite_sample_frame_speed),
                style = DesignSystemThemeImpl.typeScale.textStrongM,
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                maxLines = 1,
            )
            ArchiText(
                text = stringResource(
                    R.string.sprite_sample_frame_speed_value,
                    frameDurationMillis,
                    framesPerSecond,
                ),
                style = DesignSystemThemeImpl.typeScale.textRegularM,
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3,
                maxLines = 1,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                onFrameDurationChanged(value.toFrameDurationMillis())
            },
            valueRange = 0f..1f,
            steps = FrameDurationSliderSteps,
            colors = SliderDefaults.colors(
                thumbColor = DesignSystemThemeImpl.designSystemColor.contentAccent,
                activeTrackColor = DesignSystemThemeImpl.designSystemColor.contentAccent,
                inactiveTrackColor = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ArchiText(
                text = stringResource(R.string.sprite_sample_speed_slow),
                style = DesignSystemThemeImpl.typeScale.textRegularM,
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3,
                maxLines = 1,
            )
            ArchiText(
                text = stringResource(R.string.sprite_sample_speed_fast),
                style = DesignSystemThemeImpl.typeScale.textRegularM,
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3,
                maxLines = 1,
            )
        }
    }
}

/**
 * frame duration 을 slider 의 0f..1f 값으로 변환한다.
 *
 * 왼쪽은 느림([MaxFrameDurationMillis]), 오른쪽은 빠름([MinFrameDurationMillis]) 이므로 값의 방향을
 * duration 과 반대로 둔다.
 */
private fun Long.toSliderValue(): Float {
    val durationRange = MaxFrameDurationMillis - MinFrameDurationMillis
    val offsetFromSlowest = MaxFrameDurationMillis - this
    return offsetFromSlowest.toFloat() / durationRange
}

/**
 * slider 값을 frame duration 으로 되돌린다.
 *
 * 사용자가 드래그하는 중에도 10ms 단위로 snap 해서 sample 이 표시하는 값과
 * 실제 재생 속도를 일치시킨다.
 */
private fun Float.toFrameDurationMillis(): Long {
    val durationRange = MaxFrameDurationMillis - MinFrameDurationMillis
    val rawDuration = MaxFrameDurationMillis - this.coerceIn(0f, 1f) * durationRange
    val snappedDuration = (rawDuration / FrameDurationStepMillis)
        .roundToInt() * FrameDurationStepMillis
    return snappedDuration
        .coerceIn(
            minimumValue = MinFrameDurationMillis,
            maximumValue = MaxFrameDurationMillis,
        )
}

@Composable
private fun SpriteAnimationSelector(
    selectedAnimation: SpriteSampleAnimation,
    onSelect: (SpriteSampleAnimation) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SpriteSampleAnimation.entries.forEach { animation ->
            SpriteOptionButton(
                text = stringResource(animation.labelRes),
                selected = animation == selectedAnimation,
                onClick = { onSelect(animation) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SpriteLoopToggle(
    loop: Boolean,
    onLoopChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .toggleable(
                value = loop,
                role = Role.Checkbox,
                onValueChange = onLoopChanged,
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val shape = RoundedCornerShape(4.dp)
        val backgroundColor = if (loop) {
            DesignSystemThemeImpl.designSystemColor.contentAccent
        } else {
            DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2
        }
        val borderColor = if (loop) {
            DesignSystemThemeImpl.designSystemColor.contentAccent
        } else {
            DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(shape)
                .background(backgroundColor)
                .border(BorderStroke(1.dp, borderColor), shape),
            contentAlignment = Alignment.Center,
        ) {
            if (loop) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.Filled.Check,
                    tint = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
                    contentDescription = null,
                )
            }
        }
        ArchiText(
            text = stringResource(R.string.sprite_sample_loop),
            style = DesignSystemThemeImpl.typeScale.textStrongM,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            maxLines = 1,
        )
    }
}

@Composable
private fun SpritePlaybackControls(
    isPlaying: Boolean,
    onPreviousFrame: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNextFrame: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SpriteIconButton(
            imageVector = Icons.Filled.SkipPrevious,
            contentDescription = stringResource(R.string.sprite_sample_previous_frame),
            onClick = onPreviousFrame,
            modifier = Modifier.weight(1f),
        )
        SpriteIconButton(
            imageVector = if (isPlaying) {
                Icons.Filled.Pause
            } else {
                Icons.Filled.PlayArrow
            },
            contentDescription = stringResource(
                if (isPlaying) {
                    R.string.sprite_sample_pause
                } else {
                    R.string.sprite_sample_play
                },
            ),
            selected = isPlaying,
            onClick = onTogglePlayback,
            modifier = Modifier.weight(1f),
        )
        SpriteIconButton(
            imageVector = Icons.Filled.SkipNext,
            contentDescription = stringResource(R.string.sprite_sample_next_frame),
            onClick = onNextFrame,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SpriteFrameCounter(
    currentFrame: Int,
    totalFrames: Int,
) {
    val safeCurrentFrame = currentFrame.coerceIn(0, totalFrames - 1)

    ArchiText(
        text = stringResource(
            R.string.sprite_sample_frame_counter,
            safeCurrentFrame + 1,
            totalFrames,
        ),
        style = DesignSystemThemeImpl.typeScale.textStrongM,
        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun SpriteIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val backgroundColor = if (selected) {
        DesignSystemThemeImpl.designSystemColor.contentAccent
    } else {
        DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2
    }
    val contentColor = if (selected) {
        DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1
    } else {
        DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2
    }
    val borderColor = if (selected) {
        DesignSystemThemeImpl.designSystemColor.contentAccent
    } else {
        DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = imageVector,
            tint = contentColor,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun SpriteOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val backgroundColor = if (selected) {
        DesignSystemThemeImpl.designSystemColor.contentAccent
    } else {
        DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2
    }
    val contentColor = if (selected) {
        DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1
    } else {
        DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2
    }
    val borderColor = if (selected) {
        DesignSystemThemeImpl.designSystemColor.contentAccent
    } else {
        DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        ArchiText(
            text = text,
            style = DesignSystemThemeImpl.typeScale.textStrongM,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun SpritePageContentPreview() {
    DesignSystemTheme {
        SpritePageContent(
            uiState = SpriteUIState(
                isLoading = false,
                isPlaying = false,
                loop = true,
            ),
            onIntent = {},
        )
    }
}
