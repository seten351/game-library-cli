package gamelibrary;

import java.util.Objects;

/** メニューの番号と表示名。ゲームのデータとは独立した入力用の値です。 */
public record MenuOption(int number, String label) {
    public MenuOption {
        Objects.requireNonNull(label);
    }
}
