package com.dandi.nyummy.meal.presentation.component

import android.content.pm.PackageManager
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 후면 카메라 실시간 프리뷰와 정지사진 촬영을 담당하는 뷰입니다.
 *
 * [isCapturing] 이 참이 되는 순간 한 장을 촬영해 앱 캐시 디렉터리에 저장하고,
 * 결과를 [onCaptured]/[onCaptureFailed] 로 돌려줍니다. 촬영 지시·결과 반영은
 * 모두 상위 MVI 상태를 경유하며, 이 뷰는 하드웨어 실행만 맡습니다.
 *
 * 카메라 하드웨어가 없는 기기에서는 사용 불가 안내 UI를 대신 표시합니다.
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

    val hasCameraHardware = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    if (!hasCameraHardware) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            NyummyText(text = "카메라를 사용할 수 없는 기기입니다.", style = NyummyTheme.typography.bodyM)
        }
        return
    }

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

    // 촬영 지시([isCapturing])는 ViewModel 에 남아 있어, 화면이 다시 만들어지면 같은 지시로 한 장을 더 찍게 된다.
    // 지시를 이미 실행했는지 저장해 두고 한 번만 찍는다. 앞서 찍던 결과는 이전 카메라가 성공이나 실패로 돌려준다.
    var captureIssued by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isCapturing) {
        if (!isCapturing) {
            captureIssued = false
            return@LaunchedEffect
        }
        if (captureIssued) return@LaunchedEffect
        captureIssued = true
        val photoFile = File(context.cacheDir, "meal_capture_${System.currentTimeMillis()}.jpeg")
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
