package gamelibrary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;

/** v2.0の基本的なJSON保存。安全な置換やロックはv2.1で扱います。 */
public final class JsonGameStore implements GameStore {
    private final Path path;
    private final Gson gson = new GsonBuilder().setStrictness(Strictness.STRICT)
            .setPrettyPrinting().create();

    public JsonGameStore(Path path) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
    }

    @Override
    public List<Game> load() throws IOException {
        try {
            String json;
            try {
                json = Files.readString(path, StandardCharsets.UTF_8);
            } catch (NoSuchFileException exception) {
                return new ArrayList<>();
            }
            JsonElement document = gson.fromJson(json, JsonElement.class);
            JsonObject root = requireObject(document, "ルート");
            JsonPrimitive version = requireNumber(root, "schemaVersion");
            if (version.getAsBigDecimal().intValueExact() != 1) {
                throw new IllegalArgumentException("未対応のschemaVersionです。");
            }
            JsonElement entries = root.get("games");
            if (entries == null || !entries.isJsonArray()) {
                throw new IllegalArgumentException("gamesには配列が必要です。");
            }
            List<Game> games = new ArrayList<>();
            for (JsonElement entry : entries.getAsJsonArray()) {
                JsonObject game = requireObject(entry, "ゲーム");
                // Gameへ直接変換せず、コンストラクタの検証を必ず通します。
                games.add(new Game(requireString(game, "title"),
                        Platform.valueOf(requireString(game, "platform")),
                        Genre.valueOf(requireString(game, "genre")),
                        requireNumber(game, "playHours").getAsDouble()));
            }
            // 全件が有効になってから、呼び出し側へ返します。
            return games;
        } catch (IOException | SecurityException | JsonParseException
                | IllegalArgumentException | IllegalStateException | ArithmeticException exception) {
            throw new IOException("読み込みに失敗しました: " + path + "（"
                    + exception.getMessage() + "）", exception);
        }
    }

    @Override
    public void save(List<Game> games) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        JsonArray entries = new JsonArray();
        for (Game game : games) {
            JsonObject entry = new JsonObject();
            entry.addProperty("title", game.getTitle());
            entry.addProperty("platform", game.getPlatform().name());
            entry.addProperty("genre", game.getGenre().name());
            entry.addProperty("playHours", game.getPlayHours());
            entries.add(entry);
        }
        root.add("games", entries);
        // JSONを完成させてから書き込みます。v2.0はファイルを直接上書きします。
        String json = gson.toJson(root) + System.lineSeparator();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (IOException | SecurityException exception) {
            throw new IOException("保存に失敗しました: " + path + "（"
                    + exception.getMessage() + "）", exception);
        }
    }

    private static JsonObject requireObject(JsonElement value, String name) {
        if (value == null || !value.isJsonObject()) {
            throw new IllegalArgumentException(name + "にはオブジェクトが必要です。");
        }
        return value.getAsJsonObject();
    }

    private static String requireString(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(name + "には文字列が必要です。");
        }
        return value.getAsString();
    }

    private static JsonPrimitive requireNumber(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(name + "には数値が必要です。");
        }
        return value.getAsJsonPrimitive();
    }
}
