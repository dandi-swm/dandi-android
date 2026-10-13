package com.dandi.nyummy.common.data.network

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.dandi.nyummy.common.domain.helper.DeviceHelper
import com.dandi.nyummy.common.entity.time.DeviceTime
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

/**
 * 자사 API 요청에 기기·환경 정보 공통 헤더 6종을 붙인다. 서버는 값이 없으면 null로 받으므로 빈 값은 보내지 않는다.
 *
 * 같은 클라이언트로 presigned URL(외부 스토리지)에도 직접 요청하므로, 기기 정보가 밖으로 새지 않게
 * [apiHost]로 가는 요청에만 붙인다.
 *
 * 언어·지역·타임존은 앱 실행 중에도 바뀔 수 있어 요청마다 읽는다.
 */
class CommonHeaderInterceptor(
    private val context: Context,
    private val deviceHelper: DeviceHelper,
    private val apiHost: String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // 우리 개발 서버가 아닌 S3 이미지 업로드 url 발급 과정에 헤더가 추가되는 것을 방지하기위함
        if (apiHost == null || !request.url.host.equals(apiHost, ignoreCase = true)) return chain.proceed(request)
        val builder = request.newBuilder()
        commonHeaders().forEach { (name, value) ->
            value.toHeaderValue()?.let { builder.header(name, it) }
        }
        return chain.proceed(builder.build())
    }

    private fun commonHeaders(): List<Pair<String, String>> = listOf(
        HEADER_APP_VERSION to deviceHelper.appVersionName,
        HEADER_DEVICE_MODEL to Build.MODEL,
        HEADER_OS_VERSION to "Android ${Build.VERSION.RELEASE}",
        HEADER_LANGUAGE to Locale.getDefault().language,
        HEADER_REGION to region(),
        HEADER_TIMEZONE_ID to DeviceTime.zone().id,
    )

    /**
     * 기기가 실제로 동작하는 국가. 언어 설정과 별개라 SIM → 네트워크 국가 순으로 읽고,
     * 둘 다 없을 때(Wi-Fi 전용 기기 등)만 언어 설정의 지역으로 대신한다.
     */
    private fun region(): String {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val country = telephony?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: telephony?.networkCountryIso?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().country
        return country.uppercase(Locale.ROOT)
    }

    /** OkHttp는 ASCII가 아닌 헤더 값을 거부하므로 출력 가능한 ASCII만 남긴다. 남는 게 없으면 보내지 않는다. */
    private fun String.toHeaderValue(): String? =
        filter { it in ' '..'~' }.trim().takeIf { it.isNotEmpty() }

    companion object {
        const val HEADER_APP_VERSION = "X-App-Version"
        const val HEADER_DEVICE_MODEL = "X-Device-Model"
        const val HEADER_OS_VERSION = "X-OS-Version"
        const val HEADER_LANGUAGE = "X-Language"
        const val HEADER_REGION = "X-Region"
        const val HEADER_TIMEZONE_ID = "X-Timezone-Id"
    }
}
