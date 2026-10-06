package gamelibrary;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** 同じ保存先の二重起動を防ぎます。JSON本体は置換されるため、別ファイルをロックします。 */
public final class GameFileLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private GameFileLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /** Storeとロックで共通の実体パスを使います。リンク切れは空の保存先と扱いません。 */
    public static Path resolveDataPath(Path requested) throws IOException {
        Path path = requested.toAbsolutePath().normalize();
        if (path.getFileName() == null) {
            throw new IOException("保存先にはファイル名を指定してください: " + path);
        }
        try {
            Files.createDirectories(path.getParent());
            try {
                return path.toRealPath();
            } catch (NoSuchFileException exception) {
                if (Files.isSymbolicLink(path)) {
                    throw new IOException("保存先のシンボリックリンクが切れています: " + path, exception);
                }
                return path.getParent().toRealPath().resolve(path.getFileName());
            }
        } catch (SecurityException exception) {
            throw new IOException("保存先を確認できません: " + path, exception);
        }
    }

    /** 読み込み前に取得し、CLIと端末の終了後まで保持します。待機はしません。 */
    public static GameFileLock acquire(Path resolvedPath) throws IOException {
        Path lockPath = resolvedPath.resolveSibling(resolvedPath.getFileName() + ".lock");
        FileChannel channel = null;
        try {
            // ロックファイルを他の場所へのリンクとして使わないようにします。
            channel = FileChannel.open(lockPath, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            FileLock lock = channel.tryLock();
            if (lock == null) {
                throw new IOException("同じ保存先を別のアプリが使用しています。");
            }
            return new GameFileLock(channel, lock);
        } catch (IOException | OverlappingFileLockException | SecurityException
                | UnsupportedOperationException exception) {
            IOException failure = new IOException("保存先のロックを取得できません: " + resolvedPath
                    + "（" + exception.getMessage() + "）", exception);
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException closeException) {
                    failure.addSuppressed(closeException);
                }
            }
            throw failure;
        }
    }

    @Override
    public void close() throws IOException {
        // releaseが失敗しても、try-with-resourcesでチャネルを必ず閉じます。
        try (channel) {
            lock.release();
        }
        // .lockは消しません。存在していても、ロックを取得できれば起動できます。
    }
}
