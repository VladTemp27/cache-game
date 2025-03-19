package org.amalzen.app.log_in;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

import java.io.IOException;

public class LogInController {

    @FXML
    private Button loginButton;
    @FXML
    private TextField usernameTextfield;
    @FXML
    private TextField passwordTextfield;


    // TODO: Insert fxml file paths of corresponding views and authentication
    public void initialize() {
        loginButton.setOnMouseClicked(event -> {
            System.out.println("Login button clicked.");
            String username = usernameTextfield.getText();
            String password = passwordTextfield.getText();

            if (authenticateUser(username, password)) {
                Main.ChangeScene(ResourcePath.MAIN_MENU.getPath()); // Navigate to main menu
            } else {
                System.out.println("Invalid credentials.");
            }
        });
    }

    // TODO: Replace with actual authenticator made by Mr. Rabang
    private boolean authenticateUser(String username, String password) {
        return "admin".equals(username) && "password".equals(password);
    }
}
