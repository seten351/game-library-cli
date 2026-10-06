# ゲームライブラリ管理CLI v2.0

> 公開時の復元に関する補足：Javaソースとpom.xmlは記録された内容そのままです。当時のテストは残っていません。
> 以下のREADMEの個人パス・SSH接続情報は公開用の例へ置き換えました。Wrapperは現行版から追加した共通ビルド補助です。
> 復元元とハッシュは [RESTORATION.md](RESTORATION.md) に記録しています。

復元版をビルドするときは、存在しない旧テストの実行を明示的に省略します。

```sh
./mvnw -Dexec.skip=true -Dmaven.test.skip=true clean package
```

以下の旧テスト件数は当時のREADMEの記録です。今回そのテスト一式を復元・再実行したという意味ではありません。
今回の追加操作確認はRESTORATION.mdを参照してください。

Javaのクラス、enum、List、メソッド、例外、インターフェースとファイル入出力を学ぶCLIアプリです。
ゲームの登録・一覧表示・タイトル検索に、JSONによる基本的な永続化を追加しました。
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
java -jar target/game-library-cli-2.0.0.jar
```

JAVA_HOMEにはJREではなくJDK 21以上を指定します。この開発環境のJDK 22でもJava 21互換でビルドできます。

```sh
JAVA_HOME="$(/usr/libexec/java_home -v 22)" ./mvnw clean verify
```

Mavenの事前インストールは不要です。WrapperがMaven 3.10.0を取得し、配布物のSHA-256を検証します。
コンパイルはUTF-8・release 21で行い、JLine 4.4.6（jdk11配布物）とGson 2.14.0を実行JARに同梱します。
外部のアプリ用フレームワークは使いません。初回の依存関係取得後はキャッシュを使用できます。
起動・配布するのは `target/game-library-cli-2.0.0.jar` です。original-で始まるJARは配布用ではありません。
旧v1のoutディレクトリや手動のjavacコマンドは、今回の起動には使いません。

## 保存先とJSON形式

既定の保存先は `~/.game-library-cli/games.json` です。起動時に絶対パスを表示します。
作業ディレクトリを変えても同じホームのデータを使います。SSHでは接続先ユーザーのホームです。
初回にファイルがなければ空で開始し、初めて登録が完了したときに親ディレクトリとファイルを作成します。

```sh
java -jar target/game-library-cli-2.0.0.jar --data /tmp/my-learning-games.json
java -jar target/game-library-cli-2.0.0.jar --plain --data './practice/games.json'
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
| 0 | 終了 |

メインメニュー・プラットフォーム・ジャンルは↑↓で選択し、Enterで確定できます。
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
java -jar target/game-library-cli-2.0.0.jar --plain
```

--plain、非TTY、TERM=dumb・未設定、端末機能不足、端末初期化失敗では行単位の番号入力へ切り替えます。
このモードでは番号とEnterを使います。空の番号は不正入力です。
制御シーケンスを出力せず、パイプや入力リダイレクトを読みます。/dev/ttyへ迂回しません。
入力はJLineかScannerのいずれか一方が担当します。

Ctrl+C / EOFで終了します。登録途中のゲームは追加・保存しません。
完了済みの登録はその時点で保存しています。文字入力中のCtrl+Dは空の行でEOFになります。
一覧・検索・通常終了・入力終了だけではファイルを書き換えません。

## 読み込み・保存に失敗した場合

| 状況 | v2.0の動作 |
| --- | --- |
| 保存ファイルが存在しない | 空で開始。登録するまでファイルを作成しない |
| 壊れたJSON、空ファイル、値の不正、未対応形式、読み取り失敗 | 原因と保存先を案内し、この起動中の保存を無効にする。元ファイルを変更せずメモリ内で操作を続ける |
| 登録後の書き込み失敗 | 原因・保存先・未保存を案内。登録をメモリ内に保持し、メニューを続ける |
| 書き込み問題を解消して次の通常登録が成功 | 以前の未保存分を含む全件を保存する |
| 未保存の登録がある状態で終了 | 終了すると未保存分が失われる旨を表示する。終了時の保存は行わない |

読み込み失敗では保存済みゲームを復元できていません。表示するのは、その実行中に新しく登録した内容だけです。
元ファイルを別途退避・修復するか、別の--data保存先を指定して再起動してください。
保存無効になった実行中は、次に登録しても保存を行いません。

v2.0は基本的な入出力の理解に集中し、ファイルを直接上書きします。
書き込み途中の失敗では、以前のファイルが途中まで書き換わる可能性があります。
同じ保存先を複数のアプリで同時に使うと上書きが競合するため、1つのアプリで使ってください。
原子的置換、ファイルロック、再試行UI、複雑な再試行、永続化専用の終了コード整理はv2.1の学習対象です。
自動バックアップ・編集・削除・DB・GUI・Web版は今回の実装範囲に含みません。

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
Main（保存先・入力方式を選び、起動時にloadを呼ぶ）
  ├─ PlainCliInput / JLineCliInput → CliInput
  └─ GameLibraryCli（入力検証・表示）
          ↓
     GameLibraryService（起動時復元・登録後保存の連携）
       ├─ GameLibrary → List<Game> → Game / Platform / Genre
       └─ GameStore ← JsonGameStore → JSONファイル
```

