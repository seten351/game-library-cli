package gamelibrary;

import java.io.IOError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--plain"))) {
            System.err.println("使用方法: java -jar game-library-cli-1.1.0.jar [--plain]");
            System.exit(2);
            return;
        }
        GameLibrary library = new GameLibrary();
        Terminal terminal = createTerminal(args.length == 1);
        if (terminal != null) {
            try (terminal) {
                new GameLibraryCli(library, new JLineCliInput(terminal), System.out).run();
            } catch (IOException | IOError exception) {
                System.err.println("端末の入出力に失敗しました: " + exception.getMessage());
                System.exit(1);
            }
            return;
        }
        System.out.println("番号入力モード（番号を入力してEnterで決定）");
        try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8)) {
            GameLibraryCli cli = new GameLibraryCli(library, input, System.out);
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
