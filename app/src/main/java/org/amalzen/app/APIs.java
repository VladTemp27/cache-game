package org.amalzen.app;

public enum APIs {
    AUTH_URL("http://%s/api/auth"),
    USER_URL("http://%s/api/users"),
    MM_URL("ws://%s/websoc/mm"),
    GR_URL("ws://%s:/game/ws");

    // change this to running server IP
    private static final String DEFAULT_HOST = "localhost";
    private final String urlTemplate;

    APIs(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getValue() {
        String envValue = System.getenv(this.name());
        if (envValue != null) {
            return envValue;
        }

        return String.format(urlTemplate, getHost());
    }

    private static String getHost() {
        String host = System.getenv("API_HOST");
        return host != null ? host : DEFAULT_HOST;
    }
}