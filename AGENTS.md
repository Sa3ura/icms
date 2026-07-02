# Repository Guidelines

## Project Structure & Module Organization

This repository is split into `ICMS-frontend/` and `ICMS-backend/`. The frontend is a Vue 3 + Vite app: source lives in `ICMS-frontend/src`, with API helpers in `src/api`, Pinia state in `src/store`, routing in `src/router`, reusable logic in `src/composables`, assets in `src/assets`, and pages in `src/views`. The backend is a Spring Boot 3 app under `ICMS-backend/src/main/java/com/icms`, organized by `controller`, `service`, `mapper`, `entity`, `config`, `security`, and `common`. Resources, Flyway migrations, and configuration files live in `ICMS-backend/src/main/resources`.

## Build, Test, and Development Commands

- `cd ICMS-frontend && npm run dev`: start the Vite development server.
- `cd ICMS-frontend && npm run build`: create the production frontend build in `dist/`.
- `cd ICMS-frontend && npm run preview`: preview the built frontend locally.
- `cd ICMS-frontend && npm run lint`: run ESLint and auto-fix Vue/JS files.
- `cd ICMS-backend && .\mvnw.cmd spring-boot:run`: start the backend on Windows.
- `cd ICMS-backend && .\mvnw.cmd test`: run backend tests when present.
- `cd ICMS-backend && .\mvnw.cmd package`: compile, test, and package the Spring Boot artifact.

## Coding Style & Naming Conventions

Use Java 17 with 4-space indentation. Keep backend classes in the existing `com.icms` package layers and follow names such as `UserController`, `UserService`, `UserServiceImpl`, `UserMapper`, and entity names matching domain tables. Use Lombok and MyBatis-Plus patterns already present in the codebase.

Frontend code uses Vue 3 Composition API, ES modules, and 2-space indentation. Name page components in PascalCase, composables as `useThing.js`, and route/store modules by feature. Run `npm run lint` before committing; the ESLint config allows console usage and warns on unused variables.

## Testing Guidelines

No frontend test runner is currently configured, so validate frontend changes with `npm run lint` and `npm run build`. Add backend tests under `ICMS-backend/src/test/java` using Spring Boot Test and name test classes `*Test`. For database-sensitive changes, verify Flyway migrations in `src/main/resources/db/migration`.

## Commit & Pull Request Guidelines

Recent history uses Conventional Commit prefixes, especially `fix:`. Keep messages concise and scoped, for example `fix: adjust classroom filters` or `feat: add device status chart`. Pull requests should include a summary, affected areas, validation commands, linked issues, and screenshots for visible UI changes.

## Security & Configuration Tips

Do not commit real credentials or local secrets. Use `ICMS-backend/src/main/resources/application-local.yml` for local database/JWT settings and keep `.env.development` or `.env.production` aligned with the intended API mode. Review authentication, role checks, and ID ownership checks when changing protected backend endpoints. Backend APIs must derive actor identity from `StpUtil`, not request bodies.
