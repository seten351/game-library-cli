package gamelibrary;

import java.util.List;

/** 入力方式の違いをCLIの処理から切り離します。nullは入力終了を表します。 */
public interface CliInput {
    String readLine(String prompt);

    /** 番号の文字列を返します。範囲の検証はGameLibraryCliが行います。 */
    String readSelection(String prompt, List<MenuOption> options);
}
