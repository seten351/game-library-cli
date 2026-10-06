package gamelibrary;

import java.util.ArrayList;
import java.util.List;

/** 端末から独立した選択状態の確認。番号順と表示順の違いも検証します。 */
public final class MenuSelectionTest {
    private static int checks;

    private MenuSelectionTest() {
    }

    public static void main(String[] args) {
        List<MenuOption> menu = new ArrayList<>(List.of(
                new MenuOption(1, "登録"), new MenuOption(2, "一覧"),
                new MenuOption(3, "検索"), new MenuOption(4, "保存再試行"),
                new MenuOption(0, "終了")));
        MenuSelection selection = new MenuSelection(menu);
        menu.clear();
        check(selection.value().equals("1") && selection.hint().contains("登録"), "先頭を初期選択、外部Listと独立");
        selection.move(-1);
        check(selection.value().equals("1"), "上端で停止");
        selection.move(1);
        check(selection.value().equals("2") && selection.hint().contains("一覧"), "下へ移動しラベル更新");
        selection.move(1);
        selection.move(1);
        check(selection.value().equals("4"), "保存再試行へ表示順で移動");
        selection.move(1);
        check(selection.value().equals("0"), "番号順ではなく表示順で終了へ移動");
        selection.move(1);
        check(selection.value().equals("0"), "下端で停止");
        selection.type("3");
        check(selection.value().equals("3") && selection.hint().contains("検索"), "初期値へ追記せず番号入力");
        selection.type("2");
        check(selection.value().equals("32") && !selection.hint().contains("一覧"), "複数桁を最後の数字として扱わない");
        selection.backspace();
        check(selection.value().equals("3"), "Backspaceで入力を編集");
        selection.move(-1);
        check(selection.value().equals("2"), "番号入力から方向キーへ切り替え");
        selection.type("12");
        check(selection.value().equals("12"), "貼り付けも全体を保持");
        selection.move(1);
        check(selection.value().equals("3"), "不正入力後は最後の有効な選択から移動");
        selection.backspace();
        check(selection.value().isEmpty(), "方向キー選択からBackspaceで空の番号入力");
        selection.type(" 0 ");
        check(selection.hint().contains("終了"), "前後空白付きの番号");
        selection.move(-1);
        selection.type("😃");
        selection.backspace();
        check(selection.value().isEmpty(), "Unicodeを1文字単位で削除");
        for (int size : new int[] {6, 8}) {
            List<MenuOption> options = new ArrayList<>();
            for (int i = 1; i <= size; i++) {
                options.add(new MenuOption(i, "項目" + i));
            }
            selection = new MenuSelection(options);
            selection.type(Integer.toString(size));
            check(selection.hint().contains("項目" + size), "enumの最終項目を番号選択");
            selection.move(1);
            check(selection.value().equals(Integer.toString(size)), "enumの下端で停止");
            selection.move(-1);
            check(selection.value().equals(Integer.toString(size - 1)), "enumの上下移動");
        }
        System.out.println("MenuSelectionTest: " + checks + " checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
