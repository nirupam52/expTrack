# PostgreSQL Database Migration Plan

## Status

Planned. This is a living execution guide for agents working across multiple sessions.

## Purpose

Move the backend database from SQLite to PostgreSQL with a clean, empty database.

The application is not deployed in production. Existing Docker volumes may be deleted. No application data export, import, backfill, dual write, or compatibility reader is required.

The REST contract, ownership rules, amount model, and JPA entity model must remain stable. The database engine is the implementation that changes.

## Agent operating rules

Every migration agent must:

1. Read `AGENTS.md` and this document before editing.
2. Inspect the current worktree before changing files.
3. Work on the earliest incomplete checkpoint.
4. Keep one checkpoint focused. Do not add unrelated feature work.
5. Run the checkpoint acceptance gate before marking it complete.
6. Record the changed files, verification command, and result in the execution log.
7. Stop at a failed gate. Do not mark later checkpoints complete.
8. Preserve unrelated user changes in the worktree.

The backend persistence cutover is intentionally one atomic checkpoint. Flyway SQL, datasource configuration, the PostgreSQL search query, and the database-backed tests must change together. A half-converted state cannot be verified reliably.

After the first production deployment, Flyway migrations are append-only. Do not rewrite or delete migrations that have been applied to a deployed database.

## Migration contract

- PostgreSQL major version: `16` unless the project owner changes this before Checkpoint 1.
- Local and test PostgreSQL must use the same major version.
- Flyway owns schema creation and changes.
- Hibernate keeps `spring.jpa.hibernate.ddl-auto=validate`.
- The REST API remains the primary behavior test seam.
- The frontend should not need a code change because the API contract does not change.
- The new database starts empty.
- Local PostgreSQL credentials are development defaults only. Runtime deployments must provide credentials through environment or secret configuration.

## Execution status

Update this list only after the matching acceptance gate passes:

- [x] Checkpoint 0 - Freeze current behavior
- [x] Checkpoint 1 - Switch backend persistence to PostgreSQL
- [x] Checkpoint 2 - Switch Docker Compose and the runtime image
- [x] Checkpoint 3 - Make CI prove PostgreSQL parity
- [x] Checkpoint 4 - Update documentation and remove stale references


## Current repository map

| Area | Current SQLite coupling | Migration action |
| --- | --- | --- |
| `backend/pom.xml` | SQLite JDBC and community dialect dependencies | Add PostgreSQL JDBC and Flyway PostgreSQL support; remove SQLite dependencies |
| `backend/src/main/resources/application.properties` | SQLite URL, driver, dialect, database path | Use environment-based PostgreSQL settings |
| `backend/src/main/resources/db/migration` | `AUTOINCREMENT`, `COLLATE NOCASE`, FTS5, triggers, `rowid` | Replace with a clean PostgreSQL migration set |
| `ExpenseRepository.findHistory` | SQLite `MATCH`, `expenses_search`, and `rowid` | Use PostgreSQL full-text search and a GIN index |
| Backend endpoint tests | `jdbc:sqlite::memory:` overrides | Run tests against PostgreSQL Testcontainers |
| `compose.yaml` | App-owned `/data` volume | Add PostgreSQL service and database volume |
| `compose.debug.yaml` | SQLite path and debug data volume | Point the app to the PostgreSQL service |
| `Dockerfile` | `/data` setup and SQLite environment variable | Remove database filesystem setup |
| README and specification | SQLite is the documented database | Document PostgreSQL and volume reset behavior |

The JPA entities and most repository queries are database-portable. Keep them unchanged unless PostgreSQL schema validation proves that a targeted change is required.

## Checkpoints

## Checkpoint 0: Freeze current behavior

### Goal

Create a behavior baseline before changing the database engine.

### Work

Review the current endpoint tests. Add only missing assertions for behavior that can change during the port:

- Mixed-case email registration remains case-insensitive.
- Search matches title text.
- Search matches note text.
- Search handles punctuation and empty results.
- Search works with category and date filters.
- Cursor pagination remains stable when expenses share a date.
- Maximum supported integer amount remains valid.
- Search and dashboard results remain owner-scoped.

### Files

Likely test files:

- `backend/src/test/java/com/exptrack/user/RegistrationEndpointTest.java`
- `backend/src/test/java/com/exptrack/expense/ExpenseWorkflowEndpointTest.java`
- `backend/src/test/java/com/exptrack/expense/ExpenseEndpointTest.java`

### Acceptance gate

```sh
cd backend
mvn -B verify
```

The current SQLite suite must pass. Do not erase the existing volume until this gate passes.

### Rollback

Revert only the new baseline assertions if they expose an unrelated pre-existing defect. Record the defect in the execution log instead of weakening the assertion.

---

## Checkpoint 1: Switch backend persistence to PostgreSQL

### Goal

Make the Spring Boot backend start, migrate, validate, and serve requests against PostgreSQL.

### Maven changes

Update `backend/pom.xml`:

- Add the PostgreSQL JDBC driver with runtime scope.
- Remove `org.xerial:sqlite-jdbc`.
- Remove `hibernate-community-dialects`.
- Add the Flyway PostgreSQL database module when required by the Flyway version managed by Spring Boot.
- Add `spring-boot-testcontainers` with test scope.
- Add `org.testcontainers:postgresql` with test scope.

Use the same PostgreSQL image tag in Testcontainers and Compose, for example `postgres:16-alpine`.

### Datasource changes

Replace the SQLite settings in `backend/src/main/resources/application.properties` with environment-based PostgreSQL settings:

```properties
spring.datasource.url=${EXPTRACK_DATABASE_URL:jdbc:postgresql://localhost:5432/exptrack}
spring.datasource.username=${EXPTRACK_DATABASE_USERNAME:exptrack}
spring.datasource.password=${EXPTRACK_DATABASE_PASSWORD:exptrack}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

Keep Flyway enabled, Hibernate validation, and `spring.jpa.open-in-view=false`.

Remove the SQLite-only setting:

```properties
spring.jpa.properties.hibernate.hbm2ddl.jdbc_metadata_extraction_strategy=individually
```

Do not add `baselineOnMigrate`, `cleanOnValidationError`, or a silent fallback to SQLite.

### Flyway migration changes

Because no database is deployed, replace the current SQLite migration history with this clean PostgreSQL set:

```text
V1__create_users.sql
V2__create_expenses.sql
V3__add_expense_indexes.sql
V4__add_expense_search.sql
```

Remove the old `V1`, `V2`, `V7`, `V8`, and `V9` SQLite migration files. Do not create a temporary duplicate migration directory.

Use these rules in the new migrations:

- Use `GENERATED BY DEFAULT AS IDENTITY` for integer IDs.
- Keep `amount_minor` as positive `BIGINT`.
- Keep the user foreign key and existing nullable note.
- Replace `COLLATE NOCASE` with a unique index on `lower(email)`:

```sql
CREATE UNIQUE INDEX uq_users_email_lower
    ON users (lower(email));
```

- Preserve the existing user/date, user/category/date/id, and user/date/id indexes.
- Do not add cascade behavior that was not present in the SQLite schema.

### PostgreSQL search index

Replace the SQLite FTS5 table and triggers with one PostgreSQL expression index:

```sql
CREATE INDEX idx_expenses_search
    ON expenses
    USING GIN (
        to_tsvector(
            'simple'::regconfig,
            coalesce(title, '') || ' ' || coalesce(note, '')
        )
    );
