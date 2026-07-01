# AGENTS.md

Compact agent guide for the ICMS repo. Authoritative deep-dive: [`CLAUDE.md`](./CLAUDE.md) — read it before non-trivial work. Requirements/schema intent also in the root `ICMS_*.md` design docs.

## Layout

Two independent packages, no monorepo tooling — always `cd` into the package before running commands.

- `ICMS-frontend/` — Vue 3 SPA (Vite, Pinia, Element Plus, Vue Router hash mode). Entry: `src/main.js` → `src/router/index.js` + `src/layout/index.vue`.
- `ICMS-backend/` — Spring Boot 3 REST API on `:8080`. Layering: `controller -> service(impl) -> mapper (MyBatis-Plus) -> entity`. Base package `com.icms`.

## Commands

Frontend (`ICMS-frontend/`):
```bash
npm install
npm run dev        # Vite dev server :5173
npm run build      # -> dist/
npm run lint       # eslint src --ext .vue,.js --fix  (lint == format; both auto-fix)
```
No typecheck step (plain JS). No test runner is configured.

Backend (`ICMS-backend/`, Windows PowerShell):
```powershell
.\mvnw.cmd spring-boot:run    # starts :8080; Flyway auto-migrates, then DatabaseInitializer seeds demo data
.\mvnw.cmd test               # JUnit; no tests exist yet
.\mvnw.cmd clean package      # build jar
```
On startup the backend auto-creates schema + seed data — no manual SQL import. API docs at `http://localhost:8080/doc.html` (Knife4j).

## Gotchas an agent will likely miss

- **Dual-mode API facade (critical).** `ICMS-frontend/src/api/index.js` switches on `VITE_USE_MOCK`. When `true` it serves `src/db/mockDb.js` (localStorage); when `false` it calls the real backend via axios. **Every new API function must implement both branches** or one mode silently breaks. Note: both `.env.development` and `.env.production` currently ship with `VITE_USE_MOCK=false`.
- **Role guard lives in `main.js`** (`router.beforeEach`), not in `src/router/index.js`. Each route carries `meta.roles: [...]` and `meta.requiresAuth`. Add the route + sidebar entry + `meta.roles` together. Roles: `admin`, `jw`, `teacher`, `student`, `repairer`.
- **snake_case ⇄ camelCase boundary** spans three layers — keep all consistent when adding a field or it vanishes: (1) MyBatis-Plus `map-underscore-to-camel-case` maps DB↔entity; (2) controllers accept snake_case query params explicitly (`@RequestParam Long campus_id`); (3) `common/BeanUtils.toMap()` converts entity→snake_case `Map` before returning. Frontend/DB speak snake_case (`campus_id`, `user_type`); Java entities are camelCase. Enum→label translation centralized in `ICMS-frontend/src/utils/mappings.js`.
- **Auth is SaToken (JWT stateless), NOT Spring Security.** Only BCrypt is borrowed from spring-security-crypto. `SaInterceptor` guards `/api/**` with an explicit exclude list in `SaTokenConfig`; roles resolved via `StpInterfaceImpl` reading the `role` claim. Mutating controllers use `@SaCheckRole(value = {"admin","jw"}, mode = SaMode.OR)`. Double-token: access token held in a JS module variable (never localStorage in real mode), refresh token in httpOnly cookie `icms_rf`; axios 401 interceptor calls `POST /api/auth/refresh` once (deduped via `refreshPromise`).
- **Response shape.** Controllers return `Result<T>` = `{code, message, data}`; `code == 200` means success. Business errors throw `CustomException` (caught by `GlobalExceptionHandler`). Paginated lists return `{ total, records: [...] }`; mock mode mirrors this via `mockPaginate`.
- **Audit logging:** `@Log("描述")` on a controller method → `LogAspect` (AOP) writes an `OperationLog` row.
- **Login security:** SVG captcha (`CaptchaUtil`, id in an HTML comment on `/api/auth/captcha`) + `LoginLimiter` (5 failures / 5 min per IP → 15-min block).

## Database / migrations

- Flyway versioned SQL in `ICMS-backend/src/main/resources/db/migration/` (`V1__init_schema.sql`, `V2__add_refresh_token.sql`). **Add schema changes as a new `V{n}__desc.sql` — never edit an applied migration** (Flyway checksums them; `clean-disabled: true`).
- Logic-delete is global: column `deleted`, `1` = deleted. Filtering uses MyBatis-Plus `LambdaQueryWrapper`.
- DB connection is **env-var overrideable**, not hardcoded: `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` (defaults in `application.yml` point at `localhost:3306/icms` with empty password). Secrets can also go in `application-local.yml` (gitignored, `optional:classpath:` import) — do not commit real credentials. (CLAUDE.md says "hardcoded"; prefer the env/local-file approach.)

## Style / conventions

- UI text, doc strings, and code comments are **Simplified Chinese** — match this for new UI strings/comments.
- ECharts is imported on-demand (Line/Bar/Pie + **SVGRenderer** only) per-component — don't pull the full bundle. (README/CLAUDE.md say "CanvasRenderer"; the code actually uses SVGRenderer.)
- `useHmrData` composable re-fetches data after a Vite HMR update so dev edits don't leave pages stale.
- Default seed accounts (password `123456`): `admin`, `jw_admin`, `teacher_wang`, `student_li`, `repairer_zhang`.
- Logging is Log4j2 (Logback excluded); config in `log4j2.xml`, output under `ICMS-backend/logs/`.
