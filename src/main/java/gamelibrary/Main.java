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
            System.err.println("使用方法: java -jar game-library-cli-2.0.0.jar [--plain] [--data <ファイルパス>]");
            System.exit(2);
            return;
        }
        GameLibraryService service = new GameLibraryService(new GameLibrary(), new JsonGameStore(dataPath));
        System.out.println("保存先: " + dataPath);
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
            } catch (IOException | IOError exception) {
                System.err.println("端末の入出力に失敗しました: " + exception.getMessage());
                System.exit(1);
            }
            return;
        }
        System.out.println("番号入力モード（番号を入力してEnterで決定）");
        try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8)) {
            GameLibraryCli cli = new GameLibraryCli(service, input, System.out);
            cli.run();
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
