package com.dandi.nyummy.common.data.image

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.common.data.image.dto.ImageUploadUrlDTO
import com.dandi.nyummy.common.data.image.dto.ImageUploadUrlRequestDTO
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * 용도별 이미지 업로드. 서버에서 업로드 URL을 받고([issueUploadUrl]) 그 URL로 스토리지에 직접 올린다([upload]).
 * 올린 이미지는 각 기능이 자기 생성 API에 이미지 키를 넘겨야 확정되므로, 확정은 여기서 하지 않는다.
 */
class ImageUploadDataSource(
    private val apiService: ImageUploadApiService,
    private val storageApiService: ImageStorageApiService,
) : BaseRemoteDataSource() {

    suspend fun issueUploadUrl(
        purpose: ImageUploadPurpose,
        contentType: String,
        fileSizeBytes: Long,
    ): ImageUploadUrlDTO = checkResponse(
        apiService.issueUploadUrl(
            ImageUploadUrlRequestDTO(
                purpose = purpose,
                contentType = contentType,
                fileSizeBytes = fileSizeBytes,
            ),
        ),
    )

    /**
     * 발급받은 [uploadUrl]로 [file]을 올린다. [uploadHeaders]는 서명에 들어간 값이라 그대로 싣는다.
     * 헤더에 Content-Type이 없으면 [defaultContentType]으로 보낸다.
     */
    suspend fun upload(
        uploadUrl: String,
        uploadHeaders: Map<String, String>,
        file: File,
        defaultContentType: String,
    ) {
        // Content-Type 은 RequestBody 가 전달하므로 헤더 목록에서 분리해 중복 전송을 막는다.
        val contentType = uploadHeaders.entries
            .firstOrNull { it.key.equals(HEADER_CONTENT_TYPE, ignoreCase = true) }
            ?.value
            ?: defaultContentType
        val headers = uploadHeaders.filterKeys { !it.equals(HEADER_CONTENT_TYPE, ignoreCase = true) }
        checkResponse(
            storageApiService.uploadImage(
                uploadUrl = uploadUrl,
                headers = headers,
                body = file.asRequestBody(contentType.toMediaType()),
            ),
        )
    }

    private companion object {
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}