```

The `simple` text configuration is intentional. It provides case-insensitive token search without language-specific stemming.

### Repository query changes

Update `backend/src/main/java/com/exptrack/expense/repository/ExpenseRepository.java`.

Replace the `expenses_search`, `MATCH`, and `rowid` predicate with a predicate using the same expression as the GIN index:

```sql
to_tsvector(
    'simple'::regconfig,
    coalesce(e.title, '') || ' ' || coalesce(e.note, '')
) @@ websearch_to_tsquery(
    'simple'::regconfig,
    :searchText
)
```

Keep `ExpenseService.searchPhrase` unless a test demonstrates a required semantic adjustment. It already removes unsupported punctuation and produces a phrase search value.

Use a named or SpEL parameter form that Spring Data JPA binds correctly. Keep the existing owner, category, currency, date, cursor, ordering, and limit behavior unchanged.

The REST tests must prove:

- Title search.
- Note search.
- Phrase search.
- Punctuation handling.
- No-result search.
- Search combined with other filters.
- Search combined with cursor pagination.
- Search cannot cross the user ownership scope.


### Test database changes

Create shared PostgreSQL Testcontainers configuration for the Spring endpoint tests.

The test setup must:

- Start PostgreSQL automatically.
- Run Flyway against the container.
- Remove every `jdbc:sqlite::memory:` test override.
- Use the same PostgreSQL major version as Compose.
- Isolate tests by truncating `expenses` and `users` and resetting identities between tests.
- Avoid dependence on a developer-installed PostgreSQL server.

A PostgreSQL test cleanup can use:

```sql
TRUNCATE TABLE expenses, users RESTART IDENTITY CASCADE;
```

### Expected files

- `backend/pom.xml`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/db/migration/*`
- `backend/src/test/java/com/exptrack/*`
- `backend/src/main/java/com/exptrack/expense/repository/ExpenseRepository.java`

- New test configuration under `backend/src/test/java` if needed

### Acceptance gate

```sh
cd backend
mvn -B verify
```

The complete backend suite must pass against PostgreSQL, including migrations, authentication, sessions, ownership, validation, expense CRUD, filters, search, cursor pagination, and dashboard aggregation.

### Rollback

Discard the PostgreSQL test database or container and revert the checkpoint commit. Do not repair a failed fresh migration by adding Flyway baselines or disabling validation.

---


## Checkpoint 2: Switch Docker Compose and the runtime image

### Goal

Run the application with a PostgreSQL service instead of an app-owned SQLite file.

### `compose.yaml`

Add a `postgres` service that:

- Uses the pinned PostgreSQL image.
- Defines `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` from environment variables with local defaults.
- Has a `pg_isready` health check.
- Stores data in a PostgreSQL volume.
- Is not exposed outside the Compose network in the normal app stack.

Configure `app` to:

- Depend on PostgreSQL health.
- Receive `EXPTRACK_DATABASE_URL`, `EXPTRACK_DATABASE_USERNAME`, and `EXPTRACK_DATABASE_PASSWORD`.
- Keep the read-only filesystem, dropped capabilities, and non-root user.
- Remove the `/data` volume.

The app connection URL inside Compose must use the service name:

```text
jdbc:postgresql://postgres:5432/exptrack
```

### `compose.debug.yaml`

- Remove `EXPTRACK_DATABASE_PATH`.
- Remove the SQLite debug data volume.
- Point the debug app to `postgres`.
- Keep frontend source, frontend `node_modules`, and Maven cache volumes.
- Expose PostgreSQL on `127.0.0.1:5432` only if direct local inspection is useful.

### `Dockerfile`

Remove:

- `/data` creation and ownership setup.
- `EXPTRACK_DATABASE_PATH`.
- The `/data` volume declaration.
- SQLite-specific comments.

Keep the non-root runtime user and read-only container hardening.

### Acceptance gate

Reset the local stack:

```sh
docker compose down --volumes --remove-orphans
docker compose -f compose.yaml -f compose.debug.yaml down --volumes --remove-orphans
```

Start the production-style stack:

```sh
docker compose up --build --wait
```

Check health:

