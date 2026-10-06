# ゲームライブラリ管理CLI v2.1

Javaのクラス、enum、List、メソッド、例外、インターフェースとファイル入出力を学ぶCLIアプリです。
ゲームの登録・一覧表示・タイトル検索とJSON永続化に、安全な置換・排他制御・保存再試行を追加しました。
v1.1のゲーム管理・データ構造と、方向キー・番号・plain入力を維持しています。

## 必要な環境とビルド

| 用途 | 必要なもの |
| --- | --- |
| ビルド | JDK 21以上、初回はインターネット接続、macOSではcurl |
| JARの実行 | Java 21以上（JREでも可）、UTF-8を扱えるターミナル |
| 方向キー操作 | 標準入力・出力がTTYで、TERMと端末機能が利用できる環境 |
| 追加のPTY検証 | macOS/LinuxとPython 3.9以上（アプリの実行には不要） |

```sh
cd game-library-cli
./mvnw clean verify
java -jar target/game-library-cli-2.1.0.jar
```

JAVA_HOMEにはJREではなくJDK 21以上を指定します。この開発環境のJDK 22でもJava 21互換でビルドできます。

```sh
JAVA_HOME="$(/usr/libexec/java_home -v 22)" ./mvnw clean verify
```

Mavenの事前インストールは不要です。WrapperがMaven 3.10.0を取得し、配布物のSHA-256を検証します。
コンパイルはUTF-8・release 21で行い、JLine 4.4.6（jdk11配布物）とGson 2.14.0を実行JARに同梱します。
外部のアプリ用フレームワークは使いません。初回の依存関係取得後はキャッシュを使用できます。
起動・配布するのは `target/game-library-cli-2.1.0.jar` です。original-で始まるJARは配布用ではありません。
旧v1のoutディレクトリや手動のjavacコマンドは、今回の起動には使いません。

## 保存先とJSON形式

既定の保存先は `~/.game-library-cli/games.json` です。起動時に絶対パスを表示します。
作業ディレクトリを変えても同じホームのデータを使います。SSHでは接続先ユーザーのホームです。
初回にJSONがなければ空で開始します。起動時に親ディレクトリとロックファイルを作り、登録完了時にJSONを作成します。
保存先は実体パスへ解決します。既存のシンボリックリンクは実体を使い、リンク切れは起動失敗とします。

```sh
java -jar target/game-library-cli-2.1.0.jar --data /tmp/my-learning-games.json
java -jar target/game-library-cli-2.1.0.jar --plain --data './practice/games.json'
```

`--data <ファイルパス>` は絶対・相対パスに対応します。相対パスは起動時の作業ディレクトリ基準です。
`--plain` との順序は自由です。空のパス、不明なオプション、重複したオプションは使用方法を表示します。
空白のあるパスは引用符で囲んでください。保存ファイルはソースコードと別に管理します。

```json
{
  "schemaVersion": 1,
  "games": [
    {
      "title": "Echo",
      "platform": "PC",
      "genre": "RPG",
      "playHours": 1.5
    }
  ]
}
```

UTF-8のJSONです。schemaVersionはアプリのバージョンとは別の保存形式番号です。
画面の表示名ではなくenum定数名を保存し、登録順・重複タイトル・小数を保持します。
日本語、引用符、バックスラッシュ等のエスケープはGsonに任せます。
読み込みではJSONの構造と型を確認し、Gameのコンストラクタで値も検証します。
全件が有効になるまでGameLibraryへ追加しません。未対応の形式番号も読み込み失敗として扱います。

## 操作とデータのルール

| 番号 | 機能 |
| --- | --- |
| 1 | ゲームを登録し、全件を自動保存 |
| 2 | 全ゲームを登録順で表示 |
| 3 | タイトルを部分一致検索 |
| 4 | 未保存データの保存を再試行（新しい登録は不要） |
| 0 | 終了。未保存があれば確認 |

