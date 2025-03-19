package org.amalzen.app.main_menu;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainmenuView extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainmenuView.class.getResource("/org/amalzen/app/view/mainmenu-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1650, 944);
        stage.setTitle("Main Menu");
        stage.setScene(scene);
        stage.show();
        stage.setResizable(false);
    }

    public static void main(String[] args) {
        launch();
    }
}