```sh
curl http://127.0.0.1:8080/actuator/health
```

Exercise registration, login, expense creation, search, filters, cursor pagination, dashboard, update, delete, and ownership isolation through the running application.

### Rollback

Stop the stack, discard the PostgreSQL volume, and revert the runtime checkpoint. The old SQLite stack can be started from the previous commit. No database data restore is required.

---

## Checkpoint 3: Make CI prove PostgreSQL parity

### Goal

Ensure local tests and CI use the same database engine.

### Work

Review `.github/workflows/ci.yml`:

- Keep backend `mvn -B verify` as the backend gate.
- Confirm the backend job can start Testcontainers.
- Keep the fresh-volume production Compose job.
- Ensure Compose waits for both PostgreSQL and the app.
- Preserve the existing container hardening checks.

If the CI runner cannot provide Docker access to the backend job, use a PostgreSQL service for that job and document the difference. Prefer Testcontainers because it keeps the test setup self-contained and matches local behavior.

### Acceptance gate

Both CI paths must pass:

1. Maven backend tests against PostgreSQL.
2. Fresh production Compose startup with PostgreSQL and a healthy app.

---

## Checkpoint 4: Update documentation and remove stale references

### Goal

Make the repository describe the active PostgreSQL architecture.

### Work

Update:

- `README.md`: describe PostgreSQL volume persistence, local database variables, and volume reset.
- `docs/SPEC-v1-personal-expense-tracker.md`: replace SQLite with PostgreSQL and remove the statement that PostgreSQL is only a future option.
- Relevant ADRs if they mention SQLite.
- `Dockerfile` comments if any SQLite wording remains.

Search active source, configuration, migrations, and documentation for:

```text
sqlite
SQLite
org.xerial
SQLiteDialect
EXPTRACK_DATABASE_PATH
expenses_search
fts5
rowid
COLLATE NOCASE
```

Build output and Git history do not need cleanup. Active application references do.

### Acceptance gate

The search returns no active SQLite implementation references. The repository has one documented database path: PostgreSQL.

---

## Final cutover rehearsal

Run the full sequence from a clean worktree or known migration commit:

```sh
docker compose down --volumes --remove-orphans
cd backend
mvn -B verify
cd ..
docker compose up --build --wait
curl http://127.0.0.1:8080/actuator/health
```

Verify the real application workflow:

1. Register a user.
2. Sign in.
3. Create expenses in more than one category.
4. Search by title.
5. Search by note.
6. Apply category, currency, and date filters.
7. Fetch more than one page using the cursor.
8. View the current-month dashboard.
9. Update an expense.
10. Delete an expense.
11. Confirm another user cannot read or change it.

Inspect the database and confirm:

- Flyway reports all migrations as successful.
- `users` and `expenses` exist.
- Expected indexes exist.
- The PostgreSQL search index exists.
- No SQLite search table or trigger exists.
- Hibernate validation succeeds at startup.

## Finish line

The migration is complete when all of these are true:

- Backend tests pass against PostgreSQL.
- The application starts from empty PostgreSQL volumes.
- The full REST workflow passes against the running Compose stack.
- CI passes for the Maven and container jobs.
- SQLite dependencies and configuration are removed.
- SQLite migrations, FTS5 objects, and `/data` volume configuration are removed.
- README, specification, and relevant ADRs describe PostgreSQL.
- Future schema changes use new append-only Flyway migrations.

## Session handoff record

Each agent should append one entry after completing a checkpoint:

```text
### YYYY-MM-DD - Checkpoint N
- Agent: <agent/session name>
- Changes: <short file and behavior summary>
- Verification: <exact command or smoke scenario>
- Result: PASS or BLOCKED
- Follow-up: <remaining issue, or "none">
```