メインメニュー・プラットフォーム・ジャンル・終了確認は↑↓で選択し、Enterで確定できます。
上下端で停止し、循環しません。番号を入力してEnterでも選択できます。
番号入力中に方向キーへ戻ることもでき、複数桁は入力全体を検証します。
タイトル・検索語・プレイ時間は文字入力です。Backspaceによる修正にも対応します。

例：`1` → `Echo` → `1`（PC）→ `2`（RPG）→ `1.5`を、各Enterで確定します。
登録後に保存成功の案内が出ます。`2`で一覧、`3` → `ech`で検索、`0`で終了できます。
再起動して一覧を表示すると、保存されたゲームが復元されます。

- タイトルは必須で、前後の空白を除去します。日本語や同じタイトルの複数登録も可能です。
- Platform：PC / PLAYSTATION / NINTENDO_SWITCH / XBOX / MOBILE / OTHER。
- Genre：ACTION / RPG / ADVENTURE / SIMULATION / STRATEGY / SPORTS / PUZZLE / OTHER。
- 画面にはenumの表示名を使います。OTHERは「その他」で追加の自由入力はありません。
- プレイ時間は0以上の有限の数値です。小数点は`.`を使います。負数、NaN、無限大は拒否します。
- 検索はタイトルのみ。大文字・小文字を区別せず部分一致で検索し、空の検索語は拒否します。
- 一覧・検索結果には4項目を表示します。不正な入力は説明して再入力を求めます。

### plain入力とフォールバック

```sh
java -jar target/game-library-cli-2.1.0.jar --plain
```

--plain、非TTY、TERM=dumb・未設定、端末機能不足、端末初期化失敗では行単位の番号入力へ切り替えます。
このモードでは番号とEnterを使います。空の番号は不正入力です。
制御シーケンスを出力せず、パイプや入力リダイレクトを読みます。/dev/ttyへ迂回しません。
入力はJLineかScannerのいずれか一方が担当します。

EOF／JLineで受け取るCtrl+Cでは、確認や自動保存をせず終了します。登録途中のゲームは追加・保存しません。
未保存の登録があれば失われる旨を表示し、終了コードは1になります。文字入力中のCtrl+Dは空の行でEOFになります。
plain入力中のCtrl+CはOSのシグナルによる従来の終了で、アプリの確認や終了コードへの統一は行いません。
一覧・検索・未保存のない通常終了・入力終了だけではJSONを書き換えません。

## v2.0からv2.1へ：問題と解決

| 学習対象 | v2.0では何が問題か | v2.1でどう解決するか |
| --- | --- | --- |
| 原子的置換 | 本体を直接上書きするため、途中の失敗でJSONが欠ける可能性 | 同じディレクトリの一時ファイルに全件を書き、閉じてからATOMIC_MOVEで置換 |
| ファイルロック | 二重起動した両方が古い一覧を読み、後の保存で先の登録を消す可能性 | 読み込み前から終了まで、固定のサイドカーファイルをtryLockでロック |
| 未保存と再試行 | 保存失敗後は次の追加登録まで再保存できない | 未保存をメモリに保持し、4で追加登録なしに再保存 |
| 終了コード | 読み込み失敗や未保存でも通常終了扱いになる | 起動時の失敗と終了時の保存状態を0／1／2で表す |

### 保存の流れと原子的置換

```text
CLI → Service：メモリに登録、未保存=true
  → JsonGameStore：全件のJSONを生成
  → 同じディレクトリに .game-library-*.tmp を作成
  → UTF-8で書き込み完了、ファイルを閉じる
  → ATOMIC_MOVEで games.json を置換
  → Service：成功後だけ未保存=false
```

一時ファイルへ書いている途中では、従来のgames.jsonを変更しません。
置換前の失敗では元の内容を保持し、今回の処理で作った一時ファイルを削除します。
削除自体が失敗した場合や強制終了では一時ファイルが残ることがあります。自動復元や一括削除はしません。
既存ファイルのPOSIXの通常の読み書き権限は引き継ぎ、書き込み不可の保存先は拒否します。

