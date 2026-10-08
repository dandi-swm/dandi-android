package com.dandi.nyummy.common.presentation.image

import coil3.EventListener
import coil3.decode.DataSource
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult

/**
 * Coil 이미지 요청 하나의 로딩 시간을 [ImageLoadReport] 로 넘긴다. 요청마다 새로 만든다([factory]).
 * 시작부터 이미지가 준비될 때까지를 재고, 어디서 가져왔는지(메모리 캐시, 디스크, 네트워크)를 함께 남긴다.
 */
internal class ImageLoadEventListener(
    private val report: ImageLoadReport,
) : EventListener() {

    private var trace: ImageLoadTrace? = null

    override fun onStart(request: ImageRequest) {
        trace = report.start(ImageKind.of(request.data))
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        trace?.finish(result.dataSource.toImageSource(), success = true)
        trace = null
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        trace?.finish(source = null, success = false)
        trace = null
    }

    override fun onCancel(request: ImageRequest) {
        // 화면을 떠나 취소된 요청은 로딩 시간으로 보지 않는다.
        trace = null
    }

    companion object {
        fun factory(report: ImageLoadReport): EventListener.Factory =
            EventListener.Factory { ImageLoadEventListener(report) }
    }
}

private fun DataSource.toImageSource(): ImageSource = when (this) {
    DataSource.MEMORY_CACHE -> ImageSource.MEMORY_CACHE
    DataSource.MEMORY -> ImageSource.MEMORY
    DataSource.DISK -> ImageSource.DISK
    DataSource.NETWORK -> ImageSource.NETWORK
}
