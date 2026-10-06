#!/usr/bin/env python3
"""実行JARの番号入力・PTY操作・端末復元を検証する。SSH実機の代用ではない。"""
import argparse
import json
import tempfile
import fcntl
import os
from pathlib import Path
import pty
import select
import signal
import struct
import subprocess
import termios
import time

MENU = "選択中（↑↓ / 番号、Enterで決定）: "
UP = b"\x1b[A"
DOWN = b"\x1b[B"
checks = 0


def check(condition, message):
    global checks
    if not condition:
        raise AssertionError(message)
    checks += 1


class TerminalSession:
    def __init__(self, command, environment=None, invalid_data=False):
        self.storage = tempfile.TemporaryDirectory(prefix="game-library-v2-pty-")
        self.data_path = Path(self.storage.name) / "games.json"
        if invalid_data:
            self.data_path.write_text("broken", encoding="utf-8")
        command = command + ["--data", str(self.data_path)]
        self.master, self.slave = pty.openpty()
        self.original = termios.tcgetattr(self.slave)
        fcntl.ioctl(self.slave, termios.TIOCSWINSZ, struct.pack("HHHH", 40, 120, 0, 0))
        self.data = bytearray()
        self.mark = 0
        self.status = None
        self.pid = os.fork()
        if self.pid == 0:
            try:
                os.close(self.master)
                os.setsid()
                fcntl.ioctl(self.slave, termios.TIOCSCTTY, 0)
                for fd in (0, 1, 2):
                    os.dup2(self.slave, fd)
                if self.slave > 2:
                    os.close(self.slave)
                env = dict(os.environ, TERM="xterm-256color", LC_ALL="en_US.UTF-8")
                env.update(environment or {})
                # アプリ終了後もシェルを残し、macOSでPTYが無効になる前に属性を比較します。
                shell = '"$@"; result=$?; printf "\\n__JAVA_EXIT__=%s\\n" "$result"; read -r done; exit "$result"'
                os.execvpe("/bin/sh", ["sh", "-c", shell, "verify-cli", *command], env)
            except BaseException as error:
                os.write(2, str(error).encode())
                os._exit(127)

    def read(self, timeout=0.1):
        if select.select([self.master], [], [], timeout)[0]:
            try:
                chunk = os.read(self.master, 65536)
            except OSError:
                chunk = b""
            self.data.extend(chunk)
            # JLineの位置問い合わせに応答する疑似端末。アプリの入力には含めない。
            if b"\x1b[6n" in chunk:
                os.write(self.master, b"\x1b[1;1R")

    def wait(self, text, timeout=15):
        needle = text.encode("utf-8")
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            self.read()
            if needle in self.data[self.mark:]:
                return
            ended, status = os.waitpid(self.pid, os.WNOHANG)
            if ended:
                self.status = status
                break
        raise AssertionError(f"PTYで待機した文字列が出ない: {text}\n{self.text()[-2500:]}")

    def send(self, value):
        # プロンプト本体と再描画が別の書き込みで届いても、次の操作と混同しません。
        while select.select([self.master], [], [], 0.05)[0]:
            self.read(0)
        self.mark = len(self.data)
        os.write(self.master, value.encode("utf-8") if isinstance(value, str) else value)

    def resize(self):
        fcntl.ioctl(self.slave, termios.TIOCSWINSZ, struct.pack("HHHH", 16, 45, 0, 0))
        os.kill(self.pid, signal.SIGWINCH)

    def finish(self, expected_code=0):
        self.wait(f"__JAVA_EXIT__={expected_code}")
        check(termios.tcgetattr(self.slave) == self.original, "アプリ終了後の端末属性を完全に復元")
        self.send(b"\n")  # 属性確認を終えたので、検証用シェルも終了させます。
        deadline = time.monotonic() + 15
        while self.status is None and time.monotonic() < deadline:
            self.read()
            ended, status = os.waitpid(self.pid, os.WNOHANG)
            if ended:
                self.status = status
        self.read(0)
        check(self.status is not None, "PTYプロセスが終了する")
        check(os.waitstatus_to_exitcode(self.status) == expected_code, "PTY終了コードが期待どおり")
        check("Exception in thread" not in self.text(), "PTYに未処理例外を出さない")

    def text(self):
        return self.data.decode("utf-8", errors="replace")

    def __enter__(self):
        return self

    def __exit__(self, *args):
        if self.status is None:
            ended, status = os.waitpid(self.pid, os.WNOHANG)
            if not ended:
                # 失敗時の後始末を、子プロセスの終了待ちで無期限に停止させません。
                os.killpg(self.pid, signal.SIGKILL)
                os.waitpid(self.pid, 0)
        os.close(self.master)
        os.close(self.slave)
        self.storage.cleanup()


