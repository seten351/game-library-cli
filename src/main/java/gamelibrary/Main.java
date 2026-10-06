package gamelibrary;

import java.io.IOError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        // runが戻る時点で、端末・ロック・チャネルはすべて閉じています。
        System.exit(run(args));
    }

    static int run(String[] args) {
        boolean plain = false;
        Path dataPath;
        try {
            dataPath = Path.of(System.getProperty("user.home"), ".game-library-cli", "games.json");
            boolean dataSpecified = false;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--plain") && !plain) {
                    plain = true;
                } else if (args[i].equals("--data") && !dataSpecified
                        && i + 1 < args.length && !args[i + 1].startsWith("--")
                        && !args[i + 1].isBlank()) {
                    dataPath = Path.of(args[++i]);
                    dataSpecified = true;
                } else {
                    throw new IllegalArgumentException("起動オプションを確認してください。");
                }
            }
            dataPath = dataPath.toAbsolutePath().normalize();
        } catch (IllegalArgumentException exception) {
            System.err.println("使用方法: java -jar game-library-cli-2.1.0.jar [--plain] [--data <ファイルパス>]");
            return 2;
        }
        try {
            dataPath = GameFileLock.resolveDataPath(dataPath);
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
            return 1;
        }
        System.out.println("保存先: " + dataPath);
        // 保存時だけではなく、古い一覧を読む前から終了まで排他利用します。
        try (GameFileLock ignored = GameFileLock.acquire(dataPath)) {
            GameLibraryService service = new GameLibraryService(new GameLibrary(), new JsonGameStore(dataPath));
            try {
                service.load();
            } catch (IOException exception) {
                System.err.println(exception.getMessage());
                System.err.println("元ファイルを保護するため、この起動中は保存を無効にします。"
                        + "復旧後、または別の--data保存先で再起動してください。");
            }
            Terminal terminal = createTerminal(plain);
            if (terminal != null) {
                try (terminal) {
                    new GameLibraryCli(service, new JLineCliInput(terminal), System.out).run();
                }
            } else {
                System.out.println("番号入力モード（番号を入力してEnterで決定）");
                try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8)) {
                    new GameLibraryCli(service, input, System.out).run();
                    if (input.ioException() != null) {
                        throw input.ioException();
                    }
                }
            }
            return service.isSavingEnabled() && !service.hasUnsavedChanges() ? 0 : 1;
        } catch (IOException | IOError exception) {
            System.err.println("起動・入出力・終了処理に失敗しました: " + exception.getMessage());
            return 1;
        }
    }

    private static Terminal createTerminal(boolean plain) {
        String type = System.getenv("TERM");
        // 標準入力/出力が端末でない場合は/dev/ttyへ迂回せず、パイプを読みます。
        if (plain || System.console() == null || type == null || type.isBlank()
                || type.startsWith("dumb")) {
            return null;
        }
        Terminal terminal = null;
        try {
            terminal = TerminalBuilder.builder().system(true)
                    .systemOutput(TerminalBuilder.SystemOutput.SysOut)
                    .devTty(false).dumb(false).encoding(StandardCharsets.UTF_8).build();
            if (terminal.getType().startsWith("dumb")
                    || terminal.getStringCapability(Capability.cursor_up) == null
                    || terminal.getStringCapability(Capability.key_up) == null
                    || terminal.getStringCapability(Capability.key_down) == null) {
                terminal.close();
                return null;
            }
            return terminal;
        } catch (IOException | IllegalStateException | LinkageError exception) {
            if (terminal != null) {
                try {
                    terminal.close();
                } catch (IOException closeException) {
                    exception.addSuppressed(closeException);
                }
            }
            System.err.println("端末を初期化できないため、番号入力に切り替えます。");
            return null;
        }
    }
}