ATOMIC_MOVEでは他のオプションが無視され、既存ファイルを置換できるかは環境に依存します。
未対応・置換拒否などの失敗はIOExceptionで通知し、直接上書きや非原子的な置換へ切り替えません。
これは途中の内容を保存先として公開しない改善です。forceやディスク同期は追加しておらず、
電源断に対する完全な永続性やネットワークファイルシステム全般の保証ではありません。
原子的置換の契約は[Java 21のFiles.move仕様](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html)で確認できます。

### 読み込み前からロックする理由

```text
Main：実体パスの解決 → games.json.lockをtryLock
  ├─ 取得できない：原因を案内して終了コード1（待機しない）
  └─ 取得できた：load → CLI → 端末を閉じる → ロック・チャネルを閉じる → 終了
```

保存の瞬間だけロックしても、古い一覧を読み込んだ別のプロセスによる上書きは防げません。
またJSON本体は原子的置換で別のファイルになるため、本体をロックするのではなく、
固定の `games.json.lock` を使います。FileLockとFileChannelの解放はtry-with-resourcesで行います。

ロックファイルは終了後も残します。存在するだけでは「使用中」ではなく、tryLockの取得可否で判断します。
実行中にロックファイルを削除・置換しないでください。同じロック規約を使うv2.1同士の排他制御であり、
v2.0や外部エディタによる変更は防ぎません。ハードリンクによる別名利用や実行中の外部パス変更も対象外です。
OSによるロックの違いは[Java 21のFileLock仕様](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/channels/FileLock.html)に記載されています。

### 読み込み・保存に失敗した場合

| 状況 | v2.1の動作 |
| --- | --- |
| 保存JSONが存在しない | 空で開始。起動時はディレクトリと.lockだけを作成 |
| ロック取得競合・ロック作成失敗・リンク切れ | 原因を案内して起動を中止。終了コード1 |
| 壊れたJSON、空ファイル、値の不正、未対応形式、読み取り失敗 | 元ファイルを保護し、その起動中は保存無効。メモリ内で操作を続け、終了コード1 |
| 登録後の保存失敗 | 登録をメモリ内に保持し、未保存を表示してメニューを続ける |
| 問題解消後に4を選択 | 以前の未保存分を含む全件を保存。成功後に未保存状態を解除 |
| 未保存のない状態で4 | 保存済みと案内。書き込みは行わない |
| 読み込み失敗後に4 | 保存は再有効化しない。復旧後、または別の--dataで再起動するよう案内 |

読み込み失敗では保存済みゲームを復元できていません。表示するのは、その実行中に登録した内容だけです。
元ファイルを別途退避・修復するか、別の--data保存先で再起動してください。
新しい通常登録が成功した場合も、それまでの未保存分を含む全件を保存します。
自動再試行・待機・バックアップ・自動修復は追加していません。

### 未保存のまま終了する場合

メインメニューの0で終了するとき、未保存があれば次の確認を表示します。
方向キーの初期選択は「メニューに戻る」で、Enterだけで破棄しません。

| 確認の番号 | 動作 |
| --- | --- |
| 0 | メインメニューに戻る |
| 1 | 保存して終了。失敗した場合は未保存を保持してメインメニューに戻る |
| 2 | 警告を表示し、保存せず終了する |

### 終了コード

| コード | 意味 |
| --- | --- |
| 0 | 起動時の読み込み失敗がなく、終了時に未保存がない |
| 1 | 読み込み失敗、ロック取得失敗、未保存のまま終了、端末・後片付けの失敗 |
| 2 | 起動引数の誤り |

保存失敗後に再試行が成功すれば0で終了できます。読み込み失敗後は、登録せず終了しても1です。
System.exitは端末・ロック・チャネルの後片付けが終わってから呼びます。
plainのCtrl+CなどOSが直接終了させるケースは、OS側の終了結果になります。

