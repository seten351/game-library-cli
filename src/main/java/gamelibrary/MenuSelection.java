package gamelibrary;

import java.util.List;

/** 選択状態だけを管理します。端末やJLineに依存せずテストできます。 */
final class MenuSelection {
    private final List<MenuOption> options;
    private final StringBuilder typed = new StringBuilder();
    private int selectedIndex;
    private boolean typing;

    MenuSelection(List<MenuOption> options) {
        this.options = List.copyOf(options);
        if (options.isEmpty()) {
            throw new IllegalArgumentException("選択肢が必要です。");
        }
    }

    void move(int direction) {
        selectedIndex = Math.clamp(selectedIndex + direction, 0, options.size() - 1);
        typing = false;
        typed.setLength(0);
    }

    void type(String text) {
        // 最初の文字は初期選択番号に追記せず、番号入力を始めます。
        typing = true;
        typed.append(text);
        updateSelectedIndex();
    }

    void backspace() {
        typing = true;
        if (!typed.isEmpty()) {
            int last = typed.offsetByCodePoints(typed.length(), -1);
            typed.delete(last, typed.length());
        }
        updateSelectedIndex();
    }

    String value() {
        return typing ? typed.toString() : Integer.toString(options.get(selectedIndex).number());
    }

    String hint() {
        int index = findIndex(value());
        return index < 0 ? "[番号を入力し、Enterで確認]" : "[" + options.get(index).label() + "]";
    }

    private void updateSelectedIndex() {
        int index = findIndex(value());
        if (index >= 0) {
            selectedIndex = index;
        }
    }

    private int findIndex(String value) {
        try {
            int number = Integer.parseInt(value.strip());
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).number() == number) {
                    return i;
                }
            }
        } catch (NumberFormatException exception) {
            // 入力全体を検証するため、12を2として扱うことはありません。
        }
        return -1;
    }
}
