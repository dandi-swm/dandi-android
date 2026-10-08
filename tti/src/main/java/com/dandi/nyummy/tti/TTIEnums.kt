package com.dandi.nyummy.tti

enum class TimelineCategory(val categoryName: String) {
    TTI_TIME("tti_time"),
    VIEW_CREATION_TIME("view_creation_time"),
    API_REQUEST_READY_TIME("api_request_ready_time"),
    API_RESPONSE_TIME(
        "api_response_time",
    ),
    VIEW_BINDING_TIME("view_binding_time"),
    IMAGE_LOADED_TIME("image_loaded_time"),
}

enum class TTIMetaData(val metadataName: String) {
    PAGE_NAME("page_name"),
    INSTANCE_NO("instance_no"),
    IS_BOUNCED("is_bounced"),
    IS_TIMEOUT("is_timeout"),
    TTI_LOG_VERSION(
        "tti_log_version",
    ),

    /** 측정 구간에 사용자 입력을 기다린 시간(권한 안내 등)이 들어갔는지. 분석 때 거르는 데 쓴다. */
    USER_WAIT_INCLUDED("user_wait_included"),
}
