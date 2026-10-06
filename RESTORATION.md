# 復元・公開記録（収録タグ v2.1.0）

2026年10月6日。これはGit導入時に作成した記録です。過去の開発コミット・日時を再現したものではありません。
Javaソースを推測で補完せず、保存されていた完全なソース文字列をUTF-8で復元しました。バイナリからソースは生成していません。
全版のJavaソースと、残っていたpom.xmlは復元元とバイト単位で一致します。公開用の文書・ビルド補助の変更は下記で区別します。

## 検証と成果物

| 版 | ローカル検証（Java 21） | 既存バイナリとの比較 |
| --- | --- | --- |
| v1.0.0 | 新規操作確認7項目成功 | 旧class 6/6件が一致。旧JARは未発見 |
| v1.1.0 | 新規操作確認32項目成功、原本JARも32項目成功 | class 11/11件とJAR全体のSHA-256が一致 |
| v2.0.0 | 新規操作確認37項目成功、原本JARも37項目成功 | class 14/14件とJAR全体のSHA-256が一致 |
| v2.1.0 | 既存Javaテスト305項目成功、CLI統合155項目成功 | Git導入直前の全Javaソース・pom.xml・テストを維持 |

ビルドはJDK 22のrelease 21で実施し、各JARのアプリclassがmajor version 65であることを確認しました。
v2.1の305項目はMaven verifyで実行し、さらにJava 21で4つのテストmainを実行して成功しました。
操作確認は番号・登録・一覧・検索・入力不備・EOFを扱い、v1.1以降ではローカルPTYの方向キー・plain・TERM=dumb・Ctrl+C/D・端末属性復元も確認しました。
v2.0ではJSON保存と再起動、不正JSONの維持も確認しています。全検証は一時ホーム／一時保存先を使い、利用者の実データを変更していません。

v2.1のCLI統合確認の初回は、未保存状態から終了した際の端末属性の比較が1回失敗しました。
同じソース・同じJARで終了操作だけの再検証60項目、その後の全体再検証155項目は成功しました。ソースや判定条件は変更していません。
一過性の失敗の原因は未確定です。繰り返し実行・実端末での挙動を今後も確認する必要があります。

今回SSH実機の再検証は行っていません。v1.1のMBAからの動作確認は利用者からの過去の報告です。
v2.0/v2.1のSSH実機確認は未完了で、ローカルPTYや模擬入力をSSH実機の成功として数えていません。

以下のJARは今回ソースからビルドした配布物です。v1.1/v2.0は手元の原本JARも変更せず別名で添付します。

| 配布JAR | SHA-256 |
| --- | --- |
| `game-library-cli-1.0.0.jar` | `8908514ee245530dd1f8719d18a897ecf457fdbb393f1af0b4ec2bc27ced9159` |
| `game-library-cli-1.1.0.jar` | `bf081e7e7c11fc8bba50e85056fda745bb18f1ab9efbde72fccf856f106ce505` |
| `game-library-cli-1.1.0-original.jar` | `bf081e7e7c11fc8bba50e85056fda745bb18f1ab9efbde72fccf856f106ce505` |
| `game-library-cli-2.0.0.jar` | `69f9d29de459eae00ae37dabff4ced85e3323b2f60a08d253ce860c7c19275ab` |
| `game-library-cli-2.0.0-original.jar` | `69f9d29de459eae00ae37dabff4ced85e3323b2f60a08d253ce860c7c19275ab` |
| `game-library-cli-2.1.0.jar` | `57d4fe97e1548148544ee892b4b6138bfc7dadeb307f6dce681eda70fd62a7a1` |

各ReleaseのSHA256SUMS.txtにも同じ値を記録します。JAR/classや保存データはGitへ含めません。

## 復元元と不足資料

