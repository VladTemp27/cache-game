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

    exports org.amalzen.app.main_menu;
    opens org.amalzen.app.main_menu to javafx.fxml;
    exports org.amalzen.app.game_instructions;
    opens org.amalzen.app.game_instructions to javafx.fxml;

    opens org.amalzen.app.game_room to javafx.fxml;
    exports org.amalzen.app.game_room to javafx.graphics, javafx.fxml;

    opens org.amalzen.app.modals to javafx.fxml;
    exports org.amalzen.app.modals to javafx.graphics, javafx.fxml;
}