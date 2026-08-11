# DietScheduler

[![CI](https://github.com/admiravelhomemdasneves/diet_scheduler/actions/workflows/ci.yml/badge.svg)](https://github.com/admiravelhomemdasneves/diet_scheduler/actions/workflows/ci.yml)

A household meal-planning app: shared pantry inventory, recipes, a meal-plan calendar, an
auto-generated shopping list, and nutrition tracking — built for a household to coordinate on
together in real time.

- **Shared pantry** — add items by hand or by barcode scan; everyone in the household sees
  changes live over a WebSocket, no refresh needed.
- **Recipes** — your own recipes (with photos and estimated nutrition), or search/import from
  [TheMealDB](https://www.themealdb.com/).
- **Meal plan** — a calendar you fill in by hand, or auto-generate from allergies/taste
  preferences and what's actually in the pantry.
- **Shopping list** — generated from the meal plan's missing ingredients; marking an item bought
  adds it straight back into the pantry.
- **Nutrition** — per-recipe and per-day estimates, with an optional nutrition-targeted scheduling
  mode.

## Contents

- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Backend setup](#backend-setup)
- [Mobile app setup](#mobile-app-setup)
- [Google Sign-In config](#google-sign-in-config)
- [Running tests](#running-tests)
- [Repository layout](#repository-layout)
- [Production readiness](#production-readiness) — what's still missing before this could go on
  the Play Store for real users
- [License](#license)

## Architecture

- **`backend/`** — Spring Boot 3 (Java 21) REST API + WebSocket, backed by PostgreSQL. Household
  data (pantry, meal plans, shopping lists, preferences) is scoped per-household; a household's
  connected clients get live updates over a WebSocket channel.
- **`mobile/`** — Flutter client (Android; iOS is currently unconfigured/untested — see
  [Production readiness](#production-readiness)). Google Sign-In for auth, `provider` for state
  management.
- **`docker-compose.yml`** — runs Postgres + the backend together for local development.

## Prerequisites

- Docker Desktop
- Flutter SDK (matching the version in `mobile/pubspec.yaml`'s `environment: sdk:`)
- An Android emulator or device, or Xcode for iOS
- A Google Cloud OAuth **Web application** client ID (see [Google Sign-In config](#google-sign-in-config) below) — the project ships with a working default, so you only need your own if you want a separate OAuth consent screen

## Backend setup

1. Copy the environment template and fill it in:

   ```bash
   cp .env.example .env
   ```

   At minimum, set `POSTGRES_PASSWORD` / `DIETSCHEDULER_DB_PASSWORD` (any value; it just needs
   to match between the two) and `DIETSCHEDULER_JWT_SECRET` (a random string, at least 32
   characters — `.env.example` has commands to generate one). **The backend refuses to start if
   either is missing or if the JWT secret is left as an example/placeholder value** — this is
   intentional, not a bug, so a misconfigured deployment fails loudly instead of quietly signing
   tokens with a weak key.

2. Start the stack:

   ```bash
   docker compose up -d --build
   ```

   This builds and starts `dietscheduler-backend` (port 8080) and `dietscheduler-postgres`
   (no host port exposed — the backend reaches it over the Docker-internal network; see the
   comment in `docker-compose.yml` if you need direct DB access).

3. Confirm it's up:

   ```bash
   docker logs dietscheduler-backend --tail 20
   ```

   Look for `Started BackendApplication`. If it exits instead with a message starting
   `DietScheduler cannot start: required configuration is missing`, a required `.env` value
   is empty — the message lists exactly which one(s).

### Local dev vs. production schema management

`SPRING_PROFILES_ACTIVE=dev` (the `.env.example` default) lets Hibernate auto-update the schema
as entities change, which is convenient for local development. The **default** profile (used if
you don't set this) is strict — it validates the schema instead of altering it, and currently has
no migration tool wired up to actually create that schema. Keep the `dev` profile active until
Flyway migrations land (see [Production readiness](#production-readiness)).

## Mobile app setup

```bash
cd mobile
flutter pub get
flutter run
```

By default (`flutter run` with no flags) the app talks to `http://10.0.2.2:8080` — the special
alias an Android emulator uses to reach its host machine — i.e. it expects the backend from the
step above to be running on the same machine as the emulator. This comes from a single build-time
flag, `DS_API_ORIGIN` (see `mobile/lib/config.dart`'s `ApiConfig`), set via
`--dart-define-from-file`; the JSON files under `mobile/dart_define/` hold the presets.

**Testing on a physical device on the same WiFi network as your backend:**

1. Find your dev machine's LAN IP (`ipconfig` on Windows, look for the WiFi adapter's IPv4
   address — e.g. `192.168.1.37` — not a VPN or Hyper-V/WSL virtual adapter).
2. Copy `mobile/dart_define/lan.example.json` to `mobile/dart_define/lan.json` (gitignored) and
   fill in that IP.
3. Also update the literal IP in
   `mobile/android/app/src/debug/res/xml/network_security_config.xml` — Android's cleartext
   allow-list only accepts literal hosts, not a CIDR range, so this has to match `lan.json`.
4. **Open an inbound firewall rule for TCP 8080** on the dev machine — this is the most common
   reason "the phone can't connect" even when everything else is configured correctly:

   ```powershell
   New-NetFirewallRule -DisplayName "DietScheduler backend" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
   ```

   (`docker-compose.yml` already binds the backend to `0.0.0.0:8080`, i.e. all network
   interfaces, not just loopback — the firewall is the only remaining blocker.)
5. Run against the phone with the LAN config:

   ```bash
   flutter run --dart-define-from-file=dart_define/lan.json
   ```

   (`mobile/.vscode/launch.json` has an equivalent "DietScheduler (Phone over LAN)" configuration
   if you're using VS Code. To build an installable APK instead of `flutter run`, use
   `flutter build apk --debug --dart-define-from-file=dart_define/lan.json` — a plain
   `flutter build apk` with no flag silently falls back to the emulator-only `10.0.2.2` address
   and will fail to connect from a real device.)

A release build ships with cleartext traffic disabled entirely (see
`mobile/android/app/src/main/res/xml/network_security_config.xml`) — the debug-only override
above is what makes local HTTP testing possible without weakening the release build.

**If the phone can't reach the backend at all**, check in this order: (1) is
`docker ps` actually showing both containers `Up`? Docker Desktop doesn't always restart them
automatically after being closed; (2) does `curl http://<your-LAN-IP>:8080/...` work *from the
dev machine itself* using the LAN IP, not just `localhost`? (3) is the firewall rule above still
present — `Get-NetFirewallRule` after a Windows Firewall reset or profile change; (4) is the
phone on the *same* WiFi network/subnet as the dev machine, not a guest network or mobile data.

## Google Sign-In config

The app is configured against an existing Google OAuth **Web application** client ID (see
`mobile/lib/config.dart` and `docker-compose.yml`/`.env.example`). This value is a public
identifier, not a secret — it's safe to see in the repo. If you fork this project and want your
own OAuth consent screen, register a new client in
[Google Cloud Console](https://console.cloud.google.com/apis/credentials) and update
`DIETSCHEDULER_GOOGLE_CLIENT_IDS` in your `.env` and `googleServerClientId` in `config.dart`.

**Release builds need their signing certificate's SHA-1 registered separately** with Google, in
addition to the client ID above — Google Sign-In authorizes the Android app by package name +
signing certificate, not just the client ID. A release build signed with a different key than
whatever's currently registered will fail to sign in until you add its SHA-1.

## Running tests

```bash
cd backend && mvn -B verify   # self-contained -- runs against an in-memory H2 DB, no Docker needed
cd mobile && flutter test
```

Both also run in CI (`.github/workflows/ci.yml`) on every push and pull request, alongside
`flutter analyze` and a guard against raw `Colors.*` literals creeping in outside `lib/theme/`.

## Repository layout

```
backend/            Spring Boot API (Maven)
mobile/              Flutter client
tool/branding/       Reproducible launcher-icon/splash asset generator (PowerShell + System.Drawing)
.github/workflows/   CI (backend tests, mobile analyze/test, color-lint, gitleaks)
docker-compose.yml
.env.example
```

## Production readiness

Everything above gets you a working app on an emulator or a phone on your own WiFi network. None
of it is enough to put this on the Play Store for other people to install — that's a distinct
checklist, tracked here so it doesn't get lost. As of now, **nothing below is done.**

### Deployment

- [ ] **Host the backend somewhere public**, with a real domain and a TLS certificate. Right now
      the backend only ever runs via `docker-compose.yml` on a developer's own machine — there is
      no cloud deployment of any kind.
- [ ] **Point the mobile release build at that real backend.** `mobile/dart_define/prod.json`
      already exists for this, but still holds a placeholder domain
      (`https://api.dietscheduler.example.com`) — replace it once the backend is actually hosted
      somewhere, and build with `--dart-define-from-file=dart_define/prod.json`. A release build
      with no `--dart-define-from-file` flag at all falls back to `10.0.2.2` like the emulator
      default, and since release builds disable cleartext traffic entirely, that fails outright
      rather than with a helpful error.
- [ ] **Flyway migrations** (deferred as Phase 11 in the original planning doc). The default
      Spring profile is `ddl-auto: validate` — it expects the schema to already exist and won't
      create it. There's currently no migration tool wired up to create that schema at all, so
      the default (non-`dev`) profile cannot start against a fresh database yet.
- [ ] **Database backups** for the production Postgres instance, once one exists.

### Android release & Play Store listing

- [ ] **Generate a real release signing key.** `mobile/android/app/build.gradle.kts` currently has
      `signingConfig = signingConfigs.getByName("debug")` with a literal `// TODO` next to it —
      release builds are signed with the debug key so that local `flutter run --release` works,
      which is fine for testing but not for a Play Store submission.
- [ ] **Register the new release key's SHA-1 with the Google OAuth client.** Google Sign-In
      authorizes by package name *and* signing-certificate SHA-1, not just the client ID — without
      this, sign-in fails at Google's own consent screen, before the app ever talks to the
      backend.
- [ ] **Move the OAuth consent screen out of Testing mode** in Google Cloud Console. While it's
      restricted to a list of test users, nobody else can sign in even if everything else above is
      correct.
- [ ] **Publish a privacy policy URL.** Required both for the Play Store listing itself and for
      Google's OAuth verification once the app requests sign-in from users outside the test list.
- [ ] **In-app account deletion.** Households can already be left/deleted, but the underlying
      `User` row has no deletion path — Play Store policy requires a way for a user to delete
      their account and data from inside the app, not just stop using it.
- [ ] **Play Store listing assets**: screenshots, a feature graphic, the content rating
      questionnaire, and the Data Safety form (what data the app collects and why).

### Security & reliability

- [ ] **Token refresh/revocation.** Sessions are a JWT valid for 30 days
      (`DIETSCHEDULER_JWT_EXPIRATION_MINUTES`, default `43200`) with no refresh flow and no way to
      revoke one early. A user whose token expires mid-session just starts getting 401s with no
      recovery path in the UI.
- [ ] **Crash reporting** (e.g. Sentry or Firebase Crashlytics). There is currently zero visibility
      into crashes happening on real users' devices.
- [ ] **Offline / connectivity handling.** There's no explicit "you're offline" state — a lost
      connection just surfaces as a generic request failure through the error banner added in an
      earlier pass, which is honest but not especially friendly.

### iOS (only relevant if an App Store release is ever planned)

- [ ] iOS is entirely unconfigured: no `NSCameraUsageDescription` / `NSPhotoLibraryUsageDescription`
      in `Info.plist` despite the app using barcode scanning and image picking, and the iOS build
      has never been tested. Submitting as-is would be a guaranteed rejection.

## License

MIT — see [LICENSE](LICENSE).