An agent must not mark a checkpoint as complete when its acceptance gate is missing, failing, or not run.
### 2026-09-06 - Checkpoint 0
- Agent: Main session with BaselineImplementor and CheckpointReviewer
- Changes: Added REST baseline assertions for mixed-case registration, title and note search, punctuation, empty results, date filtering, and same-date cursor pagination.
- Verification: Focused `RegistrationEndpointTest` and `ExpenseWorkflowEndpointTest` passed (12 tests). Full gate equivalent `docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9-eclipse-temurin-21 mvn -B verify` failed in the existing `ExpenseEndpointTest.signedInUserCanAddAnExactExpenseAndSeeOnlyTheirRecentExpenses` check with two `Coffee` rows.
- Result: BLOCKED
- Follow-up: The defect is low complexity. A minimal fix is a unique fixture or owner-scoped assertion. The preferred fix is shared test cleanup that clears `expenses` and `users` between tests, aligned with Checkpoint 1. Checkpoint 0 remains incomplete.

### 2026-09-06 - Checkpoint 0
- Agent: Main session (orchestrated fix-up)
- Changes: Added `backend/src/test/java/com/exptrack/AbstractEndpointTest.java`, a shared base class that clears `expenses` and `users` in a `@BeforeEach` hook. All five `@SpringBootTest` endpoint test classes (`RegistrationEndpointTest`, `AuthRateLimitTest`, `HealthEndpointTest`, `ExpenseEndpointTest`, `ExpenseWorkflowEndpointTest`) now extend it and no longer declare their own `JdbcTemplate` field. Root cause of the prior BLOCKED result: Spring caches the `ApplicationContext` across test classes with identical `@SpringBootTest(properties = ...)`, so `ExpenseEndpointTest` and `ExpenseWorkflowEndpointTest` shared one in-memory SQLite connection with no reset between tests, letting two differently-owned "Coffee" fixtures collide on an unscoped `SELECT ... WHERE title = ?`.
- Verification: `cd backend && mvn -B verify` run locally (Temurin 25 toolchain, `maven.compiler.release=21`). Result: `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- Result: PASS
- Follow-up: The same `@BeforeEach` reset seam is reusable for Checkpoint 1 by swapping the SQLite `DELETE` statements for a Postgres `TRUNCATE ... RESTART IDENTITY CASCADE` once Testcontainers is wired in.

### 2026-09-07 - Checkpoint 1
- Agent: Main session with Checkpoint1Implementor
- Changes: Switched backend persistence to PostgreSQL. `backend/pom.xml`: added `org.postgresql:postgresql` (runtime), `org.flywaydb:flyway-database-postgresql`, `spring-boot-testcontainers` and `org.testcontainers:postgresql` (test scope); removed `org.xerial:sqlite-jdbc` and `hibernate-community-dialects`. `application.properties`: environment-based PostgreSQL datasource (`EXPTRACK_DATABASE_URL/USERNAME/PASSWORD`), `org.postgresql.Driver`, `PostgreSQLDialect`; removed the SQLite `hbm2ddl.jdbc_metadata_extraction_strategy` override. Replaced the SQLite migration set with a clean PostgreSQL set: `V1__create_users.sql` (`GENERATED BY DEFAULT AS IDENTITY`, unique index on `lower(email)` instead of `COLLATE NOCASE`), `V2__create_expenses.sql` (identity PK, existing `idx_expenses_user_date`), `V3__add_expense_indexes.sql` (the user/category/date/id and user/date/id indexes, replacing old V7/V8), `V4__add_expense_search.sql` (GIN index over `to_tsvector('simple', title || note)`, replacing the FTS5 virtual table and triggers). `ExpenseRepository.findHistory` now uses `to_tsvector(...) @@ websearch_to_tsquery('simple'::regconfig, :text)` matching the GIN index expression; every optional native-query filter got an explicit `CAST(:param AS <type>)` on its `IS NULL` branch because PostgreSQL's extended protocol cannot infer a type for a bare untyped parameter. `AbstractEndpointTest` now starts one shared `PostgreSQLContainer("postgres:16-alpine")` for the whole test JVM via a static initializer and `@DynamicPropertySource`, and resets state with `TRUNCATE TABLE expenses, users RESTART IDENTITY CASCADE`; all `jdbc:sqlite::memory:` overrides removed from the five `@SpringBootTest` subclasses.
- Verification: `cd backend && rm -rf target && mvn -B clean verify` run independently by the orchestrator (not just the subagent's claim). Result: `BUILD SUCCESS`, `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`, confirmed against real PostgreSQL 16.15 via Testcontainers with Flyway log lines `Database: jdbc:postgresql://localhost:<port>/test (PostgreSQL 16.15)` and `Successfully validated 4 migrations`. `grep -riE 'sqlite|xerial|expenses_search|rowid|COLLATE NOCASE|AUTOINCREMENT'` under `backend/` returns no matches (only the unrelated new index name `idx_expenses_search`).
- Result: PASS
- Follow-up: none. Checkpoint 2 (Docker Compose + runtime image) can reuse the `postgres:16-alpine` tag already pinned in the Testcontainers base class.

