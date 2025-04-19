package com.zbib.hiresync.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for HTTP content processing, extracted from ConsolidatedRequestLogger.
 * Handles header extraction, content processing, and related operations.
 */
@Component
@RequiredArgsConstructor
public class HttpContentProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpContentProcessor.class);
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "token", "x-api-key", "x-auth-token"
    );
    private static final List<MediaType> VISIBLE_TYPES = Arrays.asList(
            MediaType.valueOf("text/*"),
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            MediaType.valueOf("application/*+json"),
            MediaType.valueOf("application/*+xml")
    );
    private static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();
    private static final String BINARY_DATA_PLACEHOLDER = "[Binary data]";
    private static final String BINARY_CONTENT_PLACEHOLDER = "[Binary content]";
    private static final String TRUNCATED_SUFFIX = "... [truncated]";
    
    @Value("${hiresync.logging.max-content-length:1000}")
    private int maxContentLength;
    
    private final ObjectMapper objectMapper;
    private final MaskingUtils maskingUtils;
    
    /**
     * Extracts headers from HTTP request with sensitive data masked.
     *
     * @param request HTTP request
     * @return Map of header names to values (masked if sensitive)
     */
    public Map<String, String> extractRequestHeaders(HttpServletRequest request) {
        if (request == null) {
            return Collections.emptyMap();
        }
        
        Map<String, String> headerMap = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(name -> {
            String value = request.getHeader(name);
            if (isSensitiveHeader(name)) {
                value = MaskingUtils.DEFAULT_MASK;
            }
            headerMap.put(name, value);
        });
        return headerMap;
    }
    
    /**
     * Extracts headers from HTTP response with sensitive data masked.
     *
     * @param response HTTP response
     * @return Map of header names to values (masked if sensitive)
     */
    public Map<String, String> extractResponseHeaders(HttpServletResponse response) {
        if (response == null) {
            return Collections.emptyMap();
        }
        
        Map<String, String> headerMap = new HashMap<>();
        response.getHeaderNames().forEach(name -> {
            String value = response.getHeader(name);
            if (isSensitiveHeader(name)) {
                value = MaskingUtils.DEFAULT_MASK;
            }
            headerMap.put(name, value);
        });
        return headerMap;
    }
    
    /**
     * Determines if a header name is sensitive and should be masked.
     *
     * @param name Header name
     * @return true if sensitive, false otherwise
     */
    public boolean isSensitiveHeader(String name) {
        if (name == null) {
            return false;
        }
        
        String lowerName = name.toLowerCase();
        return SENSITIVE_HEADERS.contains(lowerName) || maskingUtils.isSensitive(lowerName);
    }
    
    /**
     * Extracts the client IP address from request headers.
     *
     * @param request HTTP request
     * @return Client IP address
     */
    public String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Gets the request body as a string.
     *
     * @param request Cached request wrapper
     * @return Request body string or null if empty
     */
    public String getRequestBody(ContentCachingRequestWrapper request) {
        if (request == null) {
            return null;
        }
        
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        
        try {
            String contentEncoding = request.getCharacterEncoding();
            return truncate(new String(content, contentEncoding != null ? contentEncoding : DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Failed to parse request body", e);
            return BINARY_DATA_PLACEHOLDER;
        }
    }
    
    /**
     * Gets the response body as a string.
     *
     * @param response Cached response wrapper
     * @return Response body string or null if empty
     */
    public String getResponseBody(ContentCachingResponseWrapper response) {
        if (response == null) {
            return null;
        }
        
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        
        try {
            String contentEncoding = response.getCharacterEncoding();
            return truncate(new String(content, contentEncoding != null ? contentEncoding : DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Failed to parse response body", e);
            return BINARY_DATA_PLACEHOLDER;
        }
    }
    
    /**
     * Processes content based on content type.
     *
     * @param contentType MIME type of the content
     * @param content Content as string
     * @return Processed and possibly masked content
     */
    public Object processContent(String contentType, String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        if (!isVisibleContent(contentType)) {
            return BINARY_CONTENT_PLACEHOLDER;
        }
        
        if (isJsonContent(contentType)) {
            try {
                return objectMapper.readValue(content, Object.class);
            } catch (Exception e) {
                return maskingUtils.mask(content);
            }
        }
        
        return maskingUtils.mask(content);
    }
    
    /**
     * Determines if content is of a visible/readable type.
     *
     * @param contentType MIME type
     * @return true if content is visible/readable
     */
    private boolean isVisibleContent(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return false;
        }
        
        return VISIBLE_TYPES.stream()
                .anyMatch(visibleType -> visibleType.includes(mediaType));
    }
    
    /**
     * Determines if content is JSON.
     *
     * @param contentType MIME type
     * @return true if content is JSON
     */
    private boolean isJsonContent(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("json");
    }
    
    /**
     * Truncates content if it exceeds maximum length.
     *
     * @param content Content to truncate
     * @return Truncated content
     */
    private String truncate(String content) {
        if (content == null || content.length() <= maxContentLength) {
            return content;
        }
        return content.substring(0, maxContentLength) + TRUNCATED_SUFFIX;
    }
} 