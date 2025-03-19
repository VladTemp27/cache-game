package org.amalzen.app.log_in;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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
            String username = usernameTextfield.getText();
            String password = passwordTextfield.getText();

            if (authenticateUser(username, password)) {
                navigateToView(""); //TODO: replace with main menu fxml once authenticator returns true
            } else {
                System.out.println("Invalid credentials.");
            }
        });
    }
    // TODO: Replace with actual authenticator made by Mr. Rabang
    private boolean authenticateUser(String username, String password) {
        return "admin".equals(username) && "password".equals(password);
    }

    private void navigateToView(String file) {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(file));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
