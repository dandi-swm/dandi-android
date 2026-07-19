package com.dandi.nyummy.main.domain.deeplink

/**
 * 라우트 path 템플릿을 deep-link URI 의 path segment 들과 매칭한다.
 *
 * 템플릿은 in-app path 와 동일한 문자열을 그대로 쓴다(앱 내 이동과 deep-link 가 같은 식별자를 공유).
 * 동적 구간은 중괄호로 표기한다. 예) "/articleList/articlePage/{articleId}".
 *
 * - 리터럴 구간: 값이 정확히 일치해야 한다.
 * - 파라미터 구간(`{name}`): 임의의 한 segment 와 매칭되고, 그 값이 [match] 결과 맵의 `name` 키에 담긴다.
 *
 * segment 개수가 다르면 매칭 실패. query parameter 는 본 클래스가 관여하지 않으며 호출부에서 병합한다.
 *
 * Android 비의존 순수 로직 — 호스트(main/presentation)의 deep-link 해석이 본 primitive 를 사용한다.
 */
class RoutePattern(val template: String) {

    private sealed interface Segment {
        data class Literal(val value: String) : Segment
        data class Param(val name: String) : Segment
    }

    private val segments: List<Segment> = template
        .trim('/')
        .split('/')
        .filter { it.isNotEmpty() }
        .map { raw ->
            if (raw.length >= 2 && raw.first() == '{' && raw.last() == '}') {
                Segment.Param(raw.substring(1, raw.length - 1))
            } else {
                Segment.Literal(raw)
            }
        }

    /**
     * 동적 구간을 1개 이상 가진 템플릿인지. 전부 리터럴이면 정적 path 이므로
     * 호출부의 exact map(appRouteByPath)이 O(1) 로 처리한다(여기 템플릿 매칭 대상이 아님).
     */
    val hasParams: Boolean = segments.any { it is Segment.Param }

    /**
     * URI path segment 목록과 매칭한다.
     * @return 매칭 성공 시 `{파라미터명 -> 값}` 맵(파라미터 없으면 빈 맵), 실패 시 null.
     */
    fun match(uriSegments: List<String>): Map<String, String>? {
        if (uriSegments.size != segments.size) return null
        val params = LinkedHashMap<String, String>()
        segments.forEachIndexed { index, segment ->
            val value = uriSegments[index]
            when (segment) {
                is Segment.Literal -> if (segment.value != value) return null
                is Segment.Param -> params[segment.name] = value
            }
        }
        return params
    }
}
