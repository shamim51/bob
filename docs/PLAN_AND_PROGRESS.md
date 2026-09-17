# Plan, what we shipped, and where to start

This repo is an **independent Spring Boot API** that sits beside Chatwoot (`/home/newroz/open-source/chatwoot`). It is not a Rails rewrite and does not live inside the Chatwoot git tree.

**Rule:** stay side-by-side with Chatwoot. Same URL paths, query params, JSON keys, integer message enums, unix timestamps, and `display_id` as the public conversation id. Do not invent a new chat model.

---

## 1. What the plan was

The original idea was “migrate Chatwoot to Spring Boot with agentic coding.” Chatwoot is a large Rails + Vue product (~87k Ruby LOC, 98 tables, 12 channels, Enterprise overlay). A full port is on the order of **hundreds to thousands of hours**.

The plan was then cut down in this order:

| Decision | Meaning |
|---|---|
| Keep the Vue dashboard | Do not rewrite the agent UI. Match the existing REST JSON so Conversations can talk to Spring. |
| Keycloak, not Devise | The SPA logs in at Keycloak and sends `Authorization: Bearer`. Spring is **only a resource server** (validate JWT, no login). One Keycloak user = one Chatwoot agent, matched by **email**. |
| Chatbox is the core | Inbound/outbound order like Chatwoot: `echo_id`, `source_id`, pagination by message `id`, `last_activity_at`, private notes never leave the thread. |
| Captain later via Spring AI | Reply-box tasks, copilot, inbox auto-reply — **not this run**. |
| First run = Conversations only | Independent project. No Facebook, no My Inbox product work, no ActionCable, no Vue Keycloak adapter yet. |

First-run product target:

- Enough API for Vue **Conversations**: list threads, open a chat, paginate messages, send a reply.
- Persist messages only (no Graph / `SendReplyJob`).
- Copy Chatwoot finders/builders, not a new design.

Hour context (agentic, one engineer), for orientation only:

- Full Chatwoot: ~900–1600h OSS, more with Enterprise.
- This Conversations slice: the first vertical of a ~300–440h chatbox + Facebook + Spring AI path. This run is the **conversation REST core** only.

---

## 2. What we did (first run)

Location: `/home/newroz/open-source/chatwoot-spring`

Stack: Java 21, Spring Boot 3.4, Gradle, JPA, Flyway, PostgreSQL (H2 in tests), OAuth2 resource server.

### Auth

- JWT required on all APIs except nothing public yet (Facebook webhook comes later).
- `CurrentUserService` reads JWT `email` (fallback `preferred_username`), loads `users` + `account_users`. Unknown email → 401.
- `GET /api/v1/profile` → `{ payload: { success, data } }` in Chatwoot user shape (`accounts[]`, `ui_settings`, `pubsub_token`).

### Data (Chatwoot-shaped tables)

`accounts`, `users`, `account_users`, `inboxes`, `inbox_members`, `contacts`, `contact_inboxes`, `conversations`, `messages`, plus a `conversation_display_id_counters` table (Chatwoot uses per-account sequences).

Inbox `channel_type` is a string (`Channel::Api` in seed). Message `message_type` / `status` / `content_type` are integers in the DB; JSON uses Chatwoot’s mix (integer `message_type`, string `status` like `"sent"`).

### Conversation APIs (ported behavior)

| Method | Path | Chatwoot behavior we copied |
|---|---|---|
| GET | `/api/v1/accounts/{id}/conversations` | `ConversationFinder`: default status `open`, `assignee_type`, inbox membership, sort `last_activity_at_desc`, page 25. Body `{ data: { meta, payload } }`. Conversation `id` = **display_id**. |
| GET | `.../conversations/meta` | Same counts |
| GET | `.../conversations/{displayId}` | Conversation partial |
| GET | `.../messages` | `MessageFinder`: latest 20 asc; `before` 20 then reverse; `after` 100 |
| POST | `.../messages` | `MessageBuilder`; `echo_id` not persisted; returned on JSON; `last_activity_at` = message `created_at` |
| POST | `.../update_last_seen` | `agent_last_seen_at` |
| POST | `.../toggle_status` | open / resolved / pending / snoozed |
| POST | `.../assignments` | `assignee_id` |

Supporting reads so the conversation page does not 404: account, inboxes, agents, contact show, empty labels/teams/custom filters/attributes, notifications unread `0`.

### Chatbox rules already in code

- Display order: `created_at` ascending (tie-break `id`).
- Pagination cursor: message **primary key**, not timestamp.
- Optimistic send: client UUID `echo_id` on the create response only.
- Private notes persist; there is no channel send yet anyway.
- API inbox: `can_reply` is true (same as Chatwoot `Channel::Api` without a reply window).

### Tests

`./gradlew test` — MessageFinder latest/before/after, list uses `display_id`, POST returns `echo_id` and bumps `last_activity_at`, profile/inboxes/agents.

### Intentionally not done

- Vue Keycloak login / Axios Bearer adapter (dashboard still speaks Devise headers).
- ActionCable / live `message.created`.
- Facebook Messenger webhook + Graph send.
- Captain / Spring AI.
- My Inbox (notifications center) as a product.
- Attachments, labels as first-class data, teams, custom roles.

---

## 3. Where to start (next session)

Work in **this repo**, not in Chatwoot, unless you are comparing a jbuilder/finder.

### Start here if you are new

1. Open `/home/newroz/open-source/chatwoot-spring`.
2. Read this file, then `README.md`.
3. Run tests: `./gradlew test`.
4. Compare JSON to Chatwoot sources of truth (do not “improve” the shape):
   - `chatwoot/app/views/api/v1/accounts/conversations/index.json.jbuilder`
   - `chatwoot/app/views/api/v1/conversations/partials/_conversation.json.jbuilder`
   - `chatwoot/app/views/api/v1/models/_message.json.jbuilder`
   - `chatwoot/app/finders/message_finder.rb`
   - `chatwoot/app/finders/conversation_finder.rb`
   - `chatwoot/app/builders/messages/message_builder.rb`

### Recommended next builds (in order)

**A. Make the Vue Conversations screen hit this API (smallest frontend change)**

- Keycloak login in the dashboard (or a local proxy that injects a token).
- Replace Devise headers in `app/javascript/dashboard/helper/APIHelper.js` with `Authorization: Bearer`.
- Point `chatwootConfig.apiHost` at this server, or reverse-proxy `/api` and `/api/v1/profile`.
- Seed Keycloak users with the same emails as `users.email`.

Until A is done, you can only exercise the API with curl/HTTP files + JWT.

**B. Live chatbox (needed for “messages in the right order while the thread is open”)**

- Emit `message.created` / `message.updated` with the same payload as message JSON (including `echo_id`).
- Prefer a thin Vue WebSocket adapter over cloning ActionCable’s wire protocol.

**C. Facebook / Messenger (first real channel)**

- Settings callbacks: `facebook_pages`, `register_facebook_page`, `reauthorize_page`.
- Inbound webhook (Meta signature, **not** JWT).
- Outbound Graph send after `MessageBuilder`; set `source_id`; skip Chatwoot echoes.

**D. Captain with Spring AI**

- Same Chatwoot routes: `/captain/tasks/*`, copilot threads, then inbox auto-reply as a normal **outgoing** message through `MessageBuilder`.

Do not start C or D until Conversations REST + (ideally) B work against the existing Vue thread.

### Map of this codebase

| Package | Role (Chatwoot analogue) |
|---|---|
| `domain` | Models / tables |
| `finder` | `ConversationFinder`, `MessageFinder` |
| `builder` | `Messages::MessageBuilder`, display_id sequence |
| `web.api.v1` | Controllers / routes |
| `web.dto.ChatwootJson` | Jbuilder JSON |
| `security` | JWT → user by email |

---

## 4. Local run

Java 21, PostgreSQL, Keycloak realm with an issuer URI.

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/chatwoot_spring
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/chatwoot
export CHATWOOT_SEED=true
./gradlew bootRun
```

Seed emails: `agent@example.com`, `admin@example.com`.

Call example (replace the token):

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/accounts/1/conversations?assignee_type=me
```
