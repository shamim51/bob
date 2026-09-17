# chatwoot-spring

Independent Spring Boot resource server that copies Chatwoot’s **Conversations** API contracts (first run only).

This is not a fork of the Rails app. It lives beside Chatwoot and implements the same paths, query params, JSON keys, integer `message_type`, unix timestamps, and `display_id` public conversation ids.

**Read first:** [docs/PLAN_AND_PROGRESS.md](docs/PLAN_AND_PROGRESS.md) — original plan, what shipped, and where to start next.

## Scope (this run)

- Keycloak JWT resource server (`Authorization: Bearer`)
- Agent lookup by JWT `email` (one account membership)
- Conversations list/show/meta, messages index/create, last_seen, toggle_status, assignments
- Supporting reads: profile, account, inboxes, agents, contacts, empty labels/teams/notifications

**Not in this run:** Facebook, Captain, ActionCable, Vue auth adapter.

## Run

Java 21, PostgreSQL.

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/chatwoot_spring
export KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/chatwoot
export CHATWOOT_SEED=true
./gradlew bootRun
```

Seed users (must exist in Keycloak with the same emails):

- `agent@example.com`
- `admin@example.com`

## Auth

Spring Boot does not issue tokens. The dashboard must send a Keycloak access token. `GET /api/v1/profile` returns Chatwoot’s user payload wrapped as `{ "payload": { "success": true, "data": { ... } } }`.

## Chatwoot contracts copied

| API | Behavior |
|---|---|
| `GET /api/v1/accounts/:id/conversations` | `{ data: { meta, payload } }`, `id` = `display_id` |
| `GET .../messages` | latest 20 asc; `before` 20 reversed; `after` 100 |
| `POST .../messages` | `echo_id` is not stored; returned on the JSON; `last_activity_at` = message `created_at` |

Channel send (`SendReplyJob`) is skipped; outgoing messages stay `status: sent`.


## Scope (this run)

- Keycloak JWT resource server (`Authorization: Bearer`)
- Agent lookup by JWT `email` (one account membership)
- Conversations list/show/meta, messages index/create, last_seen, toggle_status, assignments
- Supporting reads: profile, account, inboxes, agents, contacts, empty labels/teams/notifications

**Not in this run:** Facebook, Captain, ActionCable, Vue auth adapter.

## Run

Java 21, PostgreSQL.

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/chatwoot_spring
export KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/chatwoot
export CHATWOOT_SEED=true
./gradlew bootRun
```

Seed users (must exist in Keycloak with the same emails):

- `agent@example.com`
- `admin@example.com`

## Auth

Spring Boot does not issue tokens. The dashboard must send a Keycloak access token. `GET /api/v1/profile` returns Chatwoot’s user payload wrapped as `{ "payload": { "success": true, "data": { ... } } }`.

## Chatwoot contracts copied

| API | Behavior |
|---|---|
| `GET /api/v1/accounts/:id/conversations` | `{ data: { meta, payload } }`, `id` = `display_id` |
| `GET .../messages` | latest 20 asc; `before` 20 reversed; `after` 100 |
| `POST .../messages` | `echo_id` is not stored; returned on the JSON; `last_activity_at` = message `created_at` |

Channel send (`SendReplyJob`) is skipped; outgoing messages stay `status: sent`.
