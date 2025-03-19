module org.amalzen.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires eu.hansolo.tilesfx;

    opens org.amalzen.app to javafx.fxml;
    exports org.amalzen.app;

    opens org.amalzen.app.match_history to javafx.fxml;
    exports org.amalzen.app.match_history to javafx.graphics, javafx.fxml;

    opens org.amalzen.app.game_room to javafx.fxml;
    exports org.amalzen.app.game_room to javafx.graphics, javafx.fxml;

    opens org.amalzen.app.components to javafx.fxml;
    exports org.amalzen.app.components to javafx.graphics, javafx.fxml;
}
