package gamelibrary;

public enum Genre {
    ACTION("アクション"),
    RPG("RPG"),
    ADVENTURE("アドベンチャー"),
    SIMULATION("シミュレーション"),
    STRATEGY("ストラテジー"),
    SPORTS("スポーツ"),
    PUZZLE("パズル"),
    OTHER("その他");

    private final String displayName;

    Genre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
