package gamelibrary;

public enum Platform {
    PC("PC"),
    PLAYSTATION("PlayStation"),
    NINTENDO_SWITCH("Nintendo Switch"),
    XBOX("Xbox"),
    MOBILE("モバイル"),
    OTHER("その他");

    private final String displayName;

    Platform(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
