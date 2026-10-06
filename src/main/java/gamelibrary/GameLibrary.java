package gamelibrary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** コンソール入出力に依存しない、登録・一覧・検索の処理。 */
public final class GameLibrary {
    private final List<Game> games = new ArrayList<>();

    public void addGame(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("ゲームを指定してください。");
        }
        games.add(game);
    }

    public List<Game> getGames() {
        // 呼び出し側がListを変更しても、登録内容には影響しません。
        return new ArrayList<>(games);
    }

    public List<Game> searchByTitle(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("検索するタイトルを入力してください。");
        }
        String normalizedKeyword = keyword.strip().toLowerCase(Locale.ROOT);
        List<Game> results = new ArrayList<>();
        for (Game game : games) {
            if (game.getTitle().toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                results.add(game);
            }
        }
        return results;
    }
}
