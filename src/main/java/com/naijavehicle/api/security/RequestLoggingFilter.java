package com.naijavehicle.api.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_LOG_SIZE = 4096;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Eagerly read and cache the request body so it can be re-read by the controller
        byte[] requestBody = StreamUtils.copyToByteArray(request.getInputStream());
        HttpServletRequest wrappedRequest = new CachedBodyRequestWrapper(request, requestBody);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        logRequest(wrappedRequest, requestBody);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logResponse(wrappedResponse, duration);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(HttpServletRequest request, byte[] body) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullUri = query != null ? uri + "?" + query : uri;

        String headers = Collections.list(request.getHeaderNames()).stream()
                .map(name -> name + ": " + maskSensitiveHeader(name, request.getHeader(name)))
                .collect(Collectors.joining("\n    "));

        String bodyStr = formatBody(body, request.getContentType());

        log.info("\n→ REQUEST\n  {} {}\n  Headers:\n    {}\n  Body: {}",
                method, fullUri, headers, bodyStr);
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        String bodyStr = formatBody(response.getContentAsByteArray(), response.getContentType());

        log.info("\n← RESPONSE\n  Status : {} ({}ms)\n  Body   : {}",
                response.getStatus(), duration, bodyStr);
    }

    private String formatBody(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) return "(empty)";
        if (contentType != null
                && !contentType.contains("application/json")
                && !contentType.contains("text")) {
            return "(binary — not logged)";
        }
        String body = new String(bytes, StandardCharsets.UTF_8);
        return body.length() > MAX_BODY_LOG_SIZE
                ? body.substring(0, MAX_BODY_LOG_SIZE) + "... (truncated)"
                : body;
    }

    private String maskSensitiveHeader(String name, String value) {
        if ("authorization".equalsIgnoreCase(name) && value != null && value.startsWith("Bearer ")) {
            return "Bearer ***";
        }
        return value;
    }

    // Wrapper that replays the pre-read body bytes for downstream consumers
    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        CachedBodyRequestWrapper(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override public int read() { return byteStream.read(); }
                @Override public boolean isFinished() { return byteStream.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
