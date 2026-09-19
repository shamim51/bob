# Bob — Agent Guidelines

Bob is an independent Spring Boot API that mirrors Chatwoot's REST contracts. Chatwoot (sibling repo in the workspace) is **read-only reference** — never edit it.

## Where to work

- **Write code only in `bob/`**
- **Read/compare in `chatwoot/`** when porting behavior or JSON shape
- Open `bob.code-workspace` so both repos are in scope

## How to mirror Chatwoot

Work in **vertical slices** (one API at a time), not big horizontal rewrites. Land each slice **inside its feature package**, not in global `domain` / `finder` / `web.api.v1` layers.

1. Find the Vue call → Rails route → controller → finder/builder → jbuilder → spec
2. Port the same slice in bob under the feature: `controller` → `finder`/`builder` (if Chatwoot has one) → `model` + `repository` → feature `dto` + `mapper`
3. Keep Chatwoot class names (`MessagesController`, `MessageFinder`, `MessageBuilder`) and Chatwoot field names (`display_id`, `echo_id`, `assignee_type`, integer enums)
4. Match JSON from jbuilder files — do not redesign the response shape
5. Port spec scenarios into `./gradlew test` where useful
6. Tests follow the same feature packages as production code

Do **not** recreate root-level `finder/`, `builder/`, `domain/`, `repo/`, or `web.dto.response/`.

## JSON / DTOs

Do **not** build responses with `Map<String, Object>` or a shared JSON bag. Use Spring-style records:

- One **response DTO** per Chatwoot jbuilder / payload, in the feature's `dto` package
- One **mapper** per feature that copies model → DTO (repository lookups stay in the mapper, not in the DTO)
- Do **not** revive a global `ChatwootResponseMapper`
- Controllers return DTO types; Jackson `SNAKE_CASE` + `ALWAYS` inclusion is already configured
- Preserve Chatwoot quirks with field annotations, not extra maps:
  - `@JsonProperty("id")` on conversation `displayId`
  - `@JsonProperty("private")` on message `privateMessage`
  - `@JsonInclude(NON_NULL)` for keys Chatwoot omits (`echo_id`, `sender`, optional assignee)
  - `@JsonInclude(NON_EMPTY)` when Chatwoot skips empty objects (`account.custom_attributes`)
  - unix `long` vs `double` timestamps as in the jbuilder (`created_at` vs `updated_at` on conversations)
- Shared wrappers only: `shared.dto.PayloadResponse`, `shared.dto.ChatwootTimestamps`
- When adding an endpoint: open the Chatwoot jbuilder first, add a matching DTO in the feature, map in that feature's mapper, return it from the controller, assert JSON keys in a test

## Package map

Root: `com.bob.api`. Layout is **package-by-feature**:

```
com.bob.api
├── config/           # SecurityConfig, Cors, seed
├── security/         # CurrentUserService (JWT → user by email)
├── shared/dto/       # ChatwootTimestamps, PayloadResponse
├── account/          # Account, User, AccountUser, profile, agents
├── contact/
├── inbox/
├── conversation/
├── messaging/
├── integration.facebook/  # callbacks, /bot webhook, Graph send
├── notification/     # stub unread/list
├── label/            # stub
├── team/             # stub
├── customattribute/  # stub
└── customfilter/     # stub
```

Each feature uses:

| Feature folder | Role (Chatwoot analogue) |
|---|---|
| `controller/` | controllers / routes |
| `model/` | models / tables |
| `repository/` | Spring Data repos |
| `dto/` | `.json.jbuilder` payloads |
| `mapper/` | jbuilder helpers / partials |
| `finder/` | `app/finders/*` — only inside the feature |
| `builder/` | `app/builders/*` — only inside the feature |
| `service/` | other feature services (e.g. display_id sequence) |

Cross-feature imports are expected (JPA FKs, nested JSON). Facebook lives under `integration.facebook`. Further channels (`integration.instagram`, WhatsApp, …) start as new packages when that slice starts — do not create empty `integration` packages ahead of time.

## JPA / LazyInitializationException

`spring.jpa.open-in-view` is **`false`** on purpose. Repository and finder queries close the persistence context when they return. Controllers and mappers then run on **detached** entities.

If a mapper reads a lazy association (anything except the entity **primary key** on a proxy), Hibernate throws `LazyInitializationException: Could not initialize proxy … - no session`.

Chatwoot `conversation_id` in message JSON is `display_id`, not `conversations.id`. `message.getConversation().getId()` is safe; `getDisplayId()` is not, unless `conversation` was join-fetched or the parent already passed `displayId` into the mapper.

**Do this instead of turning OSIV back on:**

- `join fetch` / Criteria `root.fetch(...)` / `@EntityGraph` for every association the mapper reads
- Pass already-loaded scalar values into nested mappers (e.g. `MessageMapper.message(message, conversation.getDisplayId())`)
- Hydrate transient bits (`MessageHydrator`) before mapping senders
- `@Transactional(readOnly = true)` on a mapper only as a last resort when the graph is awkward (profile). Do not put `@Transactional` on list controllers or conversation/message mappers to hide missing fetches

Examples already in the tree: `ConversationFinder` fetches `inbox`/`contact`/`assignee`/`contactInbox`; `ConversationRepository.findByAccountIdAndDisplayId` uses `join fetch`; `AccountUserRepository` uses `@EntityGraph`; message list queries fetch `m.conversation`.

`@Transactional` on a Spring MVC test keeps a session open for MockMvc and **will not catch this**. Add at least one request test without a class-level transaction (see `ConversationListLazyInitTest`).

## Rules

- Same URL paths, query params, status codes, and JSON keys as Chatwoot
- `conversation.id` in API = `display_id`, not internal PK
- Prefer the smallest change that satisfies the contract
- Read `docs/PLAN_AND_PROGRESS.md` before starting a new feature

## Build / test

```bash
./gradlew test
./gradlew bootRun
```

Java 21, PostgreSQL, Keycloak JWT resource server. See `README.md` for env vars.