```sh
java -jar target/game-library-cli-2.1.0.jar --plain
status=$?
printf '終了コード: %s\n' "$status"
```

編集・削除・DB・GUI・Web版は今回の実装範囲に含みません。

## 構成と学習ポイント

```text
game-library-cli/
├── src/main/java/gamelibrary/  # 管理・CLI・入力・保存
├── src/test/java/gamelibrary/  # main形式のJavaテスト
├── scripts/verify_cli.py       # JARの非対話・PTY・永続化検証
├── .mvn/wrapper/               # Maven Wrapperの設定
├── mvnw / mvnw.cmd
├── pom.xml
└── README.md
```

```text
Main（実体パス・ロック・入力方式・終了コードを管理）
  ├─ GameFileLock → games.json.lock（読み込み前から終了まで保持）
  ├─ PlainCliInput / JLineCliInput → CliInput
  └─ GameLibraryCli（入力検証・表示）
          ↓
     GameLibraryService（復元・登録後保存・未保存状態・再保存）
       ├─ GameLibrary → List<Game> → Game / Platform / Genre
       └─ GameStore ← JsonGameStore → 一時ファイル → JSONファイルへ原子的置換
```

| 型 | 役割と設計理由 |
| --- | --- |
| Game / Platform / Genre | v1から変更なし。コンストラクタの検証、private finalとgetter、固定選択肢を学ぶ |
| GameLibrary | v1から変更なし。ArrayListで保持し、ループで検索。ファイルや端末を知らない |
| GameStore | load / saveを宣言する小さなインターフェース。管理側がJSONの具体的な書き方に依存しないための契約 |
| JsonGameStore | JSON変換・検証・一時ファイルによる原子的置換。失敗は保存先と原因を含むIOExceptionで伝える |
| GameFileLock | 実体パスの解決と固定の.lockによる排他制御。AutoCloseableで後片付けを確実にする |
| GameLibraryService | GameLibraryとGameStoreを連携。復元・登録後保存・savePendingによる再保存と、保存有効／未保存の状態を担当し、表示はしない |
| GameLibraryCli | Serviceを呼び、入力検証・一覧・検索・再試行・終了確認・保存結果の表示を担当。JSONの読み書きを埋め込まない |
| CliInput / PlainCliInput / JLineCliInput | v1.1から変更なし。端末操作を管理や保存から分離する |
| MenuOption / MenuSelection | v1.1から変更なし。番号・表示名と選択状態を扱う |
| Main | Pathと具体的な実装を組み立てる。起動時読み込み、ロック・端末の生存期間、終了コードを管理する |

起動時は `Main → ロック取得 → Service.load → Store.load → GameLibraryへ復元` と流れます。
登録時は `CLIで全項目入力 → Game作成 → Service → メモリへ追加 → Storeで全件保存 → 結果表示` です。
GameLibraryの責務を増やさず、入力方式も変更せずに永続化を追加できる構成です。
DB等への実装差し替えを考えられる契約だけを追加し、未使用のRepository階層やファクトリー等は導入しません。

GameLibraryのaddGame / getGames / searchByTitleは従来どおりです。
一覧・検索は新しいListを返すので、呼び出し側が返却Listを変えても内部の登録内容は変わりません。
Gameは作成後に変更できません。JSONからもGameのコンストラクタを通し、不正な値を取り込みません。
CLIのScanner用コンストラクタも残し、依存先をGameLibraryServiceへ変更しています。
テストではGameStoreを簡単なメモリ用実装に差し替え、実データに触れず入力処理を確認します。

v2.0ではインターフェース・連携役・例外処理・JSONとJavaの変換を学びます。
v2.1では原子的置換・FileChannel.tryLock・AutoCloseable・未保存状態と再試行・終了コードを学びます。
GameStoreのload／save契約、GameLibrary、Game、enum、CliInputはそのままです。
保存処理をCLIに書かず、状態はService、ファイル操作はStore、起動中の資源管理はMainに分けています。

## MacBook AirからMac miniへのSSH

