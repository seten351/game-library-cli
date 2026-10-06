# ゲームライブラリ管理CLI v1.1

> 公開時の復元に関する補足：Javaソースとpom.xmlは記録された内容そのままです。当時のテストは残っていません。
> 以下のREADMEの個人パス・SSH接続情報は公開用の例へ置き換えました。Wrapperは現行版から追加した共通ビルド補助です。
> 復元元とハッシュは [RESTORATION.md](RESTORATION.md) に記録しています。

復元版をビルドするときは、存在しない旧テストの実行を明示的に省略します。

```sh
./mvnw -Dexec.skip=true -Dmaven.test.skip=true clean package
```

以下の旧テスト件数は当時のREADMEの記録です。今回そのテスト一式を復元・再実行したという意味ではありません。
今回の追加操作確認はRESTORATION.mdを参照してください。

Javaのクラス、enum、List、メソッド、ループ、条件分岐を学ぶためのCLIアプリです。
ゲームの登録・一覧表示・タイトル検索を行います。v1.1では、ゲームのデータ構造・管理機能を維持したまま、方向キー操作とSSH向けの起動手順を追加しました。

## 必要な環境

| 用途 | 必要なもの |
| --- | --- |
| ビルド | JDK 21以上（`javac`を含むJDK）、初回はインターネット接続、macOSではcurl |
| JARの実行 | Java 21以上（JREでも可）、UTF-8を扱えるターミナル |
| 方向キー操作 | 標準入力・標準出力がTTYで、`TERM`と端末機能が利用できる環境 |
| 追加のPTY検証 | macOS/LinuxとPython 3.9以上（アプリの実行には不要） |