過去版はローカルに残っていた教材サイトリポジトリ `java-game-library-lessons` のGitオブジェクトにある `sources.json` の `files` を使用しました。
下記コミットはこのアプリの新しいGit履歴のコミットではありません。教材側の不変な復元元を識別する値です。
非公開情報を含み得る元スナップショットや元README全体は添付しません。原文のハッシュと出典を記録します。

### v1.0.0

- 元コミット: `70f6bfdad014c7e9b6193064591bac664d9534df`
- 元ファイル: `dist/versions/v1/sources.json`
- 元JSON全体のSHA-256: `dfb76bd150fe274614b759e0fca8623bc646a4b2de98b4e772df0e6817b44fb3`
- 不足資料: 当時のテスト一式、当時のREADME・ビルド設定・JAR、元のGit履歴・作業ディレクトリ全体
- 新規の補助: .gitignore、RESTORATION.md、README.md（復元時に作成）
- 公開用変更: 当時のJavaソースには変更なし（新規のREADME・除外設定は上記の補助）

### v1.1.0

- 元コミット: `aa19454b9c0c072e85e302d685e1b7a332c2afcb`
- 元ファイル: `dist/versions/v1.1/sources.json`
- 元JSON全体のSHA-256: `e2cf8b22942dbcb99037e5fa390bd8a5c1a953b42484958b2d583a95f62c2fa3`
- 不足資料: 当時のテスト一式、元のGit履歴・作業ディレクトリ全体
- 新規の補助: .gitignore、RESTORATION.md、mvnw、mvnw.cmd、.mvn/wrapper/maven-wrapper.properties（現行の共通ビルド補助）
- 公開用変更: README.md

### v2.0.0

- 元コミット: `f2c14f8b2176d52be7a0f8b50ad1d987f27f9dff`
- 元ファイル: `dist/versions/v2.0/sources.json`
- 元JSON全体のSHA-256: `63e6ada6a3cefab97de220de7259f64c9f92c03f8fa692b3ee86034bbdedc7d9`
- 不足資料: 当時のテスト一式、元のGit履歴・作業ディレクトリ全体
- 新規の補助: .gitignore、RESTORATION.md、mvnw、mvnw.cmd、.mvn/wrapper/maven-wrapper.properties（現行の共通ビルド補助）
- 公開用変更: README.md

### v2.1.0

- 元資料: Git導入直前に確定した現行v2.1のソース・設定・テスト26ファイル。元Gitコミットはありません。
- 不足資料: 元のGit履歴・作業ディレクトリ全体
- 新規の補助: RESTORATION.md
- 公開用変更: README.md、.gitignore

READMEの個人用絶対パス、LANアドレス、SSHユーザー／Host別名は汎用の例へ置き換えました。
旧READMEには復元の注意とビルド手順を追記し、現行READMEにはGit運用の説明を追加しました。
全版に公開用.gitignoreとこの文書を追加し、v1.1/v2.0には現行のMaven Wrapper（3.3.4、Maven 3.10.0、配布物SHA-256検証付き）を再利用しました。
これらは当時存在したファイルと主張するものではありません。v1.0のREADMEとjavac/jar手順も今回追加したものです。

## 再ビルド

v1.0はREADMEの `javac --release 21` と `jar --create` の手順を使います。
v1.1/v2.0は旧テストがないため、次のコマンドで復元したアプリのみをビルドします。テスト成功とは扱いません。

```sh
./mvnw -Dexec.skip=true -Dmaven.test.skip=true clean package
```

v2.1は `./mvnw clean verify` と `python3 scripts/verify_cli.py` を使用します。Python 3.9以上とmacOS/Linuxが必要です。
旧版向けの今回の追加操作検証は公開準備用の一時補助であり、失われた当時のテストの代わりを復元したものではありません。

## ファイル単位のSHA-256

原文の列は復元元にあった内容、公開の列はタグに収録した内容です。同じ値はバイト単位で変更がないことを表します。
新規ファイルは原文が存在しません。このRESTORATION.md自身は自己参照のためハッシュ表から除き、Gitオブジェクトで管理します。

