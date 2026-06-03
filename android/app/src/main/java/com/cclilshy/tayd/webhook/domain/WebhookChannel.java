package com.cclilshy.tayd.webhook.domain;

import java.net.MalformedURLException;
import java.net.URL;

public final class WebhookChannel {
    private final String name;
    private final String targetUrl;
    private final String proxyUrl;

    public WebhookChannel(String name, String targetUrl, String proxyUrl) {
        this.name = requireName(name);
        this.targetUrl = requireHttpUrl(targetUrl, "targetUrl");
        this.proxyUrl = proxyUrl == null || proxyUrl.trim().isEmpty()
                ? ""
                : requireHttpUrl(proxyUrl, "proxyUrl");
    }

    public String getName() {
        return name;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    private static String requireName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.contains("\t") || trimmed.contains("\n")) {
            throw new IllegalArgumentException("invalid webhook channel name");
        }
        return trimmed;
    }

    private static String requireHttpUrl(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        try {
            URL url = new URL(trimmed);
            String protocol = url.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new IllegalArgumentException(field + " must be http or https");
            }
            if (url.getHost() == null || url.getHost().trim().isEmpty()) {
                throw new IllegalArgumentException(field + " host is required");
            }
            return trimmed;
        } catch (MalformedURLException err) {
            throw new IllegalArgumentException(field + " is invalid", err);
        }
    }
}
