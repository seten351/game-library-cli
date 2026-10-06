package gamelibrary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;

/** JSON変換と原子的なファイル置換。起動中のロック管理はMainが担当します。 */
public final class JsonGameStore implements GameStore {
    private final Path path;
    private final MoveOperation mover;
    private final Gson gson = new GsonBuilder().setStrictness(Strictness.STRICT)
            .setPrettyPrinting().create();

    public JsonGameStore(Path path) {
        this(path, (source, target) -> Files.move(source, target, StandardCopyOption.ATOMIC_MOVE));
    }

    // OSに依存する置換失敗をテストで再現するための、小さな差し替え口です。
    JsonGameStore(Path path, MoveOperation mover) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
        this.mover = Objects.requireNonNull(mover);
    }

    @FunctionalInterface
    interface MoveOperation {
        void move(Path source, Path target) throws IOException;
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
        // JSONを完成させ、一時ファイルを閉じてから保存先へ公開します。
        String json = gson.toJson(root) + System.lineSeparator();
        Path temporary = null;
        IOException failure = null;
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path) && !Files.isWritable(path)) {
                throw new IOException("保存先に書き込み権限がありません。");
            }
            Set<PosixFilePermission> permissions = existingPermissions();
            temporary = Files.createTempFile(path.getParent(), ".game-library-", ".tmp");
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            if (permissions != null) {
                Files.setPosixFilePermissions(temporary, permissions);
            }
            // ATOMIC_MOVEでは他のオプションが無視されます。失敗時は上書きへ戻しません。
            mover.move(temporary, path);
        } catch (IOException | SecurityException | UnsupportedOperationException exception) {
            failure = new IOException("保存に失敗しました: " + path + "（"
                    + exception.getMessage() + "）", exception);
            throw failure;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException | SecurityException exception) {
                    // 置換の成功を、後片付けの失敗で保存失敗に変えません。
                    if (failure != null) {
                        failure.addSuppressed(exception);
                    }
                }
            }
        }
    }

    private Set<PosixFilePermission> existingPermissions() throws IOException {
        if (Files.getFileAttributeView(path, PosixFileAttributeView.class) == null) {
            return null;
        }
        try {
            return Files.getPosixFilePermissions(path);
        } catch (NoSuchFileException exception) {
            return null;
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