### v1.0.0のファイル

| パス | 原文SHA-256 | 公開SHA-256 |
| --- | --- | --- |
| `src/main/java/gamelibrary/Game.java` | `e776a270760f8ce1bc3a02d1bbb4d9b802d30e6d57750688cfed91511558d493` | `e776a270760f8ce1bc3a02d1bbb4d9b802d30e6d57750688cfed91511558d493` |
| `src/main/java/gamelibrary/GameLibrary.java` | `c4b09e29d1f303e0ebc31f560a6816b00ae36d0a4dea57d12960f30118eac173` | `c4b09e29d1f303e0ebc31f560a6816b00ae36d0a4dea57d12960f30118eac173` |
| `src/main/java/gamelibrary/GameLibraryCli.java` | `6216844bc3be0147e8085cdd35357b222e972ad9cd7d86c816234e6b77642f2f` | `6216844bc3be0147e8085cdd35357b222e972ad9cd7d86c816234e6b77642f2f` |
| `src/main/java/gamelibrary/Genre.java` | `2a9d21793ee233c6d99eaeddc14aadedb635146aed36b15b81cdc18b6227f5e2` | `2a9d21793ee233c6d99eaeddc14aadedb635146aed36b15b81cdc18b6227f5e2` |
| `src/main/java/gamelibrary/Main.java` | `adb3fa356a8746b9d82f0a290b9a8bdae286fb29c5001cf6b5b4d6e7a5396e07` | `adb3fa356a8746b9d82f0a290b9a8bdae286fb29c5001cf6b5b4d6e7a5396e07` |
| `src/main/java/gamelibrary/Platform.java` | `d4eec8de226cdddf8c708b8521bcbe1e37123af2034a2b1b1e7f2cb9aca73c05` | `d4eec8de226cdddf8c708b8521bcbe1e37123af2034a2b1b1e7f2cb9aca73c05` |
| `.gitignore` | 新規補助 | `0b63443aecdbd10be5ee5969dc0de7d8fd4c774ac926976016d7c0c6464ac3b7` |
| `README.md` | 新規補助 | `20022b9be6af1a40f11578d11fa6b10d7b0a48b7fc8322fec3f6d5cfc725278b` |

### v1.1.0のファイル

