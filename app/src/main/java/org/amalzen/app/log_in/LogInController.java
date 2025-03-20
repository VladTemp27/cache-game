package org.amalzen.app.log_in;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

public class LogInController {

    @FXML
    private Button loginButton;
    @FXML
    private TextField usernameTextfield;
    @FXML
    private PasswordField passwordField;


    // TODO: Insert fxml file paths of corresponding views and authentication
    public void initialize() {
        loginButton.setOnMouseClicked(event -> {
            String username = usernameTextfield.getText();
            String password = passwordField.getText();

            if (authenticateUser(username, password)) {
                Main.ChangeScene(ResourcePath.MAIN_MENU.getPath()); // Navigate to main menu
            } else {
                System.out.println("Invalid credentials."); // TODO: Replace with GUI error message
            }
        });
    }

    // TODO: Replace with API Request
    private boolean authenticateUser(String username, String password) {
        return "admin".equals(username) && "password".equals(password);
    }
}
