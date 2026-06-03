package com.cclilshy.tayd.webhook.net;

import com.cclilshy.tayd.webhook.domain.WebhookChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class WebhookClient {
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    private WebhookClient() {
    }

    public static String jsonContentType() {
        return JSON_CONTENT_TYPE;
    }

    public static int postJson(WebhookChannel channel, String json) throws IOException {
        URL url = new URL(channel.getTargetUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxyFor(channel));
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", JSON_CONTENT_TYPE);
        connection.setRequestProperty("Accept", "application/json");
        String proxyAuthorization = proxyAuthorizationHeader(channel.getProxyUrl());
        if (!proxyAuthorization.isEmpty()) {
            connection.setRequestProperty("Proxy-Authorization", proxyAuthorization);
        }
        byte[] body = (json == null ? "{}" : json).getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(body);
        }
        int code = connection.getResponseCode();
        connection.disconnect();
        return code;
    }

    private static Proxy proxyFor(WebhookChannel channel) throws IOException {
        String proxyUrl = channel.getProxyUrl();
        if (proxyUrl == null || proxyUrl.trim().isEmpty()) {
            return Proxy.NO_PROXY;
        }
        URL url = new URL(proxyUrl);
        int port = url.getPort() > 0 ? url.getPort() : defaultPort(url.getProtocol());
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(url.getHost(), port));
    }

    public static String proxyAuthorizationHeader(String proxyUrl) throws IOException {
        if (proxyUrl == null || proxyUrl.trim().isEmpty()) {
            return "";
        }
        try {
            String userInfo = new URI(proxyUrl).getUserInfo();
            if (userInfo == null || userInfo.isEmpty()) {
                return "";
            }
            return "Basic " + Base64.getEncoder().encodeToString(userInfo.getBytes(StandardCharsets.UTF_8));
        } catch (URISyntaxException err) {
            throw new IOException("invalid proxy URL", err);
        }
    }

    private static int defaultPort(String protocol) throws IOException {
        if ("http".equals(protocol)) {
            return 80;
        }
        if ("https".equals(protocol)) {
            return 443;
        }
        throw new IOException("unsupported proxy protocol");
    }
}
