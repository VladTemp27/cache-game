package org.amalzen.app;

public enum ResourcePath {
    LOGIN("view/login-view.fxml"),
    MAIN_MENU("view/mainmenu-view.fxml"),
    EXIT("view/exit-modal.fxml"),
    HISTORY("view/match-history.fxml"),
    INSTRUCTION("view/game-instructions.fxml"),
    MATCHMAKING("view/matchmaking-view.fxml"),
    GAME_ROOM("view/game-room.fxml"),
    CARD("view/card.fxml");

    private final String path;

    ResourcePath(String path) {
        this.path = "/org/amalzen/app/" + path;
    }

    public String getPath() {
        return path;
    }
}