| パス | 原文SHA-256 | 公開SHA-256 |
| --- | --- | --- |
| `README.md` | `555d3b4174252bd7255e72c6fb7c8715dd435ada5be16e34098dacea8747f07e` | `2fe36bcb04338083c480784c6b0423b1c028cd9ac9fd7e4a45da1d03e057bd98` |
| `pom.xml` | `d1c713c586e53aa8f87d1fcbd958aec2642c45d4a50d8ebea4ba2d1be880f25f` | `d1c713c586e53aa8f87d1fcbd958aec2642c45d4a50d8ebea4ba2d1be880f25f` |
| `src/main/java/gamelibrary/CliInput.java` | `9680f686c33d4358e251dd775c5c55879332bbabc5e6cfb1d48b03d4b8fa4e45` | `9680f686c33d4358e251dd775c5c55879332bbabc5e6cfb1d48b03d4b8fa4e45` |
| `src/main/java/gamelibrary/Game.java` | `e776a270760f8ce1bc3a02d1bbb4d9b802d30e6d57750688cfed91511558d493` | `e776a270760f8ce1bc3a02d1bbb4d9b802d30e6d57750688cfed91511558d493` |
| `src/main/java/gamelibrary/GameLibrary.java` | `c4b09e29d1f303e0ebc31f560a6816b00ae36d0a4dea57d12960f30118eac173` | `c4b09e29d1f303e0ebc31f560a6816b00ae36d0a4dea57d12960f30118eac173` |
| `src/main/java/gamelibrary/GameLibraryCli.java` | `40973209e9297a79b510735d4eecc3b711389b76404964ab3ac73d29266af751` | `40973209e9297a79b510735d4eecc3b711389b76404964ab3ac73d29266af751` |
| `src/main/java/gamelibrary/Genre.java` | `2a9d21793ee233c6d99eaeddc14aadedb635146aed36b15b81cdc18b6227f5e2` | `2a9d21793ee233c6d99eaeddc14aadedb635146aed36b15b81cdc18b6227f5e2` |
| `src/main/java/gamelibrary/JLineCliInput.java` | `a97a905d7e87ff51dbea5e6b1d770363baa02c355cfff38dbe39b124fea67a1f` | `a97a905d7e87ff51dbea5e6b1d770363baa02c355cfff38dbe39b124fea67a1f` |
| `src/main/java/gamelibrary/Main.java` | `c1116c9791e5b883e1c7378296b564d0135aa3cf959adf4f18385f363ab4f538` | `c1116c9791e5b883e1c7378296b564d0135aa3cf959adf4f18385f363ab4f538` |
| `src/main/java/gamelibrary/MenuOption.java` | `792f37b9b66230a12fdd42676b17ae540f972702ab8cfcd1c1bb50cf62514ddc` | `792f37b9b66230a12fdd42676b17ae540f972702ab8cfcd1c1bb50cf62514ddc` |
| `src/main/java/gamelibrary/MenuSelection.java` | `8aaf83273a57db019a39818cc0f47d0f71d868400f066786d9457680705713f0` | `8aaf83273a57db019a39818cc0f47d0f71d868400f066786d9457680705713f0` |
| `src/main/java/gamelibrary/PlainCliInput.java` | `129cf93bcca5f7f5cf1fc015cd74b34259e47407f1e20677d9e54cd7fcd506b7` | `129cf93bcca5f7f5cf1fc015cd74b34259e47407f1e20677d9e54cd7fcd506b7` |
| `src/main/java/gamelibrary/Platform.java` | `d4eec8de226cdddf8c708b8521bcbe1e37123af2034a2b1b1e7f2cb9aca73c05` | `d4eec8de226cdddf8c708b8521bcbe1e37123af2034a2b1b1e7f2cb9aca73c05` |
| `.gitignore` | 新規補助 | `0b63443aecdbd10be5ee5969dc0de7d8fd4c774ac926976016d7c0c6464ac3b7` |
| `mvnw` | 新規補助 | `cae96cef89ebea3531221f4ae17c23cf8edf67d00eae8306d4186ae1bbed4d02` |
| `mvnw.cmd` | 新規補助 | `46eedb8419bd14fe70d5bb2916d7b6f51806e51b39d5b76a42610384ca929c1c` |
| `.mvn/wrapper/maven-wrapper.properties` | 新規補助 | `03e1f35df3e20ff2341e05980c8258a4ce94923884822fc2bab922060989862c` |

### v2.0.0のファイル

