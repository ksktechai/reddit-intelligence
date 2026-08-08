package com.example.reddit.reddit;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CrawloraApiTestResource implements QuarkusTestResourceLifecycleManager {
    private HttpServer server;

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/api/v1/reddit/search", exchange -> {
                byte[] body = "{\"code\":429,\"msg\":\"rate limited\"}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.getResponseHeaders().add("Retry-After", "0");
                exchange.sendResponseHeaders(429, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return Map.of(
                    "quarkus.rest-client.crawlora-api.url",
                    "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start Crawlora test server", exception);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
