package gamelibrary;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        GameLibrary library = new GameLibrary();
        try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8)) {
            GameLibraryCli cli = new GameLibraryCli(library, input, System.out);
            cli.run();
        }
    }
}
