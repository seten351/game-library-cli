package gamelibrary;

import java.io.PrintStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

/** メニューと入力・表示を担当します。ゲームの処理は入力方式に依存しません。 */
public final class GameLibraryCli {
    private final GameLibraryService service;
    private final CliInput input;
    private final PrintStream output;

    public GameLibraryCli(GameLibraryService service, Scanner input, PrintStream output) {
        this(service, new PlainCliInput(input, output), output);
    }

    public GameLibraryCli(GameLibraryService service, CliInput input, PrintStream output) {
        this.service = Objects.requireNonNull(service);
        this.input = Objects.requireNonNull(input);
        this.output = Objects.requireNonNull(output);
    }

    public void run() {
        output.println("ゲームライブラリ管理CLI v2.0");
        output.println(service.isSavingEnabled()
                ? "登録完了ごとにJSONへ自動保存します。"
                : "保存無効：保存済みデータを読み込めていません。この実行中のデータだけを扱います。");
        while (true) {
            output.println("\n1. ゲームを登録");
            output.println("2. 一覧を表示");
            output.println("3. タイトルで検索");
            output.println("0. 終了");
            int choice = readChoice(0, 3, List.of(
                    new MenuOption(1, "ゲームを登録"), new MenuOption(2, "一覧を表示"),
                    new MenuOption(3, "タイトルで検索"), new MenuOption(0, "終了")));
            switch (choice) {
                case -1:
                    printInputEnd();
                    return;
                case 0:
                    printUnsavedWarning();
                    output.println("終了します。");
                    return;
                case 1:
                    if (!registerGame()) {
                        printInputEnd();
                        return;
                    }
                    break;
                case 2:
                    output.println("\nゲーム一覧");
                    printGames(service.getGames(), "ゲームはまだ登録されていません。");
                    break;
                case 3:
                    if (!searchGames()) {
                        printInputEnd();
                        return;
                    }
                    break;
                default:
                    throw new IllegalStateException("予期しないメニュー番号です。");
            }
        }
    }

    private boolean registerGame() {
        output.println("\nゲーム登録");
        String title = readRequiredText("タイトル: ");
        if (title == null) {
            return false;
        }
        Platform platform = readPlatform();
        if (platform == null) {
            return false;
        }
        Genre genre = readGenre();
        if (genre == null) {
            return false;
        }
        Double playHours = readPlayHours();
        if (playHours == null) {
            return false;
        }
        // 全項目の入力が完了した時点で、初めて登録します。
        try {
            boolean saved = service.registerGame(new Game(title, platform, genre, playHours));
            output.println(saved ? "ゲームを登録しました。JSONへ保存しました。"
                    : "ゲームを登録しました。保存無効のためメモリ内のみです。");
        } catch (IOException exception) {
            output.println("ゲームを登録しました。ただし未保存です。メモリ内の登録内容は保持しています。");
            output.println(exception.getMessage());
        }
        return true;
    }

    private boolean searchGames() {
        String keyword = readRequiredText("検索するタイトル: ");
        if (keyword == null) {
            return false;
        }
        output.println("\n検索結果");
        printGames(service.searchByTitle(keyword), "該当するゲームはありません。");
        return true;
    }

    private Platform readPlatform() {
        Platform[] platforms = Platform.values();
        List<MenuOption> options = new ArrayList<>();
        output.println("プラットフォームを選択してください。");
        for (int i = 0; i < platforms.length; i++) {
            output.println((i + 1) + ". " + platforms[i].getDisplayName());
            options.add(new MenuOption(i + 1, platforms[i].getDisplayName()));
        }
        int choice = readChoice(1, platforms.length, options);
        return choice == -1 ? null : platforms[choice - 1];
    }

    private Genre readGenre() {
        Genre[] genres = Genre.values();
        List<MenuOption> options = new ArrayList<>();
        output.println("ジャンルを選択してください。");
        for (int i = 0; i < genres.length; i++) {
            output.println((i + 1) + ". " + genres[i].getDisplayName());
            options.add(new MenuOption(i + 1, genres[i].getDisplayName()));
        }
        int choice = readChoice(1, genres.length, options);
        return choice == -1 ? null : genres[choice - 1];
    }

    private int readChoice(int minimum, int maximum, List<MenuOption> options) {
        while (true) {
            String value = input.readSelection("番号を入力: ", options);
            if (value == null) {
                return -1;
            }
            try {
                int choice = Integer.parseInt(value.strip());
                if (choice >= minimum && choice <= maximum) {
                    return choice;
                }
            } catch (NumberFormatException exception) {
                // 数字以外の場合も、以下の案内を出して再入力します。
            }
            output.println(minimum + "から" + maximum + "までの整数を入力してください。");
        }
    }

    private String readRequiredText(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value == null || !value.isBlank()) {
                return value;
            }
            output.println("空欄にはできません。文字を入力してください。");
        }
    }

    private Double readPlayHours() {
        while (true) {
            String value = readLine("プレイ時間（時間、例: 1.5）: ");
            if (value == null) {
                return null;
            }
            try {
                double hours = Double.parseDouble(value);
                if (Double.isFinite(hours) && hours >= 0) {
                    return hours;
                }
            } catch (NumberFormatException exception) {
                // 変換できない入力も、以下の案内を出して再入力します。
            }
            output.println("プレイ時間は0以上の有限の数値で入力してください。");
        }
    }

    private String readLine(String prompt) {
        String value = input.readLine(prompt);
        return value == null ? null : value.strip();
    }

    private void printGames(List<Game> games, String emptyMessage) {
        if (games.isEmpty()) {
            output.println(emptyMessage);
            return;
        }
        for (int i = 0; i < games.size(); i++) {
            Game game = games.get(i);
            output.printf(Locale.ROOT,
                    "[%d] タイトル: %s | プラットフォーム: %s | ジャンル: %s | プレイ時間: %s 時間%n",
                    i + 1, game.getTitle(), game.getPlatform().getDisplayName(),
                    game.getGenre().getDisplayName(), Double.toString(game.getPlayHours()));
        }
    }

    private void printInputEnd() {
        printUnsavedWarning();
        output.println("\n入力が終了したため、終了します。");
    }

    private void printUnsavedWarning() {
        if (service.hasUnsavedChanges()) {
            output.println("未保存のデータがあります。終了すると未保存分は失われます。");
        }
    }
}
