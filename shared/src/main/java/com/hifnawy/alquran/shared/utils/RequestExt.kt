package com.hifnawy.alquran.shared.utils

object RequestExt {
    private enum class RequestType {
        CONNECT,
        HEAD,
        TRACE,
        OPTIONS,
        GET,
        PUT,
        DELETE,
        POST,
        PATCH,
    }

    private enum class Headers {
        ACCEPT
    }

    private enum class ContentType(val value: String) {
        JSON("application/json"),
        XML("application/xml"),
        HTML("application/html"),
        TEXT("application/text"),
    }

    val METHOD_CONNECT get() = RequestType.CONNECT.name
    val METHOD_HEAD get() = RequestType.HEAD.name
    val METHOD_TRACE get() = RequestType.TRACE.name
    val METHOD_OPTIONS get() = RequestType.OPTIONS.name
    val METHOD_GET get() = RequestType.GET.name
    val METHOD_PUT get() = RequestType.PUT.name
    val METHOD_DELETE get() = RequestType.DELETE.name
    val METHOD_POST get() = RequestType.POST.name
    val METHOD_PATCH get() = RequestType.PATCH.name
    val HEADER_ACCEPT get() = Headers.ACCEPT.name.lowercase().replaceFirstChar { it.uppercase() }
    val CONTENT_TYPE_JSON get() = ContentType.JSON.value
    val CONTENT_TYPE_XML get() = ContentType.XML.value
    val CONTENT_TYPE_HTML get() = ContentType.HTML.value
    val CONTENT_TYPE_TEXT get() = ContentType.TEXT.value
}
