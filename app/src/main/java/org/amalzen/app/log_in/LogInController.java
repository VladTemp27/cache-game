package org.amalzen.app.log_in;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;
import org.amalzen.app.util.SessionStorage;

public class LogInController {

    @FXML
    private Button loginButton;
    @FXML
    private TextField usernameTextfield;
    @FXML
    private PasswordField passwordField;

    private final LoginModel loginModel = new LoginModel();

    public void initialize() {

        loginButton.setOnMouseClicked(event -> {
            String username = usernameTextfield.getText();
            String password = passwordField.getText();

            try {
                String sessionId = loginModel.authenticate(username, password);
                if (sessionId != null) {
                    // Store session data
                    SessionStorage.set("sessionId", sessionId);
                    SessionStorage.set("username", username);

                    // Navigate to main menu
                    Main.ChangeScene(ResourcePath.MAIN_MENU.getPath());
                } else {
                    showError("Invalid credentials");
                }
            } catch (Exception e) {
                showError("Login failed: " + e.getMessage());
            }
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

    }
}