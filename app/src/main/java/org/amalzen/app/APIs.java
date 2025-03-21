package org.amalzen.app;

public enum APIs {
    AUTH_URL("http://localhost/api/auth"),
    MM_URL("ws://localhost/websoc/mm"),
    GR_URL("ws://localhost:8082/ws");

    private final String defaultValue;

    APIs(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        String envValue = System.getenv(this.name());
        return envValue != null ? envValue : defaultValue;
    }
}