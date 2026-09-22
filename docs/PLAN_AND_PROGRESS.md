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

Location: `/home/shamim/open-source/bob`

Stack: Java 21, Spring Boot 3.4, Gradle, JPA, Flyway, PostgreSQL (H2 in tests), OAuth2 resource server.

### Auth

- JWT required on `/api/**`. Public: `/actuator/health` and Facebook webhook `GET/POST /bot` (Meta HMAC, not JWT).
- `CurrentUserService` reads JWT `email` (fallback `preferred_username`), loads `users` + `account_users`. Unknown email → 401.
- `GET /api/v1/profile` → `{ payload: { success, data } }` in Chatwoot user shape (`accounts[]`, `ui_settings`, `pubsub_token`).

### Data (Chatwoot-shaped tables)

`accounts`, `users`, `account_users`, `inboxes`, `inbox_members`, `contacts`, `contact_inboxes`, `conversations`, `messages`, plus a `conversation_display_id_counters` table (Chatwoot uses per-account sequences).

Inbox `channel_type` is a string (`Channel::Api` in seed, `Channel::FacebookPage` for Messenger). `channel_id` points at the channel table (`channel_facebook_pages.id` for Facebook). Message `message_type` / `status` / `content_type` are integers in the DB; JSON uses Chatwoot’s mix (integer `message_type`, string `status` like `"sent"`).

### Conversation APIs (ported behavior)

| Method | Path | Chatwoot behavior we copied |
|---|---|---|
| GET | `/api/v1/accounts/{id}/conversations` | `ConversationFinder`: default status `open`, `assignee_type`, inbox membership, sort `last_activity_at_desc`, page 25. Body `{ data: { meta, payload } }`. Conversation `id` = **display_id**. |
| GET | `.../conversations/meta` | Same counts |
| GET | `.../conversations/{displayId}` | Conversation partial |
| GET | `.../conversations/{displayId}/labels` | `{ payload: string[] }` from `cached_label_list` |
| GET | `.../conversations/{displayId}/attachments` | `{ meta: { total_count: 0 }, payload: [] }` until files are stored |
| GET | `.../assignable_agents` | Inbox-member intersection plus administrators; `assignee_type` when `include_ai_assignees` is set |
| GET | `.../contacts/{id}/conversations` | Last 25 for the contact, or neighbour window when `conversation_id` is a display_id |
| GET | `.../integrations/apps` | `{ payload: [] }` |
| GET | `.../messages` | `MessageFinder`: latest 20 asc; `before` 20 then reverse; `after` 100 |
| POST | `.../messages` | `MessageBuilder`; `echo_id` not persisted; returned on JSON; Facebook inboxes then Graph-send and set `source_id` |
| POST | `.../update_last_seen` | `agent_last_seen_at` |
| POST | `.../toggle_status` | open / resolved / pending / snoozed |
| POST | `.../assignments` | `assignee_id` |

Supporting reads so the conversation page does not 404: account, inboxes, agents, assignable agents, contact show, contact conversations, conversation labels (from `cached_label_list`), empty integrations apps, empty conversation attachments, empty account labels/teams/custom filters/attributes, notifications unread `0`.

### Chatbox rules already in code

- Display order: `created_at` ascending (tie-break `id`).
- Pagination cursor: message **primary key**, not timestamp.
- Optimistic send: client UUID `echo_id` on the create response only.
- Private notes persist and are **not** sent to Facebook.
- API inbox: `can_reply` is true. Facebook (and any non-API inbox): `can_reply` follows a 24h window from the last incoming message.

### Tests

`./gradlew test` — MessageFinder latest/before/after, list uses `display_id`, POST returns `echo_id` and bumps `last_activity_at`, profile/inboxes/agents, Facebook callbacks/webhook/send/inbox members.

### Intentionally not done (still)

- ActionCable / live `message.created` (refresh Conversations to see inbound Facebook messages).
- `GET .../inboxes/:id` (inbox settings after create).
- Attachment storage (conversation attachments read returns an empty payload), avatars, delivery/read receipts, Instagram-on-page DMs.
- WhatsApp, Captain / Spring AI, My Inbox, account label catalog, `POST .../labels`, teams as first-class data.

---

## 2b. Facebook Messenger (shipped)

Package: `com.bob.api.integration.facebook`. Vue wizard is in [bob-web](https://github.com/shamim51/bob-web); backend matches Chatwoot paths and JSON.

### Product journey

1. Settings → Inboxes → Facebook (tile needs `window.chatwootConfig.fbAppId` and account feature `channel_facebook`).
2. FB.login → `POST /api/v1/accounts/{id}/callbacks/facebook_pages.json` `{ omniauth_token }` → `{ data: { page_details, user_access_token } }`.
3. Pick page → `POST .../callbacks/register_facebook_page` → small inbox JSON (`id`, `channel_id`, `name`, `channel_type`, `avatar_url`, `page_id`, `enable_auto_assignment`).
4. **Add agents** (required for non-admins to see the inbox): `GET .../agents` then `PATCH .../inbox_members` `{ inbox_id, user_ids }`. Administrators see all inboxes without membership; agents only see inboxes they belong to.
5. Reauth later: `POST .../callbacks/reauthorize_page` `{ omniauth_token, inbox_id }` → `{ data: <inbox> }` or `422`.
6. Customer messages the Page → Meta `GET/POST /bot` → contact + conversation + incoming text.
7. Agent `POST .../conversations/{displayId}/messages` → Graph `me/messages`; `source_id` = Graph `message_id`. Echoes from our `FB_APP_ID` are skipped.

Inbox list adds Facebook-only keys (`page_id`, `provider_name`, `reauthorization_required`) and omits them for `Channel::Api`.

### Meta / ops (not automatic)

Chatwoot does **not** register the Facebook webhook URL. You set it in the Meta app. Bob only **subscribes the page** after inbox create.

| Env | Role |
|---|---|
| `FB_APP_ID` | Graph app id; must match `bob-web/public/window-config.js` `fbAppId` |
| `FB_APP_SECRET` | Token exchange + `X-Hub-Signature-256` |
| `FB_VERIFY_TOKEN` | `GET /bot?hub.verify_token=` |
| `FACEBOOK_API_VERSION` | default `v18.0` |

Also required: Messenger product on the app, page permissions (`pages_messaging`, `pages_show_list`, …), and a **public HTTPS** callback `{host}/bot` (tunnel for local). Empty Spring Facebook env vars will fail page listing.

### What this run does not do

- Live thread updates (no websocket). Refresh to see inbound.
- Images/files (no attachments table; conversation attachments API returns an empty payload).
- `HUMAN_AGENT` send tag (Graph may reject replies outside the 24h window).

---

## Frontend (direct port)

The agent dashboard lives in [shamim51/bob-web](https://github.com/shamim51/bob-web) (`../bob-web` locally). See that repo’s [docs/PLAN_AND_PROGRESS.md](https://github.com/shamim51/bob-web/blob/main/docs/PLAN_AND_PROGRESS.md). Run `pnpm install && pnpm dev` there and open `/app/login`.

Agent login uses Keycloak Authorization Code + PKCE (same realm/client as silkroad-fe). Point `VITE_API_HOST` at Spring and set `KEYCLOAK_ISSUER_URI` locally to that realm. ActionCable replacement is **not** done.

---

## 3. Where to start (next session)

Work in **this repo**, not in Chatwoot, unless you are comparing a jbuilder/finder.

### Start here if you are new

1. Open `/home/shamim/open-source/bob`.
2. Read this file, then `README.md`.
3. Run tests: `./gradlew test`.
4. Compare JSON to Chatwoot sources of truth (do not “improve” the shape). Map each jbuilder to a DTO in the feature’s `dto` package:
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

### Frontend (direct port)

Dashboard: [shamim51/bob-web](https://github.com/shamim51/bob-web) (`../bob-web`). Keycloak login is wired. Point `VITE_API_HOST` at Spring so profile and conversations use Bearer tokens.

**B. Live chatbox (needed for “messages in the right order while the thread is open”)**

- Emit `message.created` / `message.updated` with the same payload as message JSON (including `echo_id`).
- Prefer a thin Vue WebSocket adapter over cloning ActionCable’s wire protocol.

**C. Facebook / Messenger** — **done** (text in/out + add-provider + add-agents). Remaining: live events, attachment storage.

**D. WhatsApp Cloud** (next channel)

- Create: `POST /inboxes` `{ channel: { type: "whatsapp", provider: "whatsapp_cloud", ... } }` first; embedded signup later.
- Webhook: public `GET/POST /webhooks/whatsapp/{phone_number}` + Graph callback register.
- Send through the same `SendReplyService` hook.

**E. Captain with Spring AI**

- Same Chatwoot routes: `/captain/tasks/*`, copilot threads, then inbox auto-reply as a normal **outgoing** message through `MessageBuilder`.

### Map of this codebase

Package-by-feature under `com.bob.api`. Each feature owns `controller`, `model`, `repository`, `dto`, `mapper`, and (when Chatwoot has them) `finder` / `builder`.

| Package | Role (Chatwoot analogue) |
|---|---|
| `account` | Account, User, AccountUser, profile, agents |
| `contact` | Contact, ContactInbox |
| `inbox` | Inbox, InboxMember, `PATCH /inbox_members` |
| `conversation` | Conversation, ConversationFinder, display_id |
| `messaging` | Message, MessageFinder, MessageBuilder, `SendReplyService` |
| `integration.facebook` | Callbacks, `/bot` webhook, Graph client, `FacebookPage` |
| `notification` / `label` / `team` / `customattribute` / `customfilter` / `integration.apps` | stub supporting reads |
| `shared.dto` | timestamps, `{ payload }` wrapper |
| `security` | JWT → user by email |
| `config` | Security, CORS, seed |

---

## 4. Local run

Java 21, PostgreSQL, Keycloak realm with an issuer URI.

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/bob
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/bob
export BOB_SEED=true
export FB_APP_ID=
export FB_APP_SECRET=
export FB_VERIFY_TOKEN=
./gradlew bootRun
```

Seed emails: `agent@example.com`, `admin@example.com`.

Call example (replace the token):

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/accounts/1/conversations?assignee_type=me
```
