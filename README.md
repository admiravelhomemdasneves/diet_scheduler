# DietScheduler

A household meal-planning app: shared pantry inventory, recipes, a meal-plan calendar, an
auto-generated shopping list, and nutrition tracking — built for a household to coordinate on
together in real time.

## Architecture

- **`backend/`** — Spring Boot 3 (Java 21) REST API + WebSocket, backed by PostgreSQL. Household
  data (pantry, meal plans, shopping lists, preferences) is scoped per-household; a household's
  connected clients get live updates over a WebSocket channel.
- **`mobile/`** — Flutter client (Android; iOS is currently unconfigured/untested). Google
  Sign-In for auth, `provider` for state management.
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
Flyway migrations land.

## Mobile app setup

```bash
cd mobile
flutter pub get
flutter run
```

By default the app talks to `http://10.0.2.2:8080` on Android (the special alias an Android
emulator uses to reach its host machine) or `http://localhost:8080` elsewhere — i.e. it expects
the backend from the step above to be running on the same machine as the emulator.

**Testing on a physical device on the same WiFi network as your backend:**

1. Find your dev machine's LAN IP (`ipconfig` on Windows, look for the WiFi adapter's IPv4
   address — e.g. `192.168.1.37`).
2. Update `mobile/lib/config.dart`'s base URL to point at that IP instead of `10.0.2.2`/`localhost`.
3. **Open an inbound firewall rule for TCP 8080** on the dev machine — this is the most common
   reason "the phone can't connect" even when everything else is configured correctly:

   ```powershell
   New-NetFirewallRule -DisplayName "DietScheduler backend" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
   ```

   (`docker-compose.yml` already binds the backend to `0.0.0.0:8080`, i.e. all network
   interfaces, not just loopback — the firewall is the only remaining blocker.)

A build-time flag for switching this without editing source (`--dart-define`) is planned but not
yet implemented; check back or see the project's production-readiness notes.

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

## Repository layout

```
backend/    Spring Boot API (Maven)
mobile/     Flutter client
docker-compose.yml
.env.example
```

## License

MIT — see [LICENSE](LICENSE).