Mavenの事前インストールは不要です。付属のMaven WrapperがMaven 3.10.0を取得します。
入力部分のみ[JLine 4.4.6のJava 11–21向け配布物](https://github.com/jline/jline3#java-11-21-compatibility-jdk11-classifier)を使用します。GUIやアプリ用フレームワークは追加していません。
コンパイルはUTF-8・`release 21`で行い、依存関係は実行JARに同梱します。

## ビルド・起動

プロジェクト直下で実行します。

```sh
cd game-library-cli
./mvnw clean verify
java -jar target/game-library-cli-1.1.0.jar
```

`JAVA_HOME`はJREではなくJDK 21以上を指定してください。PATH上の`java`とJDKが異なる場合、使用するJavaを明示できます。この作業環境ではJDK 22があり、Java 21互換でビルドします。

```sh
JAVA_HOME="$(/usr/libexec/java_home -v 22)" ./mvnw clean verify
"$(/usr/libexec/java_home -v 22)/bin/java" -jar target/game-library-cli-1.1.0.jar
```

他の環境では、インストール済みJDKの場所を`JAVA_HOME`に指定してください。JDK 22は必須ではありません。
生成物は`target/`にまとまります。起動するのは依存関係入りの`game-library-cli-1.1.0.jar`です。`original-`で始まるJARは配布・起動用ではありません。

初回はMavenと依存関係をダウンロードするため、ネットワーク接続が必要です。取得後はキャッシュを使用できます。Mavenの配布物のSHA-256をWrapper設定に固定しています。

### 作業場所に依存しない起動

```sh
java -jar target/game-library-cli-1.1.0.jar
```

`java -jar`と引用した絶対パスを使うと、現在の作業ディレクトリや手動のクラスパス設定に依存しません。
v1の`src/main/java/gamelibrary/*.java`による手動コンパイルや`out/main`からの起動は、v1.1では使いません。

## 操作

メインメニュー・プラットフォーム・ジャンルに対応します。

| 操作 | 動作 |
| --- | --- |
| ↑ / ↓ | 表示順に選択。上下端で停止し、循環しません |
| Enter | 選択中の項目を確定。初期選択は各メニューの先頭です |
| 番号を入力 → Enter | 従来どおり番号を指定して確定 |
| 番号入力中に↑ / ↓ | 最後に有効だった項目から方向キー操作へ戻る |
| Backspace | 入力した番号や文字を削除 |
| Ctrl+C / EOF | 終了。登録途中のゲームは追加しません |

番号付きの項目一覧の下に、選択番号と項目名のヒントを表示します。タイトル・検索語・プレイ時間は通常の文字入力です。
番号入力の最初の文字は初期選択番号を置き換えます。複数桁は入力全体で検証し、`12`を`2`として扱うことはありません。

| 番号 | 機能 |
| --- | --- |
| 1 | ゲームを登録 |
| 2 | 全ゲームを登録順で表示 |
| 3 | タイトルを部分一致検索 |
| 0 | 終了 |

番号だけでも、`1` → `Echo` → `1`（PC）→ `2`（RPG）→ `1.5`で登録できます。各入力をEnterで確定してください。
その後、`2`で一覧表示、`3` → `ech`で検索、`0`で終了します。

### 番号入力モード

```sh
java -jar target/game-library-cli-1.1.0.jar --plain
```

`--plain`、非TTY、`TERM=dumb`または未設定、端末機能不足、端末初期化失敗時は行単位の番号入力へ切り替えます。このモードでは方向キーを使用せず、番号とEnterで操作してください。空の番号は不正入力として再入力を求めます。
制御シーケンスを出力せず、パイプ・入力リダイレクトにも対応します。`/dev/tty`へ迂回してパイプ入力を無視することはありません。

文字入力でのCtrl+Dは、空の入力行でEOFになります。文字がある場合は通常の行編集に従います。メニューではCtrl+Dで終了できます。

### データのルール

- タイトルは必須で、前後の空白を除去します。日本語も使用できます。
- プラットフォームは`PC / PLAYSTATION / NINTENDO_SWITCH / XBOX / MOBILE / OTHER`から選択します。
- ジャンルは`ACTION / RPG / ADVENTURE / SIMULATION / STRATEGY / SPORTS / PUZZLE / OTHER`から選択します。
- 画面には各enumの表示名を表示します。`OTHER`は「その他」で、追加の自由入力はありません。
- プレイ時間の単位は時間です。`0`や`1.5`など、0以上の有限の数値を入力できます。小数点は`.`を使います。
- 負数、数値以外、無限大、NaNは受け付けません。
- 同じタイトルも登録できます。登録順を維持し、一覧・検索結果には4項目すべてを表示します。
- 検索はタイトルだけが対象です。前後の空白を除き、大文字・小文字を区別せず部分一致で探します。空の検索語は受け付けません。
- 不正な入力には説明を表示し、その項目の再入力を求めます。
- 入力終端（EOF）では正常終了します。登録途中のゲームは追加しません。

**v1.1もメモリ内管理です。終了すると登録内容は消え、次の起動では空になります。**
ファイル保存、GUI、編集、削除、並べ替えは実装範囲に含みません。

## 構成と学習ポイント

```text
game-library-cli/
├── src/main/java/gamelibrary/  # アプリ本体・入力アダプタ
├── src/test/java/gamelibrary/  # main形式のJavaテスト
├── scripts/verify_cli.py       # JARの非対話・PTY検証
├── .mvn/wrapper/               # Maven Wrapperのバージョン設定
├── mvnw / mvnw.cmd
├── pom.xml
└── README.md
```

```text
Main（入力方式を選択し、端末の生存期間を管理）
  ├─ PlainCliInput（Scannerで行単位の番号入力）
  └─ JLineCliInput（LineReaderで方向キー・番号・文字入力）
             ↓ CliInputインターフェース
       GameLibraryCli（入力検証・登録/一覧/検索・表示）
             ↓
       GameLibrary（List<Game>の管理）
             ↓
       Game（Platform・Genreと4項目のデータ）
```

| クラス・enum | 役割・学習ポイント |
| --- | --- |
| `Game` | v1から変更なし。コンストラクタで検証し、private finalフィールドとgetterで値を保持・参照 |
| `Platform` / `Genre` | v1から変更なし。固定選択肢と表示名をenumで定義 |
| `GameLibrary` | v1から変更なし。`List<Game>`を`ArrayList`で保持し、通常のループで検索 |
| `GameLibraryCli` | 入力を受け取り、数値変換・検証・処理・表示を担当。ゲームの処理を端末操作から切り離す |
| `CliInput` / 2つの実装 | 入力方式を差し替える小さなインターフェース。文字入力・選択番号の文字列を返し、入力終了はnull |
| `MenuOption` / `MenuSelection` | 入力用の番号・表示名と選択状態。ゲームのデータ構造とは独立 |
| `Main` | 入力方式を選択。JLineのTerminalをtry-with-resourcesで閉じる |

JLine側は1つの`LineReader`で全入力を読み、Scannerと同時に標準入力を読みません。メニュー中だけ専用のキー設定を使い、文字入力に戻る前に復元します。
方向キーの解釈・行の再描画・端末属性の復元はJLineへ任せます。メニューの選択状態は端末から独立してテストできます。
入力履歴はファイルに保存せず、タイトルの`!`や引用符もそのまま受け取ります。
従来の`GameLibraryCli(GameLibrary, Scanner, PrintStream)`コンストラクタも残し、v1のテストと番号入力を維持します。

`GameLibrary`の公開メソッドはv1と同じです。

- `void addGame(Game game)`：ゲームを追加。nullは受け付けません。
- `List<Game> getGames()`：登録順の一覧を返します。
- `List<Game> searchByTitle(String keyword)`：部分一致したゲームを登録順で返します。nullや空白だけの検索語は受け付けません。

一覧と検索結果は新しいListを返します。返却されたListを変更しても内部の登録内容は変わりません。
`Game`自体も作成後に変更できないため、データの整合性を保てます。
不正なモデル値やAPI引数には`IllegalArgumentException`を使います。

`Game`と`GameLibrary`はコンソール入出力に依存しません。将来のGUI版やファイル保存でも同じ管理部分を利用できます。v1.1では保存用の階層やインターフェースを追加しません。

## MacBook AirからMac miniへのSSH

### 配置と起動

開発元は現在の場所に残し、SSH用JARはMac miniのDocuments外へ配置します。
Mac miniのローカルターミナルで、ビルド済みプロジェクト直下から実行してください。

```sh
mkdir -p "$HOME/Applications/game-library-cli"
cp target/game-library-cli-1.1.0.jar "$HOME/Applications/game-library-cli/"
```

MacBook Air側で接続し、Mac mini側のJavaで起動します。

```sh
ssh game-host
java -version
java -jar "$HOME/Applications/game-library-cli/game-library-cli-1.1.0.jar"
```

Air側でビルドしたJARを転送する場合、Mac miniに上記の配置先を作成した後、Air側から`scp`できます。

```sh
scp target/game-library-cli-1.1.0.jar game-host:Applications/game-library-cli/
```

リモートコマンドとして直接起動するときは、PTYを割り当てます。

```sh
ssh -t game-host 'java -jar "$HOME/Applications/game-library-cli/game-library-cli-1.1.0.jar"'
```

リモートのJavaがPATH上にない場合は、Mac miniにインストールしたJava 21以上の実行ファイルを絶対パスで指定してください。
端末種類は接続元から引き継がれるため、SSH先で`TERM`を根拠なく書き換える必要はありません。

PTYを使わず番号で操作する例です。

```sh
ssh -T game-host 'java -jar "$HOME/Applications/game-library-cli/game-library-cli-1.1.0.jar" --plain' <<'EOF'
1
Echo
1
2
1.5
2
3
ech
0
EOF
```

### v1のSSHエラーの調査記録

v1の調査時にSSH接続先から報告された事実は次のとおりです。

- 作業ディレクトリは現在のプロジェクト直下。
- ソースディレクトリは存在するが、`ls -la src/main/java/gamelibrary`で`Operation not permitted`。
- `out/main/gamelibrary/Main.class`の存在は確認できる。
- `*.java`を使用したコマンドはzshの`no matches found`で失敗。

ソースを列挙できないため、ワイルドカード展開が失敗していると考えられます。コンパイラの実行より前の問題です。
DocumentsへのmacOSアクセス保護が有力ですが、権限設定の実状態は確認していません。`Main.class`の存在だけでは、Javaから内容を読めることも、当時のクラスパスが正しかったことも確認できません。

[AppleはDocuments等へのアクセスを保護](https://support.apple.com/en-au/guide/security/secddd1d86a6/web)し、[リモートログイン設定にディスクアクセスの選択肢](https://support.apple.com/guide/mac-help/allow-a-remote-computer-to-access-your-mac-mchlp1066/mac)を用意しています。
今回の方針はSSH用JARを`~/Applications/game-library-cli`に配置することです。アプリはシステムの権限設定を変更しません。

SSH先で次の読み取り確認を行うと、配置・Java・PTYを切り分けられます。

```sh
pwd
ls -l "$HOME/Applications/game-library-cli/game-library-cli-1.1.0.jar"
java -version
tty
printf '%s\n' "$TERM"
```

## テスト・動作確認

```sh
./mvnw clean verify
python3 scripts/verify_cli.py
```

Javaテストは外部テストライブラリを使用しません。失敗時の`AssertionError`はMavenのビルド失敗になります。`-ea`は不要です。

| テスト | 確認内容 |
| --- | --- |
| `GameLibraryTest` | モデル検証、登録順、重複、検索、返却Listの独立性 |
| `GameLibraryCliTest` | 番号による登録→一覧→検索→終了、不正入力、日本語、各入力段階のEOF |
| `MenuSelectionTest` | 初期選択、上下端、表示順、番号への切り替え、複数桁、貼り付け、Backspace、enumの最終項目 |
| `scripts/verify_cli.py` | 実行JARのパイプ入力、PTYの矢印・番号・切り替え、日本語、貼り付け、画面サイズ変更、Ctrl+C/EOF、端末属性の復元、番号入力への切り替え |

Javaの実行ファイルとJARの場所も指定できます。

```sh
python3 scripts/verify_cli.py --java '/Javaの配置先/bin/java' --jar '/JARの配置先/game-library-cli-1.1.0.jar'
```

PTY自動テストはローカルの端末検証です。SSHの認証・転送・実際の通信経路を確認したことにはなりません。

### SSH実機での確認手順

MacBook Airから`ssh game-host`で接続し、次を確認します。

1. 配置したJARがJava 21以上で起動する。
2. メインメニューの↑↓・Enterで登録を選ぶ。
3. 日本語タイトル、方向キーによるプラットフォームとジャンル、小数のプレイ時間を登録する。
4. 一覧とタイトル検索で4項目・登録順を確認する。
5. 番号入力でも同じ操作ができ、不正な番号や時間は再入力になる。
6. 画面サイズを変更しても選択できる。
7. 通常終了・メニューでのCtrl+D・登録途中のCtrl+C後にシェルの文字入力と表示が正常に戻る。
8. `--plain`と`ssh -T`の入力例でも動作する。

検証前後に`stty -g`を実行すれば端末属性も比較できます。確認していない項目は未確認として記録してください。

### 今回の検証状況

2026年10月6日、Mac mini上のmacOS環境で確認しました。

| 項目 | 結果 |
| --- | --- |
| Maven Wrapperのビルド・JAR生成 | 成功。JDK 22で`release 21`を指定 |
| Javaテスト | Java 21.0.11で102項目成功（既存82＋選択状態20） |
| 実行JARの非対話・PTY検証 | Java 21.0.11で82項目成功。Ctrl+C/EOF/通常終了後の端末属性も一致 |
| ゲーム管理クラス・enum | 4ファイルのSHA-256が変更前と一致 |
| Documents外への配置 | `~/Applications/game-library-cli/game-library-cli-1.1.0.jar`に配置済み。ビルド元とハッシュ一致 |
| 配置JARの起動 | 別の作業ディレクトリから、番号入力で登録→一覧→検索→終了を確認 |
| MacBook Air→Mac miniのSSH実機操作 | **確認済み（ユーザー確認）**。2026年10月6日、MacBook AirからMac miniへSSH接続して動作を確認したとの報告を受領 |

SSH実機の結果はユーザーによる確認報告に基づきます。ローカルの自動テスト82項目とは別の検証として記録しています。

生成物は`target/`、v1の旧生成物は`out/`にあり、Git管理対象から除外します。
Mavenの生成物を消す場合はプロジェクト直下で`./mvnw clean`を実行してください。