開発元はDocumentsに残し、SSH用JARはDocuments外へ配置できます。
Mac miniのローカルターミナルで、ビルド済みプロジェクト直下から実行します。

```sh
mkdir -p "$HOME/Applications/game-library-cli"
cp target/game-library-cli-2.1.0.jar "$HOME/Applications/game-library-cli/"
```

MacBook Airで接続し、接続先のJava 21以上で起動します。

```sh
ssh game-host
# IPで接続する場合: ssh user@game-host
java -version
java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.1.0.jar"
```

Air側からJARを転送する場合、配置先の作成後に `scp target/game-library-cli-2.1.0.jar game-host:Applications/game-library-cli/` を使えます。
リモートコマンドとして方向キーを使う場合はPTYを割り当てます。

```sh
ssh -t game-host 'java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.1.0.jar"'
```

JavaがPATHにない場合は実行ファイルの絶対パスを指定してください。
TERMを根拠なく書き換える必要はありません。TTYがなければ番号入力へ切り替わります。

以前のv1ではDocuments内のソース列挙がOperation not permittedとなり、zshのワイルドカード展開にも失敗していました。
macOSのDocumentsアクセス保護が有力な原因でした。JARをDocuments外へ置く手順を維持します。
アプリはOSのアクセス権やSSH設定を変更しません。v2.1の既定データもホーム直下の専用ディレクトリです。

### 実データを使わないSSH確認

SSH接続後、専用の一時フォルダで確認できます。

```sh
test_dir=$(mktemp -d /tmp/game-library-v21-ssh.XXXXXX) || exit 1
java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.1.0.jar" --data "$test_dir/games.json"
# 登録して終了後、同じ保存先で再起動し一覧・検索を確認
java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.1.0.jar" --data "$test_dir/games.json"
java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.1.0.jar" --plain --data "$test_dir/games.json"
# 確認に使った専用フォルダだけを削除
rm -r "$test_dir"
```

方向キー・番号の切り替え、日本語、小数、登録順、再起動復元、通常終了・途中Ctrl+C/EOFを確認します。
検証前後のstty -gを比較すれば端末属性の復元も確認できます。
同じMac miniの同じユーザーで動くローカル・SSHのアプリは、同じ保存先を使用します。
専用の一時フォルダのパスを控え、片方を起動したままもう片方で同じ--dataを指定すると、
後から起動した方がロック競合で終了コード1になります。先のアプリを終了した後は再起動できます。
Mac miniローカル→SSH、SSH→ローカルの両方向で確認し、異なる--dataでは同時に起動できることも確認してください。

保存再試行は、起動後に専用テストディレクトリの書き込み権限を外して登録し、
別のターミナルで権限を戻してから4で確認できます。利用者の保存先では試さないでください。
終了確認の「戻る」「保存して終了」「保存せず終了」、EOF時の警告と終了コードも確認します。
非TTYの確認は、別の専用一時領域を使う次の例で実行できます。

```sh
ssh -T game-host 'sh -s' <<'REMOTE'
test_dir=$(mktemp -d /tmp/game-library-v21-ssh.XXXXXX) || exit 1
trap 'rm -r "$test_dir"' EXIT
java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.1.0.jar" --plain --data "$test_dir/games.json" <<'INPUT'
1
Echo
1
2
1.5
2
3
ech
0
INPUT
REMOTE
```

## テスト・今回の検証結果

```sh
./mvnw clean verify
python3 scripts/verify_cli.py
```

Javaテストはmain形式で、AssertionErrorはビルド失敗になります。-eaや外部のテストフレームワークは不要です。
JSONのテストは毎回専用の一時ディレクトリを作り、終了後に削除します。
CLI自動検証も全起動で--dataを指定し、既定パスの確認ではJVMのuser.homeを一時領域へ置き換えます。
利用者のホームにある実データや、既存の保存ファイルは読み書きしません。

