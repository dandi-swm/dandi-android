package com.dandi.nyummy.meal.presentation.component

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/**
 * 후면 카메라 실시간 프리뷰와 정지사진 촬영을 담당하는 뷰입니다.
 *
 * [isCapturing] 이 참이 되는 순간 한 장을 촬영해 앱 캐시 디렉터리에 저장하고,
 * 결과를 [onCaptured]/[onCaptureFailed] 로 돌려줍니다. 촬영 지시·결과 반영은
 * 모두 상위 MVI 상태를 경유하며, 이 뷰는 하드웨어 실행만 맡습니다.
 */
@Composable
fun MealCameraPreview(
    isCapturing: Boolean,
    onCaptured: (String) -> Unit,
    onCaptureFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    val currentOnCaptureFailed by rememberUpdatedState(onCaptureFailed)

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }

    AndroidView(
        factory = { viewContext ->
            PreviewView(viewContext).apply { this.controller = controller }
        },
        modifier = modifier,
    )

    LaunchedEffect(isCapturing) {
        if (!isCapturing) return@LaunchedEffect
        val photoFile = File(context.cacheDir, "meal_capture_${System.currentTimeMillis()}.jpg")
        controller.takePicture(
            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    currentOnCaptured(photoFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    currentOnCaptureFailed()
                }
            },
        )
    }
}
