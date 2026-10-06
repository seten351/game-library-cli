package gamelibrary;

/** ゲーム1件の情報。作成後は値を変更しません。 */
public final class Game {
    private final String title;
    private final Platform platform;
    private final Genre genre;
    private final double playHours;

    public Game(String title, Platform platform, Genre genre, double playHours) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("タイトルを入力してください。");
        }
        if (platform == null || genre == null) {
            throw new IllegalArgumentException("プラットフォームとジャンルは必須です。");
        }
        if (!Double.isFinite(playHours) || playHours < 0) {
            throw new IllegalArgumentException("プレイ時間は0以上の有限の数値にしてください。");
        }
        this.title = title.strip();
        this.platform = platform;
        this.genre = genre;
        this.playHours = playHours;
    }

    public String getTitle() {
        return title;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Genre getGenre() {
        return genre;
    }

    public double getPlayHours() {
        return playHours;
    }
}
