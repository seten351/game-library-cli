package gamelibrary;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/** v1と同じ行単位の番号入力。パイプ入力や端末機能のない環境でも使えます。 */
public final class PlainCliInput implements CliInput {
    private final Scanner input;
    private final PrintStream output;

    public PlainCliInput(Scanner input, PrintStream output) {
        this.input = Objects.requireNonNull(input);
        this.output = Objects.requireNonNull(output);
    }

    @Override
    public String readLine(String prompt) {
        output.print(prompt);
        output.flush();
        return input.hasNextLine() ? input.nextLine() : null;
    }

    @Override
    public String readSelection(String prompt, List<MenuOption> options) {
        return readLine(prompt);
    }
}
