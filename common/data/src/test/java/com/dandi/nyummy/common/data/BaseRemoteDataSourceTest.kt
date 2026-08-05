package com.dandi.nyummy.common.data

import com.dandi.nyummy.common.domain.error.HttpResponseException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Response

class BaseRemoteDataSourceTest {

    private class TestDataSource : BaseRemoteDataSource() {
        fun <T> run(response: Response<T>): T = checkResponse(response)
    }

    private val dataSource = TestDataSource()

    private fun errorResponse(code: Int, body: String): Response<String> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun `성공 응답이면 바디를 그대로 반환한다`() {
        val result = dataSource.run(Response.success("body"))

        assertEquals("body", result)
    }

    @Test
    fun `공통 에러 바디에서 code를 추출해 cause에 담는다`() {
        val exception = assertThrows(HttpResponseException::class.java) {
            dataSource.run(
                errorResponse(400, """{"code":"api.common.missingParameter","message":"필수값 누락"}"""),
            )
        }

        assertEquals("api.common.missingParameter", exception.cause?.message)
        assertEquals(400, exception.rawCode)
    }

    @Test
    fun `code 필드가 없는 JSON 에러 바디는 원문을 cause에 담는다`() {
        val body = """{"message":"no code"}"""
        val exception = assertThrows(HttpResponseException::class.java) {
            dataSource.run(errorResponse(500, body))
        }

        assertEquals(body, exception.cause?.message)
    }

    @Test
    fun `JSON이 아닌 에러 바디는 원문을 cause에 담는다`() {
        val exception = assertThrows(HttpResponseException::class.java) {
            dataSource.run(errorResponse(502, "Bad Gateway"))
        }

        assertEquals("Bad Gateway", exception.cause?.message)
    }
}