def isolated_run(command, **kwargs):
    with tempfile.TemporaryDirectory(prefix="game-library-v2-pipe-") as directory:
        isolated_command = [command[0], "-Duser.home=" + directory, *command[1:]]
        return subprocess.run(isolated_command + ["--data", str(Path(directory) / "games.json")], **kwargs)


def pipe_tests(command):
    flow = "1\nゼルダの伝説\n3\n2\n1.5\n2\n3\n伝説\n0\n"
    for args, term in [([], "xterm-256color"), (["--plain"], "xterm-256color"), ([], "dumb")]:
        result = isolated_run(command + args, input=flow, text=True, encoding="utf-8",
                                capture_output=True, timeout=15, env=dict(os.environ, TERM=term))
        check(result.returncode == 0, "非対話環境で正常終了")
        check("番号入力モード" in result.stdout, "非対話環境で番号入力へ切り替え")
        check("タイトル: ゼルダの伝説 | プラットフォーム: Nintendo Switch | ジャンル: RPG" in result.stdout,
              "非対話環境で4項目を登録・表示")
        check("検索結果" in result.stdout and "終了します。" in result.stdout, "非対話環境で検索・終了")
        check("\x1b" not in result.stdout + result.stderr, "非対話環境には端末制御文字を出さない")
    result = isolated_run(command, input="12\nabc\n0\n", text=True, encoding="utf-8",
                            capture_output=True, timeout=15)
    check(result.stdout.count("0から4までの整数を入力してください。") == 2, "番号入力の全桁検証と再入力")
    result = isolated_run(command, input="1\nPartial\n1\n", text=True, encoding="utf-8",
                            capture_output=True, timeout=15)
    check("ゲームを登録しました。" not in result.stdout and "入力が終了したため" in result.stdout,
          "パイプの途中EOFで部分登録しない")
    result = isolated_run(command + ["--unknown"], input="", text=True, capture_output=True, timeout=15)
    check(result.returncode == 2 and "使用方法" in result.stderr, "未知の起動オプションを検出")


def arrow_flow(command):
    with TerminalSession(command) as s:
        s.wait(MENU)
        s.wait("[ゲームを登録]")
        check("[ゲームを登録]" in s.text(), "初回から選択項目名を表示")
        s.send(UP + UP + b"\r")  # 上端で停止して登録へ
        s.wait("タイトル: ")
        title = '12 ゼルダ"\\!'
        s.send(title + "x\x7f\r")  # 文字入力では数字・記号を保持、Backspaceでxを削除
        s.wait(MENU)
        s.send(DOWN + DOWN + b"\r")
        s.wait(MENU)
        s.send(DOWN + b"\r")
        s.wait("プレイ時間")
        s.send("1.5\r")
        s.wait(MENU)
        saved = json.loads(s.data_path.read_text(encoding="utf-8"))
        check(saved["games"][0]["title"] == title, "終了前に方向キー登録を自動保存")
        s.send(DOWN + b"\r")
        s.wait(MENU)
        check(f"タイトル: {title} | プラットフォーム: Nintendo Switch | ジャンル: RPG" in s.text(),
              "全メニューの矢印操作と文字入力の復帰\n" + s.text()[-1500:])
        s.send(DOWN + DOWN + b"\r")
        s.wait("検索するタイトル: ")
        s.send("ゼルダ\r")
        s.wait(MENU)
        check("検索結果\r\n[1] タイトル: " + title in s.text(), "方向キーで検索を実行")
        s.resize()
        s.send(DOWN * 8 + b"\r")  # 下端で停止して終了へ
        s.finish()
        check("終了します。" in s.text(), "サイズ変更後も選択・終了")


def numeric_flow(command):
    with TerminalSession(command) as s:
        s.wait(MENU)
        s.send("12\r")
        s.wait(MENU)
        check("0から4までの整数" in s.text(), "PTYでも12を2にしない")
        s.send("abc\r")
        s.wait(MENU)
        check(s.text().count("0から4までの整数") == 2, "PTYでも文字の不正入力を再入力")
        s.send("1\r")
        s.wait("タイトル: ")
        s.send("数字入力のゲーム\r")
        s.wait(MENU)
        s.send("6\r")
        s.wait(MENU)
        s.send("8\r")
        s.wait("プレイ時間")
        s.send("0\r")
        s.wait(MENU)
        s.send("2\r")
        s.wait(MENU)
        check("プラットフォーム: その他 | ジャンル: その他 | プレイ時間: 0.0" in s.text(),
              "PTYの番号入力でenumの最終項目を選択")
        s.send("0\r")
        s.finish()


def mixed_flow(command):
    with TerminalSession(command) as s:
        s.wait(MENU)
        s.send(b"3" + UP + b"\r")  # 番号3から↑で一覧2
        s.wait(MENU)
        check("ゲームはまだ登録されていません。" in s.text(), "番号入力から方向キーへ切り替え")
        s.send(b"\x1b")
        time.sleep(0.2)  # SSHの分割到着を模したエスケープシーケンス
        s.send(b"[B\r")
        s.wait(MENU)
        check(s.text().count("ゲームはまだ登録されていません。") == 2, "分割到着する矢印キー")
        s.send(b"\x1b[200~12\x1b[201~\r")
        s.wait(MENU)
        check("0から4までの整数" in s.text(), "貼り付けも番号全体として検証")
        s.send("01\x7f0\r")  # 01を0へ削除、続けて00を入力して終了
        s.finish()


def termination_tests(command):
    for key, partial in [(b"\x03", False), (b"\x04", False),
                         (b"\x03", True), (b"\x04", True)]:
        with TerminalSession(command) as s:
            s.wait(MENU)
            if partial:
                s.send("1\r")
                s.wait("タイトル: ")
                s.send("Partial\r")
                s.wait(MENU)
                s.send("1\r")
                s.wait(MENU)
                s.send("2\r")
                s.wait("プレイ時間")
            s.send(key)
            s.finish()
            check("入力が終了したため" in s.text(), "Ctrl+C/EOFを終了経路へ変換")
            check("ゲームを登録しました。" not in s.text(), "中断時に部分登録しない")
            check(not s.data_path.exists(), "Ctrl+C/EOFで途中登録のJSONを作らない")
    for env, args in [({"TERM": "dumb"}, []), ({}, ["--plain"]),
                      ({"JAVA_TOOL_OPTIONS": "-Dorg.jline.terminal.provider=missing"}, [])]:
        with TerminalSession(command + args, env) as s:
            s.wait("番号を入力: ")
            s.send("2\n0\n")
            s.finish()
            check("番号入力モード" in s.text() and "ゲームはまだ登録されていません。" in s.text(),
                  "TTYでもdumb/--plain/端末初期化失敗時は番号入力")
            check("\x1b" not in s.text(), "番号入力モードに端末制御文字を出さない")
    for key in (b"\x03", b"\x04"):
        with TerminalSession(command, invalid_data=True) as s:
            s.wait(MENU)
            for value, prompt in [("1\r", "タイトル: "), ("Unsaved\r", MENU),
                                  ("1\r", MENU), ("2\r", "プレイ時間"), ("1\r", MENU)]:
                s.send(value)
                s.wait(prompt)
            check("保存無効のためメモリ内のみ" in s.text(), "読み込み失敗後の登録はdirty状態")
            s.send("0\r")
            s.wait("0. メニューに戻る")
            s.send("\r")  # 確認メニューの既定選択は「戻る」。
            s.wait(MENU)
            s.send(key)
            s.finish(expected_code=1)
            check("未保存のデータがあります" in s.text() and "入力が終了したため" in s.text(),
                  "dirty状態でのCtrl+C/EOFは破棄確認を自動保存せず終了コード1")
            check(s.data_path.read_text(encoding="utf-8") == "broken",
                  "dirty EOF/割り込みでも元の不正データを維持")


def persistence_tests(command):
    with tempfile.TemporaryDirectory(prefix="game-library-v2-persistence-") as directory:
        root = Path(directory)
        # JVMのホームを一時領域に置き換え、既定パスも実ホームへ触れず確認します。
        default_command = [command[0], "-Duser.home=" + directory, *command[1:]]
        data = root / ".game-library-cli" / "games.json"
        result = subprocess.run(default_command, input="2\n0\n", text=True,
                                capture_output=True, timeout=15)
        check(result.returncode == 0 and str(data) in result.stdout, "既定保存先はホーム配下")
        check(not data.exists(), "初回の閲覧・終了だけでは保存しない")
        result = subprocess.run(default_command, input="1\nゼルダ\\\"!\n3\n2\n1.5\n", text=True,
                                capture_output=True, timeout=15)
        check(result.returncode == 0 and "JSONへ保存しました" in result.stdout, "完了登録はEOF終了でも保存済み")
        saved = json.loads(data.read_text(encoding="utf-8"))
        check(saved["schemaVersion"] == 1 and saved["games"][0]["playHours"] == 1.5,
              "実行JARのJSON形式と小数")
        before = data.read_bytes()
        result = subprocess.run(default_command, input="2\n3\nゼルダ\n0\n", text=True,
                                capture_output=True, timeout=15)
        check("タイトル: ゼルダ" in result.stdout and "検索結果" in result.stdout, "再起動で復元・検索")
        check(data.read_bytes() == before, "一覧・検索・終了で保存を書き換えない")
        saved_mtime = data.stat().st_mtime_ns
        result = subprocess.run(default_command, input="4\n0\n", text=True,
                                capture_output=True, timeout=15)
        check(result.returncode == 0 and "すべて保存済みです" in result.stdout,
              "clean状態のメニュー再試行は正常に終了")
        check(data.read_bytes() == before and data.stat().st_mtime_ns == saved_mtime,
              "clean状態の再試行はJSON書き込みを行わない")

        relative = root / "別フォルダ" / "games.json"
        args = command + ["--plain", "--data", "別フォルダ/games.json"]
        flow = "1\nEcho\n1\n2\n0\n1\nEcho\n6\n8\n0.25\n1\nPartial\n"
        result = subprocess.run(args, input=flow, text=True, capture_output=True, cwd=root, timeout=15)
        records = json.loads(relative.read_text())["games"]
        check(result.returncode == 0 and str(relative) in result.stdout, "--plainと相対--dataを併用")
        check(len(records) == 2 and [g["title"] for g in records] == ["Echo", "Echo"],
              "重複と順序を保持し途中入力は保存しない")
        result = subprocess.run(command + ["--data", str(relative), "--plain"], input="2\n0\n",
                                text=True, capture_output=True, timeout=15)
        check("[1] タイトル: Echo" in result.stdout and "[2] タイトル: Echo" in result.stdout,
              "引数順序を変えて再起動復元")

        for contents in [b"broken", b'{"schemaVersion":2,"games":[]}', b""]:
            broken = root / "broken.json"
            broken.write_bytes(contents)
            result = subprocess.run(command + ["--data", str(broken)],
                                    input="1\nMemory\n1\n2\n1\n2\n0\n0\n2\n", text=True,
                                    capture_output=True, timeout=15)
            check(result.returncode == 1 and "保存無効" in result.stdout
                  and "読み込みに失敗" in result.stderr, "読み込み失敗でもメモリ専用で継続")
            check("[1] タイトル: Memory" in result.stdout and "未保存のデータがあります" in result.stdout,
                  "保存無効でも登録・一覧・終了警告")
            check(broken.read_bytes() == contents, "破損・未来の形式・空ファイルを保護")

        unreadable = root / "unreadable.json"
        unreadable.write_bytes(before)
        unreadable.chmod(0)
        try:
            if os.geteuid() != 0:
                result = subprocess.run(command + ["--data", str(unreadable)], input="0\n", text=True,
                                        capture_output=True, timeout=15)
                check(result.returncode == 1 and "読み込みに失敗" in result.stderr
                      and "保存無効" in result.stdout,
                      "読み取り権限不足を通知して継続")
        finally:
            unreadable.chmod(0o600)
        check(unreadable.read_bytes() == before, "権限不足でも元データを変更しない")

        dirty_eof = root / "dirty-eof.json"
        dirty_eof.write_text("broken", encoding="utf-8")
        result = subprocess.run(command + ["--data", str(dirty_eof)],
                                input="1\nDiscard at EOF\n1\n2\n1\n", text=True,
                                capture_output=True, timeout=15)
        check(result.returncode == 1 and "入力が終了したため" in result.stdout
              and "未保存のデータがあります" in result.stdout,
              "dirty状態でEOFが破棄確認を自動保存せず終了コード1")
        check(dirty_eof.read_text(encoding="utf-8") == "broken", "dirty EOFで元ファイルを維持")
        dirty_discard = root / "dirty-discard.json"
        dirty_discard.write_text("broken", encoding="utf-8")
        result = subprocess.run(command + ["--data", str(dirty_discard)],
                                input="1\nDiscard explicitly\n1\n2\n1\n0\n2\n", text=True,
                                capture_output=True, timeout=15)
        check(result.returncode == 1 and "保存せず終了します" in result.stdout,
              "明示的な破棄終了を確認し、保存無効状態は終了コード1")
        check(dirty_discard.read_text(encoding="utf-8") == "broken", "明示的破棄でも元ファイルを維持")
        disabled_retry = root / "disabled-retry.json"
        disabled_retry.write_text("broken", encoding="utf-8")
        result = subprocess.run(command + ["--data", str(disabled_retry)],
                                input="1\nNo retry\n1\n2\n1\n4\n0\n2\n", text=True,
                                capture_output=True, timeout=15)
        check(result.returncode == 1 and "保存無効のため再試行できません" in result.stdout,
              "保存無効状態の4番は書き込まず終了コード1")
        check(disabled_retry.read_text(encoding="utf-8") == "broken",
              "保存無効状態の再試行で元ファイルを維持")

        for args in [["--data"], ["--data", "--plain"], ["--plain", "--plain"],
                     ["--data", str(data), "--data", str(data)]]:
            result = subprocess.run(command + args, input="", text=True, capture_output=True, timeout=15)
            check(result.returncode == 2 and "使用方法" in result.stderr, "不正な引数はファイル操作前に検出")


