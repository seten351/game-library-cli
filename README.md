# ゲームライブラリ管理CLI v1.0

ゲームの登録・一覧・タイトルの部分一致検索を学ぶ、Java 21互換の番号メニュー式CLIです。
タイトル・プラットフォーム・ジャンル・プレイ時間をメモリ内に保持します。終了すると登録内容は消えます。

このREADMEは公開用に新しく作成した復元ガイドです。当時のREADME・テスト・ビルド設定・JARは残っていません。
アプリのJavaソース6件は記録された教材スナップショットから変更せず復元しました。
復元元・ファイルのSHA-256・追加した補助は [RESTORATION.md](RESTORATION.md) を参照してください。

## ビルドと起動

ビルドにはJDK 21以上が必要です。外部ライブラリはありません。macOS/Linuxでは次のように実行します。

```sh
cd game-library-cli
mkdir -p target/classes
javac --release 21 -encoding UTF-8 -d target/classes src/main/java/gamelibrary/*.java
jar --create --file target/game-library-cli-1.0.0.jar --main-class gamelibrary.Main -C target/classes .
java -jar target/game-library-cli-1.0.0.jar
```

メニューの1で登録、2で一覧、3で検索、0で終了します。プラットフォームとジャンルも番号で選びます。
検索は大文字・小文字を区別しません。空のタイトル、不正な番号、負数や数値でないプレイ時間は再入力になります。
v1.0には方向キー、`--plain`、ファイル保存の機能はありません。

## Gitで版を切り替える

```sh
git switch --detach v1.0.0
# 読み終えたら現行の開発ブランチへ戻る
git switch main
```

配布JARとSHA256SUMS.txtは [GitHub Releases](https://github.com/seten351/game-library-cli/releases) にあります。
ビルド成果物と個人データはGitへ登録しません。
