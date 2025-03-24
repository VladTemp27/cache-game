package org.amalzen.app.modals;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import org.amalzen.app.APIs;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LogoutModalController {
    private static final String AUTH_API_URL = APIs.AUTH_URL.getValue() + "/logout";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @FXML
    private Button cancelLogoutButton;

    @FXML
    private Button confirmLogoutButton;

    @FXML
    private AnchorPane rootLogoutModalPane;

    private static void handle(ActionEvent event) {
        try {
            String sessionId = Main.sessionId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AUTH_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", sessionId)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Main.sessionId = null;
                Main.username = null;
                Main.ChangeScene(ResourcePath.LOGIN.getPath());
            } else {
                System.err.println("Logout failed: " + response.body());
                // Force Logout due to session storage
                Main.ChangeScene(ResourcePath.LOGIN.getPath());

            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Logout failed: " + e.getMessage());
        }
    }

    public void initialize() {
        cancelLogoutButton.setOnAction(event -> rootLogoutModalPane.setVisible(false));
        confirmLogoutButton.setOnAction(LogoutModalController::handle);
    }
}
