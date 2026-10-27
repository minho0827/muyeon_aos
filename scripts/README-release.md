# AOS 출시 한 번에 (release-aos.sh)

이 맥에서 명령 하나로 버전 올리기 → AAB(prodRelease) → Google Play 업로드 → 트랙 출시(출시 노트) → 검토 제출까지 한다. iOS 는 `~/Muyeon/scripts/release-ios.sh`.

## 처음 한 번 — Google Play 서비스 계정

1. Google Cloud 콘솔에서 서비스 계정을 만들고 JSON 키를 받는다(Google Play Android Developer API 사용 설정).
2. Play Console(muyeon.app@gmail.com) → 사용자 및 권한 → 새 사용자 초대 → 서비스 계정 이메일, 앱 '무용연'에 '출시 관리(프로덕션·테스트 트랙에 출시)' 권한.
3. JSON 을 `~/.playconsole/` 에 두고 `~/.playconsole/muyeon.env` 를 만든다.

```
PLAY_SA_JSON=/Users/<나>/.playconsole/muyeon-play.json
```

JSON 과 이 파일은 저장소에 넣지 않는다. 업로드 서명 키는 지금처럼 `~/.gradle/gradle.properties` 의 `MUYEON_RELEASE_*` 를 쓴다.

## 출시할 때

1. `scripts/release-notes.ko.txt` 에 출시 노트(500자 이내)를 적고 커밋한다.
2. main 에서 실행한다.

```
./scripts/release-aos.sh 1.1.1                     # 비공개 테스트(alpha) 트랙, 검토 제출
./scripts/release-aos.sh 1.1.1 --track production  # 프로덕션(신청 승인 뒤)
./scripts/release-aos.sh 1.1.1 --no-review         # 출시 준비까지만, 검토는 Play Console 에서
./scripts/release-aos.sh 1.1.1 --draft             # 출시를 초안으로만
```

- versionCode 는 Play 에 올라간 가장 큰 번호 + 1 로 자동(로컬이 더 크면 로컬 + 1). 버전 올린 커밋은 자동으로 푸시한다.
- 관리형 게시가 꺼져 있으면 검토 통과 뒤 자동으로 게시된다.
- AAB 는 `app/build/outputs/bundle/prodRelease/app-prod-release.aab`(devRelease 아님).

## 멈췄을 때

- API 오류 401/403 → 서비스 계정이 Play Console 에 초대됐는지, 앱 권한이 있는지 확인.
- "APK specifies a version code that has already been used" → 다시 실행하면 번호가 올라간다.
- "출시 노트는 500자까지" → 파일을 줄인다.
