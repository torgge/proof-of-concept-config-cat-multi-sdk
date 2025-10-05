package com.example.configcat.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.*

@Configuration
class MdcConfiguration

@Component
@Order(1)
class CorrelationIdFilter : Filter {
    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        const val CORRELATION_ID_MDC_KEY = "correlationId"
        const val REQUEST_URI_MDC_KEY = "requestUri"
        const val REQUEST_METHOD_MDC_KEY = "requestMethod"
        const val USER_ID_MDC_KEY = "userId"
        const val SESSION_ID_MDC_KEY = "sessionId"
    }

    override fun doFilter(
        request: ServletRequest?,
        response: ServletResponse?,
        chain: FilterChain?,
    ) {
        val httpRequest = request as HttpServletRequest

        try {
            val correlationId = extractOrGenerateCorrelationId(httpRequest)

            MDC.put(CORRELATION_ID_MDC_KEY, correlationId)
            MDC.put(REQUEST_URI_MDC_KEY, httpRequest.requestURI)
            MDC.put(REQUEST_METHOD_MDC_KEY, httpRequest.method)

            httpRequest.getHeader("X-User-ID")?.let {
                MDC.put(USER_ID_MDC_KEY, it)
            }

            httpRequest.getHeader("X-Session-ID")?.let {
                MDC.put(SESSION_ID_MDC_KEY, it)
            }

            (response as jakarta.servlet.http.HttpServletResponse)
                .setHeader(CORRELATION_ID_HEADER, correlationId)

            chain?.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }

    private fun extractOrGenerateCorrelationId(request: HttpServletRequest): String {
        return request.getHeader(CORRELATION_ID_HEADER)
            ?: UUID.randomUUID().toString()
    }
}
