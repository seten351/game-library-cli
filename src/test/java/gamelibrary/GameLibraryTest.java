package gamelibrary;

import java.util.List;
import java.util.Locale;

/** 外部のテストライブラリを使わない、モデルとデータ管理のテスト。 */
public final class GameLibraryTest {
    private static int checks;

    private GameLibraryTest() {
    }

    public static void main(String[] args) {
        testGame();
        testRegistrationAndCopies();
        testSearch();
        System.out.println("GameLibraryTest: " + checks + " checks passed");
    }

    private static void testGame() {
        Game game = new Game("　 Echo \t", Platform.PC, Genre.RPG, 1.5);
        check(game.getTitle().equals("Echo"), "タイトルの前後の空白を除去");
        check(game.getPlatform() == Platform.PC, "プラットフォームを保持");
        check(game.getGenre() == Genre.RPG, "ジャンルを保持");
        check(game.getPlayHours() == 1.5, "小数の時間を保持");
        check(new Game("未プレイ", Platform.OTHER, Genre.OTHER, 0).getPlayHours() == 0,
                "0時間を許可");
        check(new Game("長時間", Platform.PC, Genre.RPG, Double.MAX_VALUE).getPlayHours()
                == Double.MAX_VALUE, "有限の時間を許可");

        expectInvalid(() -> new Game(null, Platform.PC, Genre.RPG, 0), "nullタイトル");
        expectInvalid(() -> new Game("", Platform.PC, Genre.RPG, 0), "空タイトル");
        expectInvalid(() -> new Game(" \t　", Platform.PC, Genre.RPG, 0), "空白タイトル");
        expectInvalid(() -> new Game("Echo", null, Genre.RPG, 0), "nullプラットフォーム");
        expectInvalid(() -> new Game("Echo", Platform.PC, null, 0), "nullジャンル");
        double[] invalidHours = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double hours : invalidHours) {
            expectInvalid(() -> new Game("Echo", Platform.PC, Genre.RPG, hours),
                    "不正なプレイ時間: " + hours);
        }
    }

    private static void testRegistrationAndCopies() {
        GameLibrary library = new GameLibrary();
        check(library.getGames().isEmpty(), "開始時は空");
        Game first = new Game("Echo", Platform.PC, Genre.RPG, 1.5);
        Game second = new Game("Echo", Platform.NINTENDO_SWITCH, Genre.ACTION, 0);
        library.addGame(first);
        List<Game> snapshot = library.getGames();
        library.addGame(second);
        List<Game> games = library.getGames();
        check(games.size() == 2, "同じタイトルを登録可能");
        check(games.get(0) == first && games.get(1) == second, "登録順を維持");
        check(snapshot.size() == 1, "取得した一覧は後の登録で変化しない");
        games.clear();
        games.add(new Game("別のゲーム", Platform.PC, Genre.PUZZLE, 2));
        check(library.getGames().size() == 2 && library.getGames().get(0) == first,
                "返却された一覧の変更は登録内容に影響しない");
        expectInvalid(() -> library.addGame(null), "nullゲームを拒否");
        check(library.getGames().size() == 2, "登録失敗時は内容を維持");
    }

    private static void testSearch() {
        GameLibrary library = new GameLibrary();
        check(library.searchByTitle("Echo").isEmpty(), "空のライブラリを検索");
        Game first = new Game("Echo Adventure", Platform.PC, Genre.RPG, 1);
        Game second = new Game("echo", Platform.PLAYSTATION, Genre.ACTION, 0);
        Game third = new Game("ゼルダの伝説", Platform.NINTENDO_SWITCH, Genre.ADVENTURE, 2);
        library.addGame(first);
        library.addGame(second);
        library.addGame(third);

        List<Game> results = library.searchByTitle("　EcH ");
        check(results.size() == 2, "空白を除去し、大文字・小文字を無視して部分一致");
        check(results.get(0) == first && results.get(1) == second, "検索結果は登録順");
        check(library.searchByTitle("Echo Adventure").size() == 1, "タイトル全体でも検索可能");
        check(library.searchByTitle("伝説").get(0) == third, "日本語の部分一致");
        check(library.searchByTitle("missing").isEmpty(), "該当なし");
        check(library.searchByTitle("PC").isEmpty(), "タイトル以外は検索しない");
        results.clear();
        check(library.searchByTitle("echo").size() == 2, "検索結果の変更は登録内容に影響しない");
        expectInvalid(() -> library.searchByTitle(null), "null検索語");
        expectInvalid(() -> library.searchByTitle(""), "空検索語");
        expectInvalid(() -> library.searchByTitle("　 \t"), "空白検索語");

        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            library.addGame(new Game("INDIE", Platform.PC, Genre.OTHER, 0));
            check(library.searchByTitle("indie").size() == 1,
                    "実行環境の言語設定に左右されない検索");
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    private static void expectInvalid(Runnable action, String description) {
        try {
            action.run();
        } catch (IllegalArgumentException exception) {
            checks++;
            return;
        }
        throw new AssertionError(description + ": IllegalArgumentExceptionが発生しませんでした。");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }
}
