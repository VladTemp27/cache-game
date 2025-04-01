package org.amalzen.app;

public enum APIs {
    AUTH_URL("http://localhost/api/auth"),
    USER_URL("http://localhost/api/users"),
    MM_URL("ws://localhost/websoc/mm"),
//    GR_URL("ws://localhost/game/ws"); // ws://localhost/game/ws?gameId=abc&player=1&username=testUser1
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