package gamelibrary;

import java.util.List;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.Widget;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

/** 端末入力はこの1つのLineReaderに集約し、Scannerと競合させません。 */
public final class JLineCliInput implements CliInput {
    private final LineReader reader;

    public JLineCliInput(Terminal terminal) {
        reader = LineReaderBuilder.builder().terminal(terminal)
                .appName("game-library-cli")
                .variable(LineReader.DISABLE_HISTORY, true)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .option(LineReader.Option.BRACKETED_PASTE, true)
                // タイトルの引用符や末尾のバックスラッシュもそのまま受け取ります。
                .parser(new DefaultParser().quoteChars(new char[0]).escapeChars(new char[0]))
                .build();
    }

    @Override
    public String readLine(String prompt) {
        try {
            return reader.readLine(prompt);
        } catch (EndOfFileException | UserInterruptException exception) {
            return null;
        }
    }

    @Override
    public String readSelection(String prompt, List<MenuOption> options) {
        MenuSelection selection = new MenuSelection(options);
        KeyMap<Binding> normalKeys = reader.getKeyMaps().get(LineReader.MAIN);
        Widget normalInit = reader.getWidgets().get(LineReader.CALLBACK_INIT);
        KeyMap<Binding> menuKeys = menuKeys(selection);
        reader.getKeyMaps().put(LineReader.MAIN, menuKeys);
        reader.setAutosuggestion(LineReader.SuggestionType.TAIL_TIP);
        reader.setTailTip(selection.hint());
        reader.getWidgets().put(LineReader.CALLBACK_INIT, () -> {
            // 初回の描画にも項目名のヒントを表示します。
            reader.callWidget(LineReader.REDISPLAY);
            return true;
        });
        try {
            // 表示ラベルはヒントに分離し、返り値は番号の文字列だけにします。
            return reader.readLine("選択中（↑↓ / 番号、Enterで決定）: ", null,
                    (Character) null, selection.value());
        } catch (EndOfFileException | UserInterruptException exception) {
            return null;
        } finally {
            reader.getKeyMaps().put(LineReader.MAIN, normalKeys);
            if (normalInit == null) {
                reader.getWidgets().remove(LineReader.CALLBACK_INIT);
            } else {
                reader.getWidgets().put(LineReader.CALLBACK_INIT, normalInit);
            }
            reader.setAutosuggestion(LineReader.SuggestionType.NONE);
            reader.setTailTip("");
        }
    }

    private KeyMap<Binding> menuKeys(MenuSelection selection) {
        KeyMap<Binding> keys = new KeyMap<>();
        // エスケープシーケンスの解釈はJLineに任せます。
        keys.setAmbiguousTimeout(1000);
        keys.setNomatch(new Reference(LineReader.UNDEFINED_KEY));
        keys.setUnicode(new Reference("menu-type"));
        reader.getWidgets().put("menu-type", () -> {
            selection.type(reader.getLastBinding());
            return refresh(selection);
        });
        for (char c = 32; c < 127; c++) {
            keys.bind(new Reference("menu-type"), Character.toString(c));
        }
        reader.getWidgets().put("menu-up", () -> {
            selection.move(-1);
            return refresh(selection);
        });
        reader.getWidgets().put("menu-down", () -> {
            selection.move(1);
            return refresh(selection);
        });
        bindArrow(keys, "menu-up", Capability.key_up, "\033[A", "\033OA");
        bindArrow(keys, "menu-down", Capability.key_down, "\033[B", "\033OB");
        reader.getWidgets().put("menu-backspace", () -> {
            selection.backspace();
            return refresh(selection);
        });
        keys.bind(new Reference("menu-backspace"), KeyMap.del(), KeyMap.ctrl('H'));
        // 貼り付けも入力全体として扱い、初期選択番号へ追記しません。
        reader.getWidgets().put("menu-paste", () -> {
            reader.getBuffer().clear();
            reader.getBuiltinWidgets().get(LineReader.BEGIN_PASTE).apply();
            selection.type(reader.getBuffer().toString());
            return refresh(selection);
        });
        keys.bind(new Reference("menu-paste"), "\033[200~");
        keys.bind(new Reference(LineReader.ACCEPT_LINE), "\r", "\n");
        keys.bind(new Reference(LineReader.SEND_BREAK), KeyMap.ctrl('C'));
        reader.getWidgets().put("menu-eof", () -> { throw new EndOfFileException(); });
        keys.bind(new Reference("menu-eof"), KeyMap.ctrl('D'));
        return keys;
    }

    private void bindArrow(KeyMap<Binding> keys, String widget, Capability capability,
            String normal, String application) {
        keys.bind(new Reference(widget), normal, application);
        String terminalKey = KeyMap.key(reader.getTerminal(), capability);
        if (terminalKey != null) {
            keys.bind(new Reference(widget), terminalKey);
        }
    }

    private boolean refresh(MenuSelection selection) {
        reader.getBuffer().clear();
        reader.getBuffer().write(selection.value());
        reader.setTailTip(selection.hint());
        return true;
    }
}