| 型 | 役割と設計理由 |
| --- | --- |
| Game / Platform / Genre | v1から変更なし。コンストラクタの検証、private finalとgetter、固定選択肢を学ぶ |
| GameLibrary | v1から変更なし。ArrayListで保持し、ループで検索。ファイルや端末を知らない |
| GameStore | load / saveを宣言する小さなインターフェース。管理側がJSONの具体的な書き方に依存しないための契約 |
| JsonGameStore | Path、Files、UTF-8、Gsonを使ったJSON変換と入出力。失敗は保存先と原因を含むIOExceptionで伝える |
| GameLibraryService | GameLibraryとGameStoreを連携。復元・登録後保存、保存有効／未保存の状態を担当し、表示はしない |
| GameLibraryCli | Serviceを呼び、入力検証・一覧・検索・保存結果の表示を担当。JSONの読み書きを埋め込まない |
| CliInput / PlainCliInput / JLineCliInput | v1.1から変更なし。端末操作を管理や保存から分離する |
| MenuOption / MenuSelection | v1.1から変更なし。番号・表示名と選択状態を扱う |
| Main | Pathと具体的な実装を組み立てる。起動時の読み込みと端末の生存期間を管理する |

起動時は `Main → Service.load → Store.load → GameLibraryへ復元` と流れます。
登録時は `CLIで全項目入力 → Game作成 → Service → メモリへ追加 → Storeで全件保存 → 結果表示` です。
GameLibraryの責務を増やさず、入力方式も変更せずに永続化を追加できる構成です。
DB等への実装差し替えを考えられる契約だけを追加し、未使用のRepository階層やファクトリー等は導入しません。

GameLibraryのaddGame / getGames / searchByTitleは従来どおりです。
一覧・検索は新しいListを返すので、呼び出し側が返却Listを変えても内部の登録内容は変わりません。
Gameは作成後に変更できません。JSONからもGameのコンストラクタを通し、不正な値を取り込みません。
CLIのScanner用コンストラクタも残し、依存先をGameLibraryServiceへ変更しています。
テストではGameStoreを簡単なメモリ用実装に差し替え、実データに触れず入力処理を確認します。

v2.0ではインターフェース・連携役・例外処理・JSONとJavaの変換を学びます。
v2.1では原子的置換・排他制御・復旧操作など、ファイル更新の信頼性を段階的に学ぶ予定です。

## MacBook AirからMac miniへのSSH

開発元はDocumentsに残し、SSH用JARはDocuments外へ配置できます。
Mac miniのローカルターミナルで、ビルド済みプロジェクト直下から実行します。

```sh
mkdir -p "$HOME/Applications/game-library-cli"
cp target/game-library-cli-2.0.0.jar "$HOME/Applications/game-library-cli/"
```

MacBook Airで接続し、接続先のJava 21以上で起動します。

```sh
ssh game-host
# IPで接続する場合: ssh user@game-host
java -version
java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.0.0.jar"
```

