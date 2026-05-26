package com.cclilshy.locrelay.proxy.domain;

public final class ProxyAuth {
    private static final ProxyAuth DISABLED = new ProxyAuth("", "");
    private final String username;
    private final String password;

    private ProxyAuth(String username, String password) {
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
    }

    public static ProxyAuth disabled() {
        return DISABLED;
    }

    public static ProxyAuth from(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return DISABLED;
        }
        return new ProxyAuth(username.trim(), password == null ? "" : password);
    }

    public boolean isRequired() {
        return !username.isEmpty();
    }

    public boolean matches(String candidateUsername, String candidatePassword) {
        return username.equals(candidateUsername) && password.equals(candidatePassword);
    }
}
