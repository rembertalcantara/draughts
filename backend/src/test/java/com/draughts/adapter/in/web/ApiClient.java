package com.draughts.adapter.in.web;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Minimal HTTP client that keeps cookies, i.e. one browser / one guest player. */
class ApiClient {

    record Response(int status, JsonNode body) {
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final String baseUrl;
    private final CookieManager cookies = new CookieManager();
    private final HttpClient http;

    ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    Response get(String path) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET());
    }

    Response post(String path, String json) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json == null ? "" : json)));
    }

    String cookieHeader() {
        return String.join("; ", cookies.getCookieStore().getCookies().stream()
                .map(c -> c.getName() + "=" + c.getValue()).toList());
    }

    private Response send(HttpRequest.Builder request) {
        try {
            var response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            var body = response.body().isBlank() ? null : JSON.readTree(response.body());
            return new Response(response.statusCode(), body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
