package org.amalzen.app;

public enum ResourcePath {
    GAME_ICON("images/game-logo.png"),
    LOGIN("view/login-view.fxml"),
    MAIN_MENU("view/mainmenu-view.fxml"),
    LEADERBOARD("view/leaderboard-view.fxml"),
    EXIT("view/exit-modal.fxml"),
    HISTORY("view/match-history.fxml"),
    INSTRUCTION("view/game-instructions.fxml"),
    MATCHMAKING("view/matchmaking-view.fxml"),
    GAME_ROOM("view/game-room.fxml"),
    CARD("view/card.fxml"),
    LEADERBOARD_COMPONENT("view/leaderboard-component.fxml"),
    GAME_OVER_MODAL("view/game-over-modal.fxml"),
    EXIT_MODAL("view/exit-modal.fxml"),
    VICTORY_MODAL("view/victory-modal.fxml"),
    DEFEAT_MODAL("view/defeat-modal.fxml"),
    LOGOUT_MODAL("view/logout-modal.fxml"),

    FLIP_CARD_EFFECT("audio/effects/flip-card-effect.mp3"),
    CORRECT_PAIR_EFFECT("audio/effects/correct-pair-effect.mp3"),
    INCORRECT_PAIR_EFFECT("audio/effects/incorrect-pair-effect.mp3"),
    VICTORY_SOUND("audio/effects/victory-sound.mp3"),
    DEFEAT_SOUND("audio/effects/defeat-sound.mp3"),

    MATCHMAKING_MUSIC("audio/music/matchmaking-music.wav"),
    GAME_ROOM_MUSIC("audio/music/game-room-music.wav"),
    MAIN_MENU_MUSIC("audio/music/main-menu-music.wav");





    private final String path;

    ResourcePath(String path) {
        this.path = "/org/amalzen/app/" + path;
    }

    public String getPath() {
        return path;
    }
}