Air側からJARを転送する場合、配置先の作成後に `scp target/game-library-cli-2.0.0.jar game-host:Applications/game-library-cli/` を使えます。
リモートコマンドとして方向キーを使う場合はPTYを割り当てます。

```sh
ssh -t game-host 'java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.0.0.jar"'
```

JavaがPATHにない場合は実行ファイルの絶対パスを指定してください。
TERMを根拠なく書き換える必要はありません。TTYがなければ番号入力へ切り替わります。

以前のv1ではDocuments内のソース列挙がOperation not permittedとなり、zshのワイルドカード展開にも失敗していました。
macOSのDocumentsアクセス保護が有力な原因でした。JARをDocuments外へ置く手順を維持します。
アプリはOSのアクセス権やSSH設定を変更しません。v2.0の既定データもホーム直下の専用ディレクトリです。

### 実データを使わないSSH確認

SSH接続後、専用の一時フォルダで確認できます。

```sh
test_dir=$(mktemp -d /tmp/game-library-v2-ssh.XXXXXX)
java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.0.0.jar" --data "$test_dir/games.json"
# 登録して終了後、同じ保存先で再起動し一覧・検索を確認
java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.0.0.jar" --data "$test_dir/games.json"
java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.0.0.jar" --plain --data "$test_dir/games.json"
# 確認に使った専用フォルダだけを削除
rm -r "$test_dir"
```

方向キー・番号の切り替え、日本語、小数、登録順、再起動復元、通常終了・途中Ctrl+C/EOFを確認します。
検証前後のstty -gを比較すれば端末属性の復元も確認できます。
非TTYの確認は、別の専用一時領域を使う次の例で実行できます。

```sh
ssh -T game-host 'sh -s' <<'REMOTE'
test_dir=$(mktemp -d /tmp/game-library-v2-ssh.XXXXXX) || exit 1
trap 'rm -r "$test_dir"' EXIT
java -jar "$HOME/Applications/game-library-cli/game-library-cli-2.0.0.jar" --plain --data "$test_dir/games.json" <<'INPUT'
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
| MenuSelectionTest（20項目） | 初期選択、上下端、番号切り替え、複数桁、貼り付け、Backspace |
| PersistenceTest（157項目） | 全enum・日本語・記号・小数のJSON往復、全件検証、保存無効・元ファイル保護、保存失敗後のメモリ保持と次の登録での保存 |
| verify_cli.py（119項目） | 実行JARのPTY・非対話入力、方向キー・番号・plain・フォールバック、端末復元、起動復元、即時保存、読み書き失敗、権限不足、途中中断 |

JavaとJARを明示することもできます。

```sh
python3 scripts/verify_cli.py --java '/Javaの配置先/bin/java' --jar '/JARの配置先/game-library-cli-2.0.0.jar'
```

2026年10月6日のv2.0検証結果：

| 環境・対象 | 結果 |
| --- | --- |
| ローカル：Mavenビルド・JAR生成 | 成功。JDK 22でrelease 21を指定 |
| ローカル：Javaテスト | Java 21.0.12.1で259項目成功 |
| ローカル：実行JARのPTY・非対話・JSON検証 | Java 21.0.12.1で119項目成功 |
| ローカル：ゲーム管理・入力関連 | Game / GameLibrary / Platform / Genreと入力関連5ファイルは変更前のSHA-256と一致 |
| SSH実機：SSH設定のHost別名 | 未確認。自動接続の認証が拒否された |
| SSH実機：user@game-host | ホスト鍵は登録済みMac miniと一致したが、認証が拒否されたためv2.0の動作は未確認 |

SSH接続の失敗はPermission denied（publickey,password,keyboard-interactive）です。
ローカルPTYテストはSSH実機検証の代用ではありません。
v1.1は以前MacBook Airからのユーザー確認報告を受けていますが、v2.0の実機成功としては扱いません。
今回の作業ではSSH用の配置済みv1.1 JARや利用者の実データを変更していません。

生成物はtarget、v1の旧生成物はoutで、Git管理対象から除外します。
既定データはプロジェクト外です。独自の保存先・バックアップもGitへ含めないでください。
生成物だけを消す場合はプロジェクト直下で `./mvnw clean` を実行します。
