package gamelibrary;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/** 入力と出力をメモリ内で用意して、CLIの操作を検証します。 */
public final class GameLibraryCliTest {
    private static int checks;

    private GameLibraryCliTest() {
    }

    public static void main(String[] args) {
        testNormalOperations();
        testInvalidInputRecovery();
        testJapaneseTitleAndLastChoices();
        testInputEnd();
        System.out.println("GameLibraryCliTest: " + checks + " checks passed");
    }

    private static void testNormalOperations() {
        GameLibrary library = new GameLibrary();
        String output = runCli(library,
                "2\n3\nmissing\n1\nEcho\n1\n2\n1.5\n"
                + "1\nEcho\n3\n1\n0\n2\n3\neCH\n3\nunknown\n0\n");
        List<Game> games = library.getGames();
        check(games.size() == 2, "重複タイトルを含めて2件登録");
        check(games.get(0).getPlatform() == Platform.PC
                && games.get(1).getPlatform() == Platform.NINTENDO_SWITCH, "登録順を維持");
        check(output.contains("ゲームはまだ登録されていません。"), "空の一覧の案内");
        check(output.contains("該当するゲームはありません。"), "該当なしの案内");
        String firstRow = "[1] タイトル: Echo | プラットフォーム: PC | ジャンル: RPG | プレイ時間: 1.5 時間";
        String secondRow = "[2] タイトル: Echo | プラットフォーム: Nintendo Switch | ジャンル: アクション | プレイ時間: 0.0 時間";
        check(output.contains(firstRow) && output.contains(secondRow), "4項目を表示");
        check(output.indexOf(firstRow) < output.indexOf(secondRow), "一覧の表示順");
        check(output.contains("検索結果" + System.lineSeparator() + firstRow
                + System.lineSeparator() + secondRow), "検索結果に2件を登録順で表示");
        check(output.endsWith("終了します。" + System.lineSeparator()), "0で終了");
        check(!output.contains("入力が終了したため"), "正常終了と入力終端を区別");
    }

    private static void testInvalidInputRecovery() {
        GameLibrary library = new GameLibrary();
        String output = runCli(library,
                "abc\n-1\n5\n1.5\n999999999999\n1\n \n　\n Valid \n"
                + "0\n7\nbad\n1\n0\n9\nbad\n2\n"
                + "\noops\n-1\nNaN\nInfinity\n-Infinity\n1e309\n0\n"
                + "3\n \n　\n vAl \n0\n");
        List<Game> games = library.getGames();
        check(games.size() == 1, "不正入力で重複登録しない");
        Game game = games.get(0);
        check(game.getTitle().equals("Valid"), "空白タイトルを再入力し、前後空白を除去");
        check(game.getPlatform() == Platform.PC && game.getGenre() == Genre.RPG,
                "不正な選択後に正しい選択を登録");
        check(game.getPlayHours() == 0, "不正な時間の再入力後に0時間を登録");
        check(output.contains("0から4までの整数を入力してください。"), "不正メニューの案内");
        check(output.contains("1から6までの整数を入力してください。"), "不正プラットフォームの案内");
        check(output.contains("1から8までの整数を入力してください。"), "不正ジャンルの案内");
        check(output.contains("空欄にはできません。"), "空文字の案内");
        check(output.contains("プレイ時間は0以上の有限の数値で入力してください。"), "不正な時間の案内");
        check(output.contains("検索結果" + System.lineSeparator() + "[1] タイトル: Valid"),
                "空の検索語を再入力した後に検索成功");
    }

    private static void testJapaneseTitleAndLastChoices() {
        GameLibrary library = new GameLibrary();
        String output = runCli(library, "1\n　ゼルダの伝説　\n3\n3\n0.25\n3\n伝説\n"
                + "1\nOther game\n6\n8\n0\n2\n0\n");
        check(library.getGames().get(0).getTitle().equals("ゼルダの伝説"), "日本語タイトルと全角空白");
        check(output.contains("検索結果" + System.lineSeparator() + "[1] タイトル: ゼルダの伝説"),
                "日本語検索が成功");
        Game other = library.getGames().get(1);
        check(other.getPlatform() == Platform.OTHER && other.getGenre() == Genre.OTHER,
                "最後の選択肢を選択可能");
        check(output.contains("プラットフォーム: その他 | ジャンル: その他"), "その他を表示");
    }

    private static void testInputEnd() {
        String[] incompleteInputs = {
            "", "bad\n", "1\n", "1\n\n", "1\nGame\n", "1\nGame\n9\n",
            "1\nGame\n1\n", "1\nGame\n1\n99\n", "1\nGame\n1\n2\n",
            "1\nGame\n1\n2\n-1\n", "3\n", "3\n\n"
        };
        for (String input : incompleteInputs) {
            GameLibrary library = new GameLibrary();
            String output = runCli(library, input);
            check(library.getGames().isEmpty(), "入力終端で途中の登録を追加しない");
            check(output.contains("入力が終了したため、終了します。"), "入力終端で正常終了");
        }
        GameLibrary library = new GameLibrary();
        runCli(library, "1\nSaved\n1\n2\n2\n1\nPartial\n1\n");
        check(library.getGames().size() == 1
                && library.getGames().get(0).getTitle().equals("Saved"),
                "入力終端でも、それ以前に完了した登録は維持");
    }

    private static String runCli(GameLibrary library, String text) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Scanner input = new Scanner(text);
                PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            GameStore store = new GameStore() {
                public List<Game> load() { return List.of(); }
                public void save(List<Game> games) { /* 既存CLIテストはファイルに書かない。 */ }
            };
            new GameLibraryCli(new GameLibraryService(library, store), input, output).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }
}
