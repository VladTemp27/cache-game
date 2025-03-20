package org.amalzen.app.log_in;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LoginModel {
    private static final String AUTH_API_URL = "http://localhost:8080/auth/login";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String authenticate(String username, String password) throws Exception {
        // Create JSON payload
        JSONObject requestBody = new JSONObject();
        requestBody.put("username", username);
        requestBody.put("password", password);

        // Build request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        // Send request and get response
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        // Check response status
        if (response.statusCode() ==
 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            return jsonResponse.getString("sessionId");
        }

        return null;
    }
}