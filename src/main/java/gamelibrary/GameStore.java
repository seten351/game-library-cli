package gamelibrary;

import java.io.IOException;
import java.util.List;

/** 保存方法の約束。ゲーム管理や画面表示には依存しません。 */
public interface GameStore {
    /** 保存先がまだない場合は空のListを返します。 */
    List<Game> load() throws IOException;

    /** 登録順を保ち、ゲーム全件を保存します。 */
    void save(List<Game> games) throws IOException;
}