| パス | 原文SHA-256 | 公開SHA-256 |
| --- | --- | --- |
| `README.md` | `c82732fd4cd57c793fe46530d58854b0e114bc1fda33ce4fb75a8b9dd9a1627e` | `0065cdda8c431fb8849b7158dc677c36f7f9e0693bec834c857c417b9d452ea4` |
| `pom.xml` | `641524c16cf5cc2d9236f90dd8bf8ed3790e150eee1edeca4593e539b715edc9` | `641524c16cf5cc2d9236f90dd8bf8ed3790e150eee1edeca4593e539b715edc9` |
| `src/main/java/gamelibrary/CliInput.java` | `9680f686c33d4358e251dd775c5c55879332bbabc5e6cfb1d48b03d4b8fa4e45` | `9680f686c33d4358e251dd775c5c55879332bbabc5e6cfb1d48b03d4b8fa4e45` |
| `src/main/java/gamelibrary/Game.java` | `e776a270760f8ce1bc3a02d1bbb4d9b802d30e6d57750688cfed91511558d493` | `e776a270760f8ce1bc3a02d1bbb4d9b802d30e6d57750688cfed91511558d493` |
| `src/main/java/gamelibrary/GameLibrary.java` | `c4b09e29d1f303e0ebc31f560a6816b00ae36d0a4dea57d12960f30118eac173` | `c4b09e29d1f303e0ebc31f560a6816b00ae36d0a4dea57d12960f30118eac173` |
| `src/main/java/gamelibrary/GameLibraryCli.java` | `c4fd0e431a3a45f4daa18aaa7e8503ad3a2cd819e8ebccd6b339d7d9958d23f5` | `c4fd0e431a3a45f4daa18aaa7e8503ad3a2cd819e8ebccd6b339d7d9958d23f5` |
| `src/main/java/gamelibrary/GameLibraryService.java` | `ae3bd945da739e49b4a71530340c986b96aa4958ba8505036d82e9626c9ddf05` | `ae3bd945da739e49b4a71530340c986b96aa4958ba8505036d82e9626c9ddf05` |
| `src/main/java/gamelibrary/GameStore.java` | `afeac7ec704ebf7bd0a1c76e4a8c6ea74647dc068096d6f7b09dc9f1f5e6b0b5` | `afeac7ec704ebf7bd0a1c76e4a8c6ea74647dc068096d6f7b09dc9f1f5e6b0b5` |
| `src/main/java/gamelibrary/Genre.java` | `2a9d21793ee233c6d99eaeddc14aadedb635146aed36b15b81cdc18b6227f5e2` | `2a9d21793ee233c6d99eaeddc14aadedb635146aed36b15b81cdc18b6227f5e2` |
| `src/main/java/gamelibrary/JLineCliInput.java` | `a97a905d7e87ff51dbea5e6b1d770363baa02c355cfff38dbe39b124fea67a1f` | `a97a905d7e87ff51dbea5e6b1d770363baa02c355cfff38dbe39b124fea67a1f` |
| `src/main/java/gamelibrary/JsonGameStore.java` | `41fae3be8ad25936d65e48eb9f8023f2c10deefc5f47e75c2b8ef375ce39e5e2` | `41fae3be8ad25936d65e48eb9f8023f2c10deefc5f47e75c2b8ef375ce39e5e2` |
| `src/main/java/gamelibrary/Main.java` | `66e94ecc405f62d44c48b3b71086fa13448ccaf0ee541bd4ed354be46661bfc3` | `66e94ecc405f62d44c48b3b71086fa13448ccaf0ee541bd4ed354be46661bfc3` |
| `src/main/java/gamelibrary/MenuOption.java` | `792f37b9b66230a12fdd42676b17ae540f972702ab8cfcd1c1bb50cf62514ddc` | `792f37b9b66230a12fdd42676b17ae540f972702ab8cfcd1c1bb50cf62514ddc` |
| `src/main/java/gamelibrary/MenuSelection.java` | `8aaf83273a57db019a39818cc0f47d0f71d868400f066786d9457680705713f0` | `8aaf83273a57db019a39818cc0f47d0f71d868400f066786d9457680705713f0` |
| `src/main/java/gamelibrary/PlainCliInput.java` | `129cf93bcca5f7f5cf1fc015cd74b34259e47407f1e20677d9e54cd7fcd506b7` | `129cf93bcca5f7f5cf1fc015cd74b34259e47407f1e20677d9e54cd7fcd506b7` |
| `src/main/java/gamelibrary/Platform.java` | `d4eec8de226cdddf8c708b8521bcbe1e37123af2034a2b1b1e7f2cb9aca73c05` | `d4eec8de226cdddf8c708b8521bcbe1e37123af2034a2b1b1e7f2cb9aca73c05` |
| `.gitignore` | 新規補助 | `0b63443aecdbd10be5ee5969dc0de7d8fd4c774ac926976016d7c0c6464ac3b7` |
| `mvnw` | 新規補助 | `cae96cef89ebea3531221f4ae17c23cf8edf67d00eae8306d4186ae1bbed4d02` |
| `mvnw.cmd` | 新規補助 | `46eedb8419bd14fe70d5bb2916d7b6f51806e51b39d5b76a42610384ca929c1c` |
| `.mvn/wrapper/maven-wrapper.properties` | 新規補助 | `03e1f35df3e20ff2341e05980c8258a4ce94923884822fc2bab922060989862c` |

