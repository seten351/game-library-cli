package gamelibrary;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/** ゲーム管理と保存を連携します。入力方法や表示方法は知りません。 */
public final class GameLibraryService {
    private final GameLibrary library;
    private final GameStore store;
    private boolean savingEnabled = true;
    private boolean unsavedChanges;

    public GameLibraryService(GameLibrary library, GameStore store) {
        this.library = Objects.requireNonNull(library);
        this.store = Objects.requireNonNull(store);
    }

    /** 起動時に1度呼びます。読み込み失敗後は、この実行中の保存を無効にします。 */
    public void load() throws IOException {
        try {
            List<Game> games = store.load();
            for (Game game : games) {
                library.addGame(game);
            }
        } catch (IOException exception) {
            savingEnabled = false;
            throw exception;
        }
    }

    /** 保存が失敗しても、メモリへ追加したゲームは取り消しません。 */
    public boolean registerGame(Game game) throws IOException {
        library.addGame(game);
        unsavedChanges = true;
        if (!savingEnabled) {
            return false;
        }
        store.save(library.getGames());
        unsavedChanges = false;
        return true;
    }

    public List<Game> getGames() {
        return library.getGames();
    }

    public List<Game> searchByTitle(String keyword) {
        return library.searchByTitle(keyword);
    }

    public boolean isSavingEnabled() {
        return savingEnabled;
    }

    public boolean hasUnsavedChanges() {
        return unsavedChanges;
    }
}
