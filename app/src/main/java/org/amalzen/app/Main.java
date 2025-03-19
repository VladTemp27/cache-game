package org.amalzen.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;

import org.amalzen.app.log_in.LogInController;
import org.amalzen.app.main_menu.MainmenuController;
import org.amalzen.app.game_instructions.GameInstructionsController;
import org.amalzen.app.match_making.MatchmakingController;
import org.amalzen.app.game_room.GameRoomController;

public class Main extends Application {
    public static Scene scene;
    public static FXMLLoader fxmlLoader;
    public static Stage primaryStage;
    @Override
    public void start(Stage primaryStage) {
        Main.primaryStage = primaryStage;
//        loadControllers().forEach(controller -> {});
        ChangeScene(ResourcePath.MAIN_MENU.getPath());
        primaryStage.setTitle("CACHE");
        primaryStage.setResizable(false);   
        primaryStage.show();
    }

    public ArrayList<Class<?>> loadControllers() {
        ArrayList<Class<?>> controllers = new ArrayList<>();
//        controllers.add(LogInController.class);
        controllers.add(MainmenuController.class);
        controllers.add(GameInstructionsController.class);
        controllers.add(MatchmakingController.class);
        controllers.add(GameRoomController.class);
        System.out.println(("Controllers: " + controllers));
        return controllers;
    }

    public static void ChangeScene(String path) {
        fxmlLoader = new FXMLLoader(Main.class.getResource(path));
        try {
            scene = new Scene(fxmlLoader.load());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Scene Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}