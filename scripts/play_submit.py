#!/usr/bin/env python3
"""
Google Play 업로드·검토 제출 자동화(2026-10-27) — scripts/release-aos.sh 가 부른다. iOS scripts/asc_submit.py 와 짝.

  --next-code : Play 에 올라간(모든 트랙) 가장 큰 versionCode + 1 을 출력만 한다(빌드 전 번호 정하기용).
  기본       : 수정(edit) 열기 → AAB 올리기 → 트랙에 출시(출시 노트 ko-KR) → 커밋(검토 제출).
               관리형 게시가 꺼져 있으면 검토 통과 뒤 자동으로 게시된다.

파이썬 기본 기능 + openssl 만 쓴다(추가 설치 없음).
인증: 서비스 계정 JSON(Play Console '사용자 및 권한'에서 이 앱 '출시 관리' 권한) — 저장소 밖
  ~/.playconsole/muyeon.env 의 PLAY_SA_JSON=/절대/경로/xxx.json
"""
import argparse
import base64
import json
import os
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request

PACKAGE = "com.muyeon.app"
API = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}"
UPLOAD = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PACKAGE}"
ENV_FILE = os.path.expanduser("~/.playconsole/muyeon.env")
LANG = "ko-KR"


def log(msg):
    print(f"[play] {msg}", file=sys.stderr, flush=True)


def load_sa():
    path = os.environ.get("PLAY_SA_JSON", "")
    if not path and os.path.exists(ENV_FILE):
        for line in open(ENV_FILE, encoding="utf-8"):
            if line.strip().startswith("PLAY_SA_JSON="):
                path = line.split("=", 1)[1].strip().strip('"')
    path = os.path.expanduser(path)
    if not path or not os.path.exists(path):
        sys.exit(f"서비스 계정 JSON 이 없어요 — {ENV_FILE} 에 PLAY_SA_JSON=경로 를 적어 주세요.")
    return json.load(open(path, encoding="utf-8"))


def b64url(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode()


def access_token(sa):
    """서비스 계정 키로 RS256 JWT 를 만들어 구글 토큰으로 바꾼다."""
    now = int(time.time())
    header = {"alg": "RS256", "typ": "JWT"}
    claims = {"iss": sa["client_email"], "scope": "https://www.googleapis.com/auth/androidpublisher",
              "aud": sa["token_uri"], "iat": now, "exp": now + 3600}
    signing = f"{b64url(json.dumps(header).encode())}.{b64url(json.dumps(claims).encode())}".encode()
    with tempfile.NamedTemporaryFile("w", suffix=".pem", delete=False) as f:
        f.write(sa["private_key"])
        key = f.name
    try:
        os.chmod(key, 0o600)
        sig = subprocess.run(["openssl", "dgst", "-sha256", "-sign", key], input=signing,
                             capture_output=True, check=True).stdout
    finally:
        os.unlink(key)
    body = urllib.parse.urlencode({"grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
                                   "assertion": signing.decode() + "." + b64url(sig)}).encode()
    with urllib.request.urlopen(urllib.request.Request(sa["token_uri"], data=body), timeout=30) as r:
        return json.loads(r.read())["access_token"]


class Play:
    def __init__(self):
        self.token = access_token(load_sa())

    def call(self, method, url, body=None, raw=None, ctype="application/json"):
        data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
        req = urllib.request.Request(url, data=data, method=method, headers={
            "Authorization": f"Bearer {self.token}", "Content-Type": ctype})
        try:
            with urllib.request.urlopen(req, timeout=600) as r:
                out = r.read()
                return json.loads(out) if out else {}
        except urllib.error.HTTPError as e:
            sys.exit(f"API 오류 {method} {url.split(PACKAGE)[-1]} → {e.code}\n{e.read().decode(errors='replace')}")


def max_version_code(play):
    edit = play.call("POST", f"{API}/edits", {})["id"]
    try:
        codes = [int(c) for b in play.call("GET", f"{API}/edits/{edit}/bundles").get("bundles", [])
                 for c in [b["versionCode"]]]
        for t in play.call("GET", f"{API}/edits/{edit}/tracks").get("tracks", []):
            for r in t.get("releases", []):
                codes += [int(c) for c in r.get("versionCodes", [])]
    finally:
        play.call("DELETE", f"{API}/edits/{edit}")
    return max(codes) if codes else 0


def publish(play, aab, track, version_name, notes, status, no_review):
    edit = play.call("POST", f"{API}/edits", {})["id"]
    log(f"AAB 올리는 중 — {os.path.basename(aab)} ({os.path.getsize(aab) // 1024 // 1024}MB)")
    bundle = play.call("POST", f"{UPLOAD}/edits/{edit}/bundles?uploadType=media",
                       raw=open(aab, "rb").read(), ctype="application/octet-stream")
    code = bundle["versionCode"]
    log(f"AAB 올림 — versionCode {code}")
    play.call("PUT", f"{API}/edits/{edit}/tracks/{track}", {
        "track": track,
        "releases": [{"name": f"{version_name} ({code})", "versionCodes": [str(code)], "status": status,
                      "releaseNotes": [{"language": LANG, "text": notes}]}]})
    log(f"트랙 '{track}' 에 출시 준비(상태 {status})")
    q = "?changesNotSentForReview=true" if no_review else ""
    play.call("POST", f"{API}/edits/{edit}:commit{q}", {})
    log("커밋 완료 — " + ("검토에는 보내지 않았어요(Play Console 게시 개요에서 직접 보내기)." if no_review
                         else "검토 제출됨(관리형 게시가 꺼져 있으면 통과 뒤 자동 게시)."))


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--next-code", action="store_true")
    p.add_argument("--aab")
    p.add_argument("--track", default="alpha", help="alpha(비공개 테스트) / production / internal …")
    p.add_argument("--version-name")
    p.add_argument("--notes")
    p.add_argument("--draft", action="store_true", help="출시를 초안으로만 만든다(게시 안 함)")
    p.add_argument("--no-review", action="store_true", help="커밋은 하되 검토에는 보내지 않음")
    a = p.parse_args()

    play = Play()
    if a.next_code:
        print(max_version_code(play) + 1)
        return
    notes = open(a.notes, encoding="utf-8").read().strip()
    if not notes:
        sys.exit("출시 노트 파일이 비어 있어요.")
    if len(notes) > 500:
        sys.exit(f"Play 출시 노트는 500자까지예요(지금 {len(notes)}자).")
    publish(play, a.aab, a.track, a.version_name, notes, "draft" if a.draft else "completed", a.no_review)


if __name__ == "__main__":
    main()
