package com.cclilshy.locrelay.gateway.domain;

public final class GatewayExtensionSummary {
    private final String endpointSummary;
    private final boolean enabled;

    public GatewayExtensionSummary(String endpointSummary, boolean enabled) {
        this.endpointSummary = endpointSummary == null ? "" : endpointSummary;
        this.enabled = enabled;
    }

    public String getEndpointSummary() {
        return endpointSummary;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