def lock_tests(command):
    with tempfile.TemporaryDirectory(prefix="game-library-v2-lock-") as directory:
        root = Path(directory)
        data = root / "store" / "games.json"
        data.parent.mkdir()
        data.write_text('{"schemaVersion":1,"games":[]}\n', encoding="utf-8")
        alias_data = root / "games-alias.json"
        alias_data.symlink_to(data)
        alias_parent = root / "store-alias"
        alias_parent.symlink_to(data.parent, target_is_directory=True)
        second = None
        child = subprocess.Popen(command + ["--plain", "--data", str(alias_data)],
                                 stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                 stderr=subprocess.STDOUT, text=True, encoding="utf-8", bufsize=0)
        try:
            output = wait_process_text(child, "番号を入力: ")
            check("番号を入力: " in output, "ファイルsymlink alias解決後にロックを保持してCLIを開始")
            contender = subprocess.run(command + ["--plain", "--data", str(alias_parent / data.name)], input="0\n",
                                       text=True, capture_output=True, timeout=15)
            check(contender.returncode == 1 and "ロック" in contender.stderr,
                  "ファイル・親ディレクトリsymlink別名間のロック競合を拒否")

            independent = root / "independent.json"
            second = subprocess.Popen(command + ["--plain", "--data", str(independent)],
                                      stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                      stderr=subprocess.STDOUT, text=True, encoding="utf-8", bufsize=0)
            second_output = wait_process_text(second, "番号を入力: ")
            check("番号を入力: " in second_output and child.poll() is None,
                  "別の保存先では最初のCLIと同時にロックを取得")
            second.kill()  # 異常終了でもOSがファイルロックを解放することを確認。
            second.communicate(timeout=5)
            reclaimed = subprocess.run(command + ["--plain", "--data", str(independent)], input="0\n",
                                        text=True, capture_output=True, timeout=15)
            check(reclaimed.returncode == 0, "異常終了後は別プロセスがロックを再取得")

            child_output, _ = child.communicate("0\n", timeout=15)
            check(child.returncode == 0, "CLI終了後にプロセスがロックを解放\n" + child_output[-1000:])
            check((data.parent / "games.json.lock").exists(), "ロックファイルが残っても再利用可能")
            released = subprocess.run(command + ["--plain", "--data", str(data)], input="0\n",
                                      text=True, capture_output=True, timeout=15)
            check(released.returncode == 0, "終了後の別プロセスがロックを再取得")
        finally:
            if second is not None and second.poll() is None:
                second.kill()
                second.communicate(timeout=5)
            if child.poll() is None:
                child.kill()
            if child.stdin and not child.stdin.closed:
                child.stdin.close()
            if child.stdout and not child.stdout.closed:
                child.stdout.close()
            if child.poll() is None:
                child.wait(timeout=5)

        blocker = root / "not-a-directory"
        blocker.write_text("preserve", encoding="utf-8")
        uncreatable = subprocess.run(command + ["--plain", "--data", str(blocker / "games.json")],
                                      input="", text=True, capture_output=True, timeout=15)
        check(uncreatable.returncode == 1, "親ディレクトリを作れない保存先は終了コード1")

        protected = root / "protected.json"
        protected.write_text('{"schemaVersion":1,"games":[]}\n', encoding="utf-8")
        lock_victim = root / "victim"
        lock_victim.write_text("preserve", encoding="utf-8")
        lock_link = protected.with_name(protected.name + ".lock")
        lock_link.symlink_to(lock_victim)
        blocked_lock = subprocess.run(command + ["--plain", "--data", str(protected)],
                                      input="", text=True, capture_output=True, timeout=15)
        check(blocked_lock.returncode == 1 and "ロック" in blocked_lock.stderr,
              "ロックパスがsymlinkならコード1で拒否")
        check(lock_victim.read_text(encoding="utf-8") == "preserve", "symlink先をロックファイルで上書きしない")


