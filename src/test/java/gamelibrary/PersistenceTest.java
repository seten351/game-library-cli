package gamelibrary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Scanner;

/** 実データを使わず、JSONの往復と保存失敗時の連携を確認します。 */
public final class PersistenceTest {
    private static int checks;

    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("game-library-v2-java-");
        try {
            testRoundTrip(directory);
            testAtomicMoveFailure(directory);
            testResolvedPaths(directory);
            testRetryWithoutRegistration(directory);
            testExitSaveFailureReturnsToMenu();
            testInvalidDocuments(directory);
            testService(directory);
            testFailures(directory);
            System.out.println("PersistenceTest: " + checks + " checks passed");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void testAtomicMoveFailure(Path directory) throws IOException {
        Path path = directory.resolve("atomic-failure.json");
        byte[] original = "existing data must survive".getBytes(StandardCharsets.UTF_8);
        Files.write(path, original);
        boolean posix = Files.getFileAttributeView(path, PosixFileAttributeView.class) != null;
        Set<PosixFilePermission> permissions = null;
        if (posix) {
            permissions = Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, permissions);
        }
        Path[] temporary = {null};
        JsonGameStore store = new JsonGameStore(path, (source, target) -> {
            temporary[0] = source;
            throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), "test");
        });
        expectIOException(() -> store.save(List.of(new Game("Replacement", Platform.PC, Genre.RPG, 1))));
        check(java.util.Arrays.equals(original, Files.readAllBytes(path)),
                "atomic move非対応でも元ファイルを維持");
        check(temporary[0] != null && !Files.exists(temporary[0]),
                "atomic move失敗後に注入元の一時ファイルを削除");
        assertNoStoreTemporaryFiles(directory, "atomic move失敗後");
        new JsonGameStore(path).save(List.of(new Game("Replacement", Platform.PC, Genre.RPG, 2)));
        if (posix) {
            check(Files.getPosixFilePermissions(path).equals(permissions),
                    "置換後も既存ファイルのPOSIX権限を維持");
        }

        Path missing = directory.resolve("first-save-failure.json");
        Path[] missingTemporary = {null};
        JsonGameStore genericFailure = new JsonGameStore(missing, (source, target) -> {
            missingTemporary[0] = source;
            throw new IOException("generic move failure");
        });
        expectIOException(() -> genericFailure.save(List.of(new Game("First", Platform.PC, Genre.RPG, 1))));
        check(!Files.exists(missing) && missingTemporary[0] != null && !Files.exists(missingTemporary[0]),
                "初回の置換失敗ではデータファイルを作らず一時ファイルも削除");
        Path unsupported = directory.resolve("unsupported-move.json");
        JsonGameStore unsupportedFailure = new JsonGameStore(unsupported, (source, target) -> {
            throw new UnsupportedOperationException("test");
        });
        expectIOException(() -> unsupportedFailure.save(List.of(new Game("First", Platform.PC, Genre.RPG, 1))));
        check(!Files.exists(unsupported), "UnsupportedOperationExceptionをIOExceptionへ変換");
        assertNoStoreTemporaryFiles(directory, "置換失敗全体の後片付け");
    }

    private static void assertNoStoreTemporaryFiles(Path directory, String message) throws IOException {
        try (var entries = Files.list(directory)) {
            check(entries.noneMatch(entry -> entry.getFileName().toString().startsWith(".game-library-")
                    && entry.getFileName().toString().endsWith(".tmp")), message + "に一時ファイルを残さない");
        }
    }

    private static void testResolvedPaths(Path directory) throws IOException {
        Path realDirectory = Files.createDirectory(directory.resolve("resolved-real"));
        Path data = realDirectory.resolve("games.json");
        Path alias = directory.resolve("resolved-alias");
        try {
            Files.createSymbolicLink(alias, realDirectory);
        } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
            Path parent = directory.resolve("new-parent");
            Path resolved = GameFileLock.resolveDataPath(parent.resolve("games.json"));
            check(resolved.equals(parent.toRealPath().resolve("games.json")),
                    "欠損データの親ディレクトリを作り実パスへ解決");
            return;
        }
        Path realData = realDirectory.toRealPath().resolve("games.json");
        check(GameFileLock.resolveDataPath(alias.resolve("games.json")).equals(realData),
                "シンボリックリンク経由のデータパスを実パスへ解決");
        try (GameFileLock ignored = GameFileLock.acquire(GameFileLock.resolveDataPath(alias.resolve("games.json")))) {
            expectIOException(() -> GameFileLock.acquire(GameFileLock.resolveDataPath(data)));
        }
        try (GameFileLock ignored = GameFileLock.acquire(GameFileLock.resolveDataPath(realData))) {
            check(true, "ロック解放後は再取得でき、残ったlockファイルを許容");
        }
        Path dangling = directory.resolve("dangling-data.json");
        Files.createSymbolicLink(dangling, directory.resolve("missing-target.json"));
        expectIOException(() -> GameFileLock.resolveDataPath(dangling));
        check(!Files.exists(data), "パス解決だけではデータファイルを作らない");
    }

    private static void testRetryWithoutRegistration(Path directory) throws IOException {
        Path path = directory.resolve("retry.json");
        int[] saves = {0};
        GameStore store = new GameStore() {
            private List<Game> saved = List.of();

            @Override
            public List<Game> load() {
                return saved;
            }

            @Override
            public void save(List<Game> games) throws IOException {
                saves[0]++;
                if (saves[0] == 1) {
                    throw new IOException("transient failure");
                }
                saved = List.copyOf(games);
            }
        };
        GameLibraryService service = new GameLibraryService(new GameLibrary(), store);
        service.load();
        try {
            service.registerGame(new Game("Retry me", Platform.PC, Genre.RPG, 1));
            throw new AssertionError("初回保存失敗を通知する必要があります。");
        } catch (IOException expected) {
            checks++;
        }
        String output = runCli(service, "4\n0\n");
        check(saves[0] == 2 && service.getGames().size() == 1,
                "保存再試行は新規登録せず既存の未保存一覧を1回保存");
        check(!service.hasUnsavedChanges() && output.contains("未保存のデータをJSONへ保存しました。"),
                "再試行成功後に未保存状態を解除");
        String cleanRetryOutput = runCli(service, "4\n0\n");
        check(saves[0] == 2 && cleanRetryOutput.contains("すべて保存済みです。"),
                "clean状態の再試行は追加のsave IOをしない");
    }

    private static void testExitSaveFailureReturnsToMenu() throws IOException {
        int[] saves = {0};
        GameLibraryService service = new GameLibraryService(new GameLibrary(), new GameStore() {
            @Override
            public List<Game> load() {
                return List.of();
            }

            @Override
            public void save(List<Game> games) throws IOException {
                saves[0]++;
                throw new IOException("persistent failure");
            }
        });
        service.load();
        try {
            service.registerGame(new Game("Keep in memory", Platform.PC, Genre.RPG, 1));
            throw new AssertionError("初回保存失敗を通知する必要があります。");
        } catch (IOException expected) {
            checks++;
        }
        String output = runCli(service, "0\n1\n0\n2\n");
        check(saves[0] == 2 && service.hasUnsavedChanges(), "終了時の保存失敗で未保存状態を維持");
        check(output.contains("保存を再試行しましたが、まだ未保存です。")
                && output.split("1\\. ゲームを登録", -1).length >= 3,
                "終了時の保存失敗は破棄せずメインメニューへ戻る");
    }

    private static void testRoundTrip(Path directory) throws IOException {
        Path path = directory.resolve("日本語 フォルダ/games.json");
        JsonGameStore store = new JsonGameStore(path);
        check(store.load().isEmpty(), "初回は空");
        check(!Files.exists(path.getParent()), "読み込みだけではディレクトリを作らない");
        List<Game> games = new ArrayList<>();
        games.add(new Game("Echo", Platform.PC, Genre.RPG, 0));
        games.add(new Game("Echo", Platform.PC, Genre.RPG, 1.5));
        for (Platform platform : Platform.values()) {
            for (Genre genre : Genre.values()) {
                games.add(new Game("ゼルダ \"\\!\n" + platform.name() + genre.name(), platform, genre, 0.25));
            }
        }
        store.save(games);
        List<Game> loaded = store.load();
        check(loaded.size() == games.size(), "登録数と重複を維持");
        for (int i = 0; i < games.size(); i++) {
            Game expected = games.get(i);
            Game actual = loaded.get(i);
            check(actual.getTitle().equals(expected.getTitle()) && actual.getPlatform() == expected.getPlatform()
                    && actual.getGenre() == expected.getGenre() && actual.getPlayHours() == expected.getPlayHours(),
                    "全enum・日本語・記号・小数・登録順の往復");
        }
        String json = Files.readString(path);
        check(json.contains("\"schemaVersion\": 1") && json.contains("\"platform\": \"PC\""),
                "保存形式とenum定数名");
        store.save(List.of());
        check(store.load().isEmpty(), "明示的な空List保存を読み込める");
    }

    private static void testInvalidDocuments(Path directory) throws IOException {
        String entry = "{\"title\":\"Echo\",\"platform\":\"PC\",\"genre\":\"RPG\",\"playHours\":1.5}";
        List<String> invalid = new ArrayList<>(List.of("", "null", "[]", "{}", "broken",
                "{\"schemaVersion\":2,\"games\":[]}", "{\"schemaVersion\":1.5,\"games\":[]}",
                "{\"schemaVersion\":\"1\",\"games\":[]}", "{\"schemaVersion\":1,\"games\":null}",
                "{\"schemaVersion\":1,\"games\":[null]}", "{schemaVersion:1,games:[]}",
                "{\"schemaVersion\":1,\"games\":[]} trailing",
                "{\"schemaVersion\":1,\"games\":[]} {}"));
        for (String invalidEntry : List.of(entry.replace("\"Echo\"", "null"),
                entry.replace("\"Echo\"", "\" \""), entry.replace("\"Echo\"", "5"),
                entry.replace("\"PC\"", "\"unknown\""), entry.replace("\"RPG\"", "null"),
                entry.replace("1.5", "-1"), entry.replace("1.5", "1e309"),
                entry.replace("1.5", "\"1.5\""), entry.replace(",\"playHours\":1.5", ""))) {
            // 先頭は正常でも、全体を採用しないことを確認します。
            invalid.add("{\"schemaVersion\":1,\"games\":[" + entry + "," + invalidEntry + "]}");
        }
        Path path = directory.resolve("invalid.json");
        for (String json : invalid) {
            Files.writeString(path, json);
            GameLibraryService service = new GameLibraryService(new GameLibrary(), new JsonGameStore(path));
            expectIOException(service::load);
            check(service.getGames().isEmpty() && !service.isSavingEnabled(), "部分復元せず保存無効");
            check(!service.registerGame(new Game("Memory", Platform.PC, Genre.RPG, 1)), "失敗後はメモリ専用");
            check(!service.savePending(), "保存無効のdirty状態では再保存できない");
            check(Files.readString(path).equals(json), "壊れた元ファイルを上書きしない");
        }
    }

    private static void testService(Path directory) throws IOException {
        Path path = directory.resolve("service.json");
        GameLibraryService first = new GameLibraryService(new GameLibrary(), new JsonGameStore(path));
        first.load();
        check(!Files.exists(path), "起動だけでは空JSONを作らない");
        first.registerGame(new Game("Echo", Platform.PC, Genre.RPG, 1.5));
        check(Files.exists(path) && !first.hasUnsavedChanges(), "登録直後に自動保存");
        GameLibraryService second = new GameLibraryService(new GameLibrary(), new JsonGameStore(path));
        second.load();
        check(second.searchByTitle("eCH").size() == 1, "別サービスで復元と検索");
        check(second.savePending(), "clean状態の保存要求はIOなしで成功");
        byte[] before = Files.readAllBytes(path);
        String output = runCli(second, "2\n3\nech\n1\nPartial\n1\n");
        check(output.contains("Echo") && output.contains("入力が終了したため"), "一覧・検索・途中EOF");
        check(java.util.Arrays.equals(before, Files.readAllBytes(path)), "閲覧・検索・途中EOF・終了は保存しない");
        check(new JsonGameStore(path).load().size() == 1, "途中登録は再起動後にも残らない");
    }

    private static void testFailures(Path directory) throws IOException {
        Path unreadable = directory.resolve("directory.json");
        Files.createDirectory(unreadable);
        GameLibraryService readFailure = new GameLibraryService(new GameLibrary(), new JsonGameStore(unreadable));
        expectIOException(readFailure::load);
        String disabled = runCli(readFailure, "1\nMemory\n1\n2\n1\n2\n0\n2\n");
        check(disabled.contains("保存無効") && disabled.contains("メモリ内のみ")
                && disabled.contains("未保存のデータがあります"), "読み取り失敗後のCLI案内と終了警告");
        check(Files.isDirectory(unreadable), "読めない保存先を保護");

        Path path = directory.resolve("save-failure.json");
        JsonGameStore store = new JsonGameStore(path);
        GameLibraryService service = new GameLibraryService(new GameLibrary(), store);
        service.load();
        // 起動後に保存先がディレクトリになり、書き込みだけが失敗する状況。
        Files.createDirectory(path);
        String output = runCli(service, "1\nFirst\n1\n2\n1\n2\n0\n0\n2\n");
        check(service.getGames().size() == 1 && service.hasUnsavedChanges(), "保存失敗でもメモリ保持");
        check(output.contains("ただし未保存") && output.contains(path.toString())
                && output.contains("[1] タイトル: First") && output.contains("未保存のデータがあります"),
                "保存失敗の原因・一覧・終了案内、操作継続");
        Files.delete(path);
        check(service.savePending() && store.load().size() == 1 && !service.hasUnsavedChanges(),
                "保存再試行で未保存分を登録し直さず保存");

        Path blocker = directory.resolve("parent-is-file");
        Files.writeString(blocker, "keep");
        expectIOException(() -> new JsonGameStore(blocker.resolve("games.json")).save(service.getGames()));
        check(Files.readString(blocker).equals("keep"), "親ディレクトリ作成失敗を通知");
    }

    private static String runCli(GameLibraryService service, String inputText) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Scanner input = new Scanner(inputText);
                PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new GameLibraryCli(service, input, output).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void expectIOException(IoAction action) throws IOException {
        try {
            action.run();
        } catch (IOException expected) {
            checks++;
            return;
        }
        throw new AssertionError("IOExceptionを通知する必要があります。");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private interface IoAction {
        void run() throws IOException;
    }
}