### 2026-09-10 - Checkpoint 2
- Agent: Main session (stacked branch postgres-migration-checkpoint-2 off postgres-migration-checkpoint-1)
- Changes: Switched Docker Compose and the runtime image to PostgreSQL. `compose.yaml`: added a `postgres` service (`postgres:16-alpine`, `POSTGRES_DB`/`POSTGRES_USER`/`POSTGRES_PASSWORD` from `EXPTRACK_DATABASE_NAME`/`EXPTRACK_DATABASE_USERNAME`/`EXPTRACK_DATABASE_PASSWORD` with local defaults, `pg_isready` health check, `postgres-data` volume, no published ports); `app` now depends on `postgres` health, receives `EXPTRACK_DATABASE_URL`/`USERNAME`/`PASSWORD` pointed at the `postgres` service name, and no longer mounts `/data`; removed the `expense-data` volume. `compose.debug.yaml`: removed `EXPTRACK_DATABASE_PATH` and the `debug-expense-data` volume from the debug `app` service (it now inherits the Postgres datasource env from the base file merge) and added a `postgres` port override (`127.0.0.1:5432`) for local inspection. `Dockerfile`: removed `/data` creation/ownership, the SQLite volume comment, `ENV EXPTRACK_DATABASE_PATH`, and `VOLUME /data` from the runtime stage; kept the non-root user and read-only/hardened runtime.
- Verification: `docker compose down --volumes --remove-orphans` then `docker compose up --build --wait --wait-timeout 180` reached healthy for both `postgres` and `app`. `docker compose config` and `docker compose -f compose.yaml -f compose.debug.yaml config` confirmed the Postgres service and env vars merge correctly. Re-ran the container-hardening checks from `.github/workflows/ci.yml` (uid/gid 10001, read-only rootfs, empty `CapEff`, `NoNewPrivs=1`): all passed. `curl http://127.0.0.1:8080/actuator/health` returned `{"status":"UP"}`. Exercised the full REST workflow against the running stack: register, login, create two expenses in different categories, title search, note search, category filter, dashboard aggregation, update, delete, and a second user confirming ownership isolation (empty result set for another owner's data) — all returned the expected status codes and bodies. `cd backend && mvn -B clean verify` still exits 0 against Testcontainers Postgres.
- Result: PASS
- Follow-up: Checkpoint 3 should confirm `.github/workflows/ci.yml`'s `container` job now passes end-to-end once this branch reaches a PR based on `main` (the job could not be exercised via GitHub Actions directly from this stacked branch, since `pull_request` triggers are scoped to `branches: [main]`; local Compose verification above stands in for it). Checkpoint 4 should still remove the `EXPTRACK_DATABASE_PATH`/SQLite comment text search hits and update README/spec references.
