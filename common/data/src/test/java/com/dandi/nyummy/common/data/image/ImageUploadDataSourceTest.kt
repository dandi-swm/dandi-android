package com.dandi.nyummy.common.data.image

import com.dandi.nyummy.common.domain.error.HttpResponseException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * [ImageUploadDataSource]의 발급 요청(경로, purpose 직렬화)과 스토리지 업로드(헤더 처리)를 검증합니다.
 */
class ImageUploadDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var dataSource: ImageUploadDataSource

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        dataSource = ImageUploadDataSource(
            apiService = retrofit.create(ImageUploadApiService::class.java),
            storageApiService = retrofit.create(ImageStorageApiService::class.java),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `발급 요청은 공통 경로로 용도와 파일 정보를 보내고 업로드 정보를 파싱한다`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "uploadUrl": "https://bucket.s3/meals/1/a.jpg?X-Amz-Signature=sig",
                  "imageKey": "meals/1/a.jpg",
                  "uploadMethod": "PUT",
                  "uploadHeaders": {"Content-Type": "image/jpeg", "x-amz-tagging": "status=temp"},
                  "expiresAt": "2026-10-12T15:00:00Z"
                }
                """.trimIndent(),
            ),
        )

        val result = dataSource.issueUploadUrl(
            purpose = ImageUploadPurpose.MEAL,
            contentType = "image/jpeg",
            fileSizeBytes = 1234,
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/images/presigned-url", request.path)
        assertEquals(
            Json.parseToJsonElement("""{"purpose":"MEAL","contentType":"image/jpeg","fileSizeBytes":1234}"""),
            Json.parseToJsonElement(request.body.readUtf8()),
        )
        assertEquals("meals/1/a.jpg", result.imageKey)
        assertEquals("PUT", result.uploadMethod)
        assertEquals("status=temp", result.uploadHeaders?.get("x-amz-tagging"))
    }

    @Test
    fun `문의 용도는 INQUIRY로 직렬화된다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"imageKey":"inquiries/1/a.jpg"}"""))

        dataSource.issueUploadUrl(ImageUploadPurpose.INQUIRY, "image/jpeg", 1)

        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("INQUIRY", body["purpose"]?.jsonPrimitive?.content)
    }

    @Test
    fun `발급 실패는 서버 code를 담은 HttpResponseException으로 올라온다`() {
        server.enqueue(
            MockResponse().setResponseCode(409).setBody(
                """{"code":"api.meal.dailyCountExceeded","message":"하루에 등록할 수 있는 식사 개수를 초과했습니다."}""",
            ),
        )

        val error = assertThrows(HttpResponseException::class.java) {
            runBlocking { dataSource.issueUploadUrl(ImageUploadPurpose.MEAL, "image/jpeg", 1) }
        }

        assertEquals(409, error.rawCode)
        assertEquals("api.meal.dailyCountExceeded", error.cause?.message)
    }

    @Test
    fun `업로드는 받은 URL로 PUT하고 Content-Type은 바디로 한 번만 보내며 나머지 헤더는 그대로 싣는다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        val file = tempFolder.newFile("meal.png").apply { writeBytes(ByteArray(16) { it.toByte() }) }

        dataSource.upload(
            uploadUrl = server.url("/bucket/meals/1/a.png?X-Amz-Signature=sig").toString(),
            uploadHeaders = mapOf("content-type" to "image/png", "x-amz-tagging" to "status=temp"),
            file = file,
            defaultContentType = "image/jpeg",
        )

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/bucket/meals/1/a.png?X-Amz-Signature=sig", request.path)
        assertEquals(listOf("image/png"), request.headers.values("Content-Type"))
        assertEquals("status=temp", request.getHeader("x-amz-tagging"))
        assertEquals(16L, request.bodySize)
    }

    @Test
    fun `업로드 헤더에 Content-Type이 없으면 기본 타입으로 보낸다`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        val file = tempFolder.newFile("meal.jpg").apply { writeBytes(ByteArray(4)) }

        dataSource.upload(
            uploadUrl = server.url("/bucket/meals/1/a.jpg").toString(),
            uploadHeaders = emptyMap(),
            file = file,
            defaultContentType = "image/jpeg",
        )

        assertEquals(listOf("image/jpeg"), server.takeRequest().headers.values("Content-Type"))
    }
}
