# Bob (chatwoot-spring)

Independent Spring Boot resource server that copies Chatwoot REST contracts. Not a Rails fork.

The agent dashboard is a separate repo: [shamim51/bob-web](https://github.com/shamim51/bob-web) (local clone: `../bob-web`).

**Read first:** [docs/PLAN_AND_PROGRESS.md](docs/PLAN_AND_PROGRESS.md) — what shipped, Facebook setup, and remaining gaps. Agent coding rules: [agents.md](agents.md).

## Scope

- Keycloak JWT resource server (`Authorization: Bearer`)
- Agent lookup by JWT `email`
- Conversations list/show/meta, messages index/create, last_seen, toggle_status, assignments
- Facebook Messenger: add page, add inbox members, webhook `/bot`, Graph send (text)
- Supporting reads: profile, account, inboxes, agents, contacts, empty labels/teams/notifications

**Not done:** WhatsApp, ActionCable / live `message.created`, attachments, `assignable_agents`, Captain.

## Run

Java 25, Spring Boot 4.1.1, PostgreSQL. Gradle 9.1 can run on JDK 25 (`./gradlew` downloads a matching toolchain if needed).

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/chatwoot_spring
export KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/chatwoot
export CHATWOOT_SEED=true
export FB_APP_ID=          # must match bob-web public/window-config.js fbAppId
export FB_APP_SECRET=
export FB_VERIFY_TOKEN=    # Meta webhook verify token
./gradlew bootRun
```

Seed users (must exist in Keycloak with the same emails): `agent@example.com`, `admin@example.com`.

Facebook inbound needs a **public HTTPS** URL for `GET/POST /bot` (tunnel locally). Set that callback in the Meta app; Bob only subscribes the Page after inbox create.

## Auth

Spring Boot does not issue tokens. The dashboard logs in at Keycloak and sends `Authorization: Bearer`. `GET /api/v1/profile` returns Chatwoot’s user payload wrapped as `{ "payload": { "success": true, "data": { ... } } }`.

To match silkroad-fe / [bob-web](https://github.com/shamim51/bob-web) locally:

```bash
export KEYCLOAK_ISSUER_URI=https://dev-kc.getsport360.com/realms/test_realm
```

## Dashboard (bob-web)

Clone [shamim51/bob-web](https://github.com/shamim51/bob-web) next to this repo (`../bob-web`). Then:

```bash
cd ../bob-web
cp .env.example .env.local   # VITE_API_HOST=http://localhost:8080
pnpm install
pnpm dev
```

Open http://localhost:5173/app/login. Production hosting is Vercel; see bob-web README.

## Chatwoot contracts copied

| API | Behavior |
|---|---|
| `GET /api/v1/accounts/:id/conversations` | `{ data: { meta, payload } }`, `id` = `display_id` |
| `GET .../messages` | latest 20 asc; `before` 20 reversed; `after` 100 |
| `POST .../messages` | `echo_id` on JSON only; Facebook channels send via Graph |
| `POST .../callbacks/facebook_pages.json` | page list + long-lived user token |
| `POST .../callbacks/register_facebook_page` | create Facebook inbox |
| `PATCH .../inbox_members` | Add Agents step (`inbox_id`, `user_ids`) |
| `GET/POST /bot` | Meta webhook verify + HMAC inbound (no JWT) |

Non-admin agents only see conversations for inboxes they were added to. Add yourself on the Add Agents screen after connecting a Page.

## Deploy (`dev`)

Merges to `dev` run GitHub Actions (not pull requests): test, fat jar, Docker Hub image, SSH restart on EC2. No Compose. Image: `$DOCKERHUB_USERNAME/bob:dev` and `:git-sha`. Runtime is Eclipse Temurin 25 with compact object headers, generational Shenandoah, and `-Xms256m -Xmx512m`. AOT cache and JFR are not enabled yet.

### GitHub secrets

| Secret | Use |
|---|---|
| `DOCKERHUB_USERNAME` | Docker Hub user / image namespace |
| `DOCKERHUB_TOKEN` | Hub access token |
| `EC2_HOST` | Instance IP or DNS |
| `EC2_USER` | SSH user |
| `EC2_SSH_KEY` | PEM private key |
| `EC2_PORT` | Optional; defaults to `22` |

Create a Docker Hub repository named `bob`.

### One-time EC2 setup

Install Docker and add the SSH user to the `docker` group. Open SSH from GitHub Actions and publish **8080** (or terminate TLS in nginx in front of it). Create `/opt/bob/.env` with mode `600`:

```
DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/sailor
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
KEYCLOAK_ISSUER_URI=https://dev-kc.getsport360.com/realms/test_realm
CHATWOOT_SEED=false
FB_APP_ID=...
FB_APP_SECRET=...
FB_VERIFY_TOKEN=...
```

Postgres must already be reachable from the container. `host.docker.internal` works because the workflow adds `--add-host=host.docker.internal:host-gateway`. Flyway runs when the process starts.