| テスト | 確認内容 |
| --- | --- |
| GameLibraryTest（34項目） | モデル検証、登録順、重複、検索、返却Listの独立性 |
| GameLibraryCliTest（48項目） | 番号による登録・一覧・検索、不正入力、日本語、各入力段階のEOF |
| MenuSelectionTest（21項目） | 初期選択、終了確認の初期値0、上下端、番号切り替え、複数桁、貼り付け、Backspace |
| PersistenceTest（202項目） | JSON往復・全件検証、原子的置換失敗・一時ファイル削除・POSIX権限、パス別名と同一JVMのロック競合、保存無効・未保存保持・追加登録なしの再試行 |
| verify_cli.py（155項目） | 実行JARのPTY・非対話入力、方向キー・番号・plain・フォールバック・端末復元、復元・保存再試行・終了確認・終了コード、別プロセスのロック競合・異常終了後の再取得 |

JavaとJARを明示することもできます。

```sh
python3 scripts/verify_cli.py --java '/Javaの配置先/bin/java' --jar '/JARの配置先/game-library-cli-2.1.0.jar'
```

v2.1の開発時検証結果（2026年10月6日）：

| 環境・対象 | 結果 |
| --- | --- |
| ローカル：Mavenビルド・JAR生成 | 成功。JDK 22でrelease 21を指定 |
| ローカル：Javaテスト | Temurin Java 21.0.11で305項目成功（JDK 22でも成功） |
| ローカル：実行JARのPTY・非対話・JSON検証 | Temurin Java 21.0.11で155項目成功 |
| ローカル：ゲーム管理・入力関連 | Game / GameLibrary / Platform / Genre、GameStoreの契約、入力関連5ファイルを維持 |
| SSH実機：SSH設定のHost別名 | 未確認。自動接続の認証が拒否された |
| SSH実機：user@game-host | 既知のMac miniのホスト鍵で接続先を検証。認証が拒否されたためv2.1の動作は未確認 |

SSH接続の失敗はPermission denied（publickey,password,keyboard-interactive）です。
ローカルPTYテストはSSH実機検証の代用ではありません。
v1.1は以前MacBook Airからのユーザー確認報告を受けていますが、v2.1の実機成功としては扱いません。
今回の作業ではSSH用の配置済みv1.1 JARや利用者の実データを変更していません。

生成物はtarget、v1の旧生成物はoutで、Git管理対象から除外します。
既定データはプロジェクト外です。独自の保存先・バックアップ・ロックファイル・一時JSONもGitへ含めないでください。
生成物だけを消す場合はプロジェクト直下で `./mvnw clean` を実行します。


## Git / GitHubでのバージョン管理

`main`は現行v2.1から今後の開発を継続するブランチです。
`v1.0.0` / `v1.1.0` / `v2.0.0` / `v2.1.0`は、検証した各ソースを指す注釈付きタグです。
これらは公開時に作成した復元コミットであり、当時のGit履歴や作成日時を再現したものではありません。
記録されたソースと不足資料、公開用変更、追加した補助は [RESTORATION.md](RESTORATION.md) に記載しています。

```sh
git switch --detach v2.0.0
# 各版のREADMEに従ってビルド・実行する
git switch main
```

[GitHub Releases](https://github.com/seten351/game-library-cli/releases) に各版のJARとSHA256SUMS.txtを保存しています。
ダウンロードした同じディレクトリで `shasum -a 256 -c SHA256SUMS.txt` を実行すると整合性を確認できます。
v1.1・v2.0の `-original.jar` は手元に残っていた配布物です。通常のJARは記録されたソースから今回ビルドしたものです。
公開済みタグは動かさず、今後の変更はmainへコミットし、新しいバージョン番号で公開します。

`.gitignore`はtarget/out、JAR/class、既定データ、practice/data/runtime/backups、一時ファイル、`.env`類を除外します。
`--data`で指定した任意のファイル名は自動では除外されません。実データはリポジトリ外に置き、コミット前にステージ内容を確認してください。
資格情報やローカル環境の設定、バックアップは登録しません。