def wait_process_text(process, target, timeout=15):
    output = ""
    deadline = time.monotonic() + timeout
    while target not in output and time.monotonic() < deadline:
        ready, _, _ = select.select([process.stdout], [], [], 0.1)
        if ready:
            output += os.read(process.stdout.fileno(), 4096).decode("utf-8", errors="replace")
        if process.poll() is not None:
            break
    return output


def save_failure_pty(command):
    with TerminalSession(command) as s:
        s.wait(MENU)
        # 起動後にだけ保存を妨げ、未保存ゲームをその場で一覧表示します。
        s.data_path.mkdir()
        for value, prompt in [("1\r", "タイトル: "), ("First\r", MENU),
                              ("1\r", MENU), ("2\r", "プレイ時間"), ("1\r", MENU)]:
            s.send(value)
            s.wait(prompt)
        check("ただし未保存" in s.text() and "保存に失敗" in s.text(), "PTYで保存失敗を案内")
        s.send("2\r")
        s.wait(MENU)
        check("[1] タイトル: First" in s.text(), "保存失敗後もメモリの一覧を表示")
        s.data_path.rmdir()
        s.send("4\r")
        retry_mark = len(s.data)
        s.wait(MENU)
        saved = json.loads(s.data_path.read_text())["games"]
        check(len(saved) == 1 and saved[0]["title"] == "First",
              "PTYの保存再試行は未保存分だけを書き、新規登録しない")
        check("未保存のデータがあります" not in s.data[retry_mark:].decode("utf-8", errors="replace"),
              "保存成功後のメニューから未保存警告を消す")

        # 別のdirty状態で、終了確認の既定値と「保存して終了」を確認します。
        s.data_path.unlink()
        s.data_path.mkdir()
        for value, prompt in [("1\r", "タイトル: "), ("Second\r", MENU),
                              ("1\r", MENU), ("2\r", "プレイ時間"), ("2\r", MENU)]:
            s.send(value)
            s.wait(prompt)
        check("ただし未保存" in s.text(), "2件目の登録でも保存失敗を保持")
        s.data_path.rmdir()
        s.send("0\r")
        s.wait("0. メニューに戻る")
        s.send("\r")
        s.wait(MENU)
        check("未保存のデータがあります" in s.text(), "確認の既定値はメニューへ戻る")
        s.send("0\r")
        s.wait("1. 保存して終了")
        s.send("1\r")
        s.finish()
        finish_tail = s.data[retry_mark:].decode("utf-8", errors="replace")
        check("保存して終了" in finish_tail and "JSONへ保存しました" in finish_tail,
              "終了確認で再保存して正常終了")
        final_games = json.loads(s.data_path.read_text(encoding="utf-8"))["games"]
        check([game["title"] for game in final_games] == ["First", "Second"],
              "終了確認の保存で既存の未保存分をまとめて永続化")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", default="java", help="Java 21以上の実行ファイル")
    parser.add_argument("--jar", type=Path,
                        default=Path(__file__).resolve().parents[1] / "target/game-library-cli-2.1.0.jar")
    args = parser.parse_args()
    if not args.jar.is_file():
        parser.error("実行JARがありません。先に ./mvnw clean verify を実行してください。")
    command = [args.java, "-jar", str(args.jar.resolve())]
    pipe_tests(command)
    print("非対話環境の確認: passed", flush=True)
    print("PTYの方向キー操作を確認中", flush=True)
    arrow_flow(command)
    numeric_flow(command)
    mixed_flow(command)
    print("PTYの矢印・番号・切り替え操作: passed", flush=True)
    termination_tests(command)
    persistence_tests(command)
    save_failure_pty(command)
    lock_tests(command)
    print("JSON保存・再起動・失敗時の動作: passed", flush=True)
    print(f"CLI integration: {checks} checks passed（ローカル検証。SSH実機検証は含まない）", flush=True)


if __name__ == "__main__":
    main()
