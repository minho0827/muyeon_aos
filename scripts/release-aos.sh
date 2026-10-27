#!/bin/bash
#
# 무용연 AOS 출시 한 번에(2026-10-27) — 버전 올리기 → 커밋·푸시 → AAB(prodRelease) → Play 업로드 → 트랙 출시 → 검토 제출.
#  iOS scripts/release-ios.sh 와 짝.
#
#   ./scripts/release-aos.sh 1.1.1                     # 비공개 테스트(alpha) 트랙, 검토 제출
#   ./scripts/release-aos.sh 1.1.1 --track production  # 프로덕션(신청 승인 뒤)
#   ./scripts/release-aos.sh 1.1.1 --no-review         # 올리고 출시 준비까지만(검토는 Play Console 에서 직접)
#   ./scripts/release-aos.sh 1.1.1 --draft             # 출시를 초안으로만
#
# 출시 노트: scripts/release-notes.ko.txt (500자 이내, 출시 전에 고쳐서 커밋)
# 서비스 계정(저장소 밖): ~/.playconsole/muyeon.env — PLAY_SA_JSON=/절대/경로/xxx.json
# 서명 키: ~/.gradle/gradle.properties 의 MUYEON_RELEASE_* (또는 local.properties 의 MY_RELEASE_*) — 지금처럼
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
GRADLE="app/build.gradle.kts"
NOTES="scripts/release-notes.ko.txt"
ENV_FILE="$HOME/.playconsole/muyeon.env"
AAB="app/build/outputs/bundle/prodRelease/app-prod-release.aab"

VERSION=""; SUBMIT_ARGS=()
while [ $# -gt 0 ]; do
  case "$1" in
    --track) SUBMIT_ARGS+=(--track "$2"); shift ;;
    --no-review|--draft) SUBMIT_ARGS+=("$1") ;;
    -h|--help) sed -n 3,14p "$0"; exit 0 ;;
    *) VERSION="$1" ;;
  esac
  shift
done

say() { printf '\n▶ %s\n' "$1"; }
die() { printf '\n✖ %s\n' "$1" >&2; exit 1; }

# ── 0. 미리 확인 ───────────────────────────────────────────
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "버전을 1.1.1 처럼 적어 주세요."
[ -f "$ENV_FILE" ] || die "서비스 계정 설정 파일이 없어요: $ENV_FILE (scripts/README-release.md 참고)"
[ -s "$NOTES" ] || die "출시 노트 파일이 비어 있어요: $NOTES"
grep -qs "MUYEON_RELEASE_STORE_FILE" "$HOME/.gradle/gradle.properties" || grep -qs "MY_RELEASE_STORE_FILE" local.properties \
  || die "업로드 서명 키 설정(MUYEON_RELEASE_STORE_FILE)이 없어요 — ~/.gradle/gradle.properties 확인."
[ "$(git branch --show-current)" = "main" ] || die "main 브랜치에서 실행해 주세요."
[ -z "$(git status --porcelain)" ] || die "커밋 안 된 변경이 있어요. 먼저 커밋해 주세요."
git fetch -q origin main
[ "$(git rev-list --count HEAD..origin/main)" = 0 ] || die "origin/main 에 더 새 커밋이 있어요. 먼저 pull 해 주세요."

# ── 1. 버전 코드 = Play 에 올라간 가장 큰 번호 + 1 (로컬 번호가 더 크면 로컬 + 1) ──
LOCAL_CODE="$(grep -m1 'versionCode = ' "$GRADLE" | sed 's/[^0-9]//g')"
PLAY_NEXT="$(python3 scripts/play_submit.py --next-code)"
CODE=$(( PLAY_NEXT > LOCAL_CODE + 1 ? PLAY_NEXT : LOCAL_CODE + 1 ))
say "버전 $(grep -m1 'versionName = ' "$GRADLE" | sed 's/.*"\(.*\)".*/\1/') ($LOCAL_CODE) → $VERSION ($CODE)"
sed -i '' "s/versionCode = [0-9]*/versionCode = $CODE/; s/versionName = \"[^\"]*\"/versionName = \"$VERSION\"/" "$GRADLE"
git add "$GRADLE"
git commit -q -m "chore(release): AOS $VERSION ($CODE)"
git push -q origin main

# ── 2. AAB 만들기(prodRelease — devRelease 아님) ─────────────
say "AAB 만들기"
./gradlew -q :app:bundleProdRelease || die "AAB 만들기 실패 — 위 오류를 확인해 주세요."
[ -f "$AAB" ] || die "AAB 가 없어요: $AAB"

# ── 3. Play 업로드 → 트랙 출시 → 검토 제출 ───────────────────
say "Google Play 업로드·제출"
python3 scripts/play_submit.py --aab "$AAB" --version-name "$VERSION" --notes "$NOTES" "${SUBMIT_ARGS[@]+"${SUBMIT_ARGS[@]}"}"
say "끝 — AOS $VERSION ($CODE)"