### v2.1.0のファイル

| パス | 原文SHA-256 | 公開SHA-256 |
| --- | --- | --- |
| `.gitignore` | `efe806661453469894d50e7879aa469963723fbcf110e5af9bb924d6cc59d421` | `0b63443aecdbd10be5ee5969dc0de7d8fd4c774ac926976016d7c0c6464ac3b7` |
| `.mvn/wrapper/maven-wrapper.properties` | `03e1f35df3e20ff2341e05980c8258a4ce94923884822fc2bab922060989862c` | `03e1f35df3e20ff2341e05980c8258a4ce94923884822fc2bab922060989862c` |
| `README.md` | `78fae8e5e0e15aec3055c9e65c64d736d77f597bce0893d57a6b92b517c0a4bb` | `22185ca359ef3acbed00b28cd75c453479463a337f93ba026cfc3693a51f6fda` |
| `mvnw` | `cae96cef89ebea3531221f4ae17c23cf8edf67d00eae8306d4186ae1bbed4d02` | `cae96cef89ebea3531221f4ae17c23cf8edf67d00eae8306d4186ae1bbed4d02` |
| `mvnw.cmd` | `46eedb8419bd14fe70d5bb2916d7b6f51806e51b39d5b76a42610384ca929c1c` | `46eedb8419bd14fe70d5bb2916d7b6f51806e51b39d5b76a42610384ca929c1c` |
| `pom.xml` | `961f25a64cbb378ae40608d484b7a42a33b540813daee74f4af93f9f6448d5fb` | `961f25a64cbb378ae40608d484b7a42a33b540813daee74f4af93f9f6448d5fb` |
| `scripts/verify_cli.py` | `7e347977001e1c2d25118a0dd6fae98c0595103256ca5c014778ce2080dd4a31` | `7e347977001e1c2d25118a0dd6fae98c0595103256ca5c014778ce2080dd4a31` |
| `src/main/java/gamelibrary/CliInput.java` | `9680f686c33d4358e251dd775c5c55879332bbabc5e6cfb1d48b03d4b8fa4e45` | `9680f686c33d4358e251dd775c5c55879332bbabc5e6cfb1d48b03d4b8fa4e45` |
| `src/main/java/gamelibrary/Game.java` | `e776a270760f8ce1bc3a02d1bbb4d9b802d30e6d57750688cfed91511558d493` | `e776a270760f8ce1bc3a02d1bbb4d9b802d30e6d57750688cfed91511558d493` |
| `src/main/java/gamelibrary/GameFileLock.java` | `4143c0f815f99b661350e4e5dd3eef0b1c836757f7d0dbc68cadbda5efd52eed` | `4143c0f815f99b661350e4e5dd3eef0b1c836757f7d0dbc68cadbda5efd52eed` |
| `src/main/java/gamelibrary/GameLibrary.java` | `c4b09e29d1f303e0ebc31f560a6816b00ae36d0a4dea57d12960f30118eac173` | `c4b09e29d1f303e0ebc31f560a6816b00ae36d0a4dea57d12960f30118eac173` |
| `src/main/java/gamelibrary/GameLibraryCli.java` | `89a114a919f19038dc6551a94aa938ac1598ecbd26016f4c0f348e9db6570fb9` | `89a114a919f19038dc6551a94aa938ac1598ecbd26016f4c0f348e9db6570fb9` |
| `src/main/java/gamelibrary/GameLibraryService.java` | `a7e415e45458ccc4c4587d7fcc37f73fbddb976ef63e6c424965e7685036f2c1` | `a7e415e45458ccc4c4587d7fcc37f73fbddb976ef63e6c424965e7685036f2c1` |
| `src/main/java/gamelibrary/GameStore.java` | `afeac7ec704ebf7bd0a1c76e4a8c6ea74647dc068096d6f7b09dc9f1f5e6b0b5` | `afeac7ec704ebf7bd0a1c76e4a8c6ea74647dc068096d6f7b09dc9f1f5e6b0b5` |
| `src/main/java/gamelibrary/Genre.java` | `2a9d21793ee233c6d99eaeddc14aadedb635146aed36b15b81cdc18b6227f5e2` | `2a9d21793ee233c6d99eaeddc14aadedb635146aed36b15b81cdc18b6227f5e2` |
| `src/main/java/gamelibrary/JLineCliInput.java` | `a97a905d7e87ff51dbea5e6b1d770363baa02c355cfff38dbe39b124fea67a1f` | `a97a905d7e87ff51dbea5e6b1d770363baa02c355cfff38dbe39b124fea67a1f` |
| `src/main/java/gamelibrary/JsonGameStore.java` | `cd23cda55e1a7d47c06bc84183b69f9b8265b0a1250260ab3c44148aa92fb4ef` | `cd23cda55e1a7d47c06bc84183b69f9b8265b0a1250260ab3c44148aa92fb4ef` |
| `src/main/java/gamelibrary/Main.java` | `1cfccec2c955ff76e78ba7446177d995df63a65170d5d53e47569099afa9b459` | `1cfccec2c955ff76e78ba7446177d995df63a65170d5d53e47569099afa9b459` |
| `src/main/java/gamelibrary/MenuOption.java` | `792f37b9b66230a12fdd42676b17ae540f972702ab8cfcd1c1bb50cf62514ddc` | `792f37b9b66230a12fdd42676b17ae540f972702ab8cfcd1c1bb50cf62514ddc` |
| `src/main/java/gamelibrary/MenuSelection.java` | `8aaf83273a57db019a39818cc0f47d0f71d868400f066786d9457680705713f0` | `8aaf83273a57db019a39818cc0f47d0f71d868400f066786d9457680705713f0` |
| `src/main/java/gamelibrary/PlainCliInput.java` | `129cf93bcca5f7f5cf1fc015cd74b34259e47407f1e20677d9e54cd7fcd506b7` | `129cf93bcca5f7f5cf1fc015cd74b34259e47407f1e20677d9e54cd7fcd506b7` |
| `src/main/java/gamelibrary/Platform.java` | `d4eec8de226cdddf8c708b8521bcbe1e37123af2034a2b1b1e7f2cb9aca73c05` | `d4eec8de226cdddf8c708b8521bcbe1e37123af2034a2b1b1e7f2cb9aca73c05` |
| `src/test/java/gamelibrary/GameLibraryCliTest.java` | `9f32d8bc978114cf2aba7624ed1c201be05d2515c9a31799160339373f72ab0e` | `9f32d8bc978114cf2aba7624ed1c201be05d2515c9a31799160339373f72ab0e` |
| `src/test/java/gamelibrary/GameLibraryTest.java` | `7093185db158a4ab57e4f1850e0fe90100bdd178b07f314deceafa10b95a3764` | `7093185db158a4ab57e4f1850e0fe90100bdd178b07f314deceafa10b95a3764` |
| `src/test/java/gamelibrary/MenuSelectionTest.java` | `de7724534b8c4db78e1fe529d9f1e772e41a74e4cd8cb8402c39c6acb4bc8ab7` | `de7724534b8c4db78e1fe529d9f1e772e41a74e4cd8cb8402c39c6acb4bc8ab7` |
| `src/test/java/gamelibrary/PersistenceTest.java` | `79b81c0d691b7299bb57c73db7733061481b114ac7fded66d54bf7f62c1fea9e` | `79b81c0d691b7299bb57c73db7733061481b114ac7fded66d54bf7f62c1fea9e` |

