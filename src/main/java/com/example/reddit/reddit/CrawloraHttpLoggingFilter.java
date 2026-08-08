package com.example.reddit.reddit;

import com.example.reddit.config.CrawloraConfig;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Priority(Priorities.USER)
public class CrawloraHttpLoggingFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final Logger LOG = Logger.getLogger(CrawloraHttpLoggingFilter.class);
    private static final String REQUEST_ID_PROPERTY = CrawloraHttpLoggingFilter.class.getName() + ".requestId";
    private static final String START_NANOS_PROPERTY = CrawloraHttpLoggingFilter.class.getName() + ".startNanos";
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization",
            "cookie",
            "proxy-authorization",
            "set-cookie",
            "x-api-key");

    private final CrawloraConfig config;

    @Inject
    public CrawloraHttpLoggingFilter(CrawloraConfig config) {
        this.config = config;
    }

    @Override
    public void filter(ClientRequestContext request) {
        if (!config.httpLogging()) {
            return;
        }

        String requestId = UUID.randomUUID().toString();
        request.setProperty(REQUEST_ID_PROPERTY, requestId);
        request.setProperty(START_NANOS_PROPERTY, System.nanoTime());

        LOG.infof(
                "Crawlora HTTP request requestId=%s method=%s url=\"%s\" headers=%s body=%s",
                requestId,
                request.getMethod(),
                redactSecrets(request.getUri().toASCIIString()),
                formatHeaders(request.getHeaders()),
                requestBody(request));
    }

    @Override
    public void filter(ClientRequestContext request, ClientResponseContext response) throws IOException {
        if (!config.httpLogging()) {
            return;
        }

        byte[] responseBytes = response.getEntityStream() == null
                ? new byte[0]
                : response.getEntityStream().readAllBytes();
        response.setEntityStream(new ByteArrayInputStream(responseBytes));

        Object requestIdProperty = request.getProperty(REQUEST_ID_PROPERTY);
        String requestId = requestIdProperty == null ? "unknown" : requestIdProperty.toString();
        long durationMs = durationMs(request.getProperty(START_NANOS_PROPERTY));
        String responseBody = responseBytes.length == 0
                ? "<empty>"
                : redactSecrets(new String(responseBytes, StandardCharsets.UTF_8));

        LOG.infof(
                "Crawlora HTTP response requestId=%s status=%d durationMs=%d headers=%s body=%s",
                requestId,
                response.getStatus(),
                durationMs,
                formatHeaders(response.getHeaders()),
                responseBody);
    }

    String formatHeaders(Map<String, ? extends List<?>> headers) {
        Map<String, String> safeHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((name, values) -> {
            String renderedValue = SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT))
                    ? "[REDACTED]"
                    : values.stream()
                            .map(Objects::toString)
                            .map(this::redactSecrets)
                            .collect(Collectors.joining(", "));
            safeHeaders.put(name, renderedValue);
        });
        return safeHeaders.toString();
    }

    String redactSecrets(String value) {
        if (value == null) {
            return null;
        }
        return config.apiKey()
                .map(String::trim)
                .filter(secret -> !secret.isEmpty())
                .map(secret -> value
                        .replace(secret, "[REDACTED]")
                        .replace(URLEncoder.encode(secret, StandardCharsets.UTF_8), "[REDACTED]"))
                .orElse(value);
    }

    private String requestBody(ClientRequestContext request) {
        if (!request.hasEntity()) {
            return "<empty>";
        }
        Object entity = request.getEntity();
        if (entity instanceof byte[] bytes) {
            return redactSecrets(new String(bytes, StandardCharsets.UTF_8));
        }
        return redactSecrets(Objects.toString(entity));
    }

    private static long durationMs(Object startNanosProperty) {
        if (!(startNanosProperty instanceof Long startNanos)) {
            return -1L;
        }
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }
}
