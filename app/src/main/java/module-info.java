module org.amalzen.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires eu.hansolo.tilesfx;
    requires java.net.http;
    requires org.json;
    requires java.logging;
    requires javafx.media;
    requires java.desktop;

    opens org.amalzen.app to javafx.fxml;
    exports org.amalzen.app;

    opens org.amalzen.app.game_room to javafx.fxml;
    exports org.amalzen.app.game_room to javafx.graphics, javafx.fxml;

    exports org.amalzen.app.main_menu;
    opens org.amalzen.app.main_menu to javafx.fxml;

    exports org.amalzen.app.game_instructions;
    opens org.amalzen.app.game_instructions to javafx.fxml;

    exports org.amalzen.app.match_making;
    opens org.amalzen.app.match_making to javafx.fxml;

    exports org.amalzen.app.log_in;
    opens org.amalzen.app.log_in to javafx.fxml;

    opens org.amalzen.app.components to javafx.fxml;
    exports org.amalzen.app.components to javafx.graphics, javafx.fxml;

    opens org.amalzen.app.modals to javafx.fxml;
    exports org.amalzen.app.modals to javafx.graphics, javafx.fxml;

    opens org.amalzen.app.leaderboards to javafx.fxml;
    exports org.amalzen.app.leaderboards to javafx.graphics, javafx.fxml;
}