# Bob web (Chatwoot agent dashboard)

Direct port of Chatwoot’s Vue agent dashboard. Agent login uses Keycloak (Authorization Code + PKCE), the same model as silkroad-fe. API calls send `Authorization: Bearer <access-token>`.

## Requirements

- Node 24.x (Node 22 works, with a pnpm engine warning)
- pnpm 10.x

If Vite cannot find the `esbuild` binary after install, run `pnpm rebuild esbuild`.

## Keycloak

Copy `.env.example` to `.env.local` (already gitignored). Defaults match silkroad-fe local:

```
VITE_API_HOST=http://localhost:8080
VITE_KEYCLOAK_URL=https://dev-kc.getsport360.com
VITE_KEYCLOAK_REALM=test_realm
VITE_KEYCLOAK_CLIENT_ID=silk_road-pulic-client
```

On that public client, add Bob’s origin (Vite uses port 5173; use another port if silkroad-fe is already bound there):

- Valid redirect URIs: `http://localhost:<port>/app/login/callback`
- Post-logout redirect URIs: `http://localhost:<port>/app/login`
- Web origins: `http://localhost:<port>`

Spring must validate the same issuer. Do not change committed `application.yml` defaults; set this locally when running Bob:

```bash
export KEYCLOAK_ISSUER_URI=https://dev-kc.getsport360.com/realms/test_realm
```

The JWT email must already exist as a Bob user or `GET /api/v1/profile` returns 401.

API requests go directly to `VITE_API_HOST` (Spring Boot, default `http://localhost:8080`).

## Run

```bash
cd web
pnpm install
pnpm dev
```

Then open http://localhost:5173/app/login. Sign in redirects to Keycloak and back to `/app/login/callback`.

| Path | Pack |
|---|---|
| `/app/login`, `/app/login/callback` | v3 login |
| `/app/*` | dashboard |

`pnpm build` writes to `dist/`. This app is not served by Spring Boot yet.

## What this is

Copied from Chatwoot: `dashboard`, `v3`, `shared`, and `widget` (helpers only). ActionCable, Captain, and some inbox settings still expect Rails.
