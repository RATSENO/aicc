# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project purpose

AICC Interface Server — a Spring Boot API layer that sits between an existing rental back-office system and an
AICC (contact center) solution. It receives requests from AICC, validates them, optionally calls back-office APIs
(via OpenFeign) and/or queries the DB (via MyBatis), and returns JSON. See `docs/PROJECT.md` for the full original
spec (in Korean) and `docs/aicc-gateway-design.html` for background/design notes — read both before making
architectural changes.

## Tech stack (do not upgrade without explicit user approval)

Java 11 / Spring Boot 2.7.18 / WAR packaging / MyBatis 2.1.3 / OpenFeign (Spring Cloud 2021.0.8) /
MariaDB JDBC 3.0.8 / springdoc-openapi-ui 1.8.0 / Jasypt 3.0.3 / Lombok.

Hard constraints (from `docs/PROJECT.md` §18): do **not** move to Spring Boot 3.x, Java 17+, or Jakarta EE, and do
not swap `springdoc-openapi-ui` for `springdoc-openapi-starter-webmvc-ui` — those are Boot 3.x-only. Do not add new
dependencies without explaining why, confirming compatibility with Boot 2.7.18/Java 11, and asking the user first.

## Commands

```bash
mvn clean package                # build (runs tests)
mvn clean package -DskipTests    # build, skip tests
mvn spring-boot:run                                    # run (defaults to `local` profile)
mvn spring-boot:run -Dspring-boot.run.profiles=local    # run, explicit profile
mvn test                                                # run all tests
mvn test -Dtest=AiccApplicationTests                    # run a single test class
mvn test -Dtest=AiccApplicationTests#contextLoads       # run a single test method
```

Build output: `target/aicc-0.0.1-SNAPSHOT.war`.

If `mvn`/`JAVA_HOME` aren't on PATH, an IntelliJ install can supply them temporarily — see
`docs/guide/run-guide.md` for exact bundled paths.

## Running locally

The `local` profile (default, `application-local.yml`) uses an **H2 in-memory DB** — no MariaDB install needed.
`schema-h2.sql` and `data-h2.sql` (classpath root) run automatically on startup and seed sample data. `dev`/`prod`
profiles use real MariaDB, configured entirely from env vars: `DB_USERNAME`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT`,
`DB_NAME`.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 console (local only): http://localhost:8080/h2-console — JDBC URL `jdbc:h2:mem:aicc`, user `sa`, empty password

Note: `spring.sql.init.encoding: UTF-8` is required in `application-local.yml` — without it, Korean seed data gets
mangled on Windows (non-UTF-8 platform default encoding).

## Architecture

Layered, one-directional flow — Controllers must not contain business logic:

```
HTTP Request → Controller → Service → { MyBatis Mapper → DB, Feign Client → Backend API } → Service → Response DTO → JSON
```

Package layout under `com.onestar.aicc`:

| Package | Role |
| --- | --- |
| `config` | Spring config (`OpenApiConfig`, future `FeignConfig`/`DatabaseConfig`) |
| `aop` | Cross-cutting AOP aspects — `TransactionLoggingAspect` logs `@Transactional` boundary state (new vs. joined transaction, commit/rollback) |
| `commons` | Shared utilities — currently `commons/response`: `ApiResponse<T>` (envelope), `PageResponse<T>` (pagination), `ErrorResponse` |
| `controller` | Receives HTTP requests, delegates to `service`, returns response — no business logic |
| `service` | Business logic; orchestrates `mapper` and `client` calls |
| `mapper` | MyBatis `@Mapper` interfaces (SQL lives in `resources/mapper/**/*.xml`, not in Java) |
| `domain` | DB entities / domain objects (MyBatis `type-aliases-package`) |
| `dto` | API request/response objects |
| `client` | OpenFeign clients for back-office/external API calls |
| `exception` | Exception handling (a global `@RestControllerAdvice` is expected here eventually) |
| `sample` | Self-contained reference implementation only — see below. Not real business logic. |

Real business packages (`controller`, `service`, `mapper`, `domain`, `dto`, `client`, top-level `exception`) don't
exist yet — they get created as real business requirements arrive. Don't invent business logic ahead of that; per
`docs/PROJECT.md` §14, initial scope is intentionally limited to infra plumbing + a health check.

MyBatis config: mapper XML files live under `resources/mapper/**/*.xml`; `map-underscore-to-camel-case: true` is
on, so snake_case DB columns map to camelCase Java fields automatically. Type aliases resolve against
`com.onestar.aicc.domain`.

### The `sample` package

`sample/` (controller/service/mapper/dto/domain/exception, plus `resources/mapper/sample/SampleBookMapper.xml`,
`schema-h2.sql`, `data-h2.sql`) is a **learning-only reference implementation** — a "Book" CRUD API unrelated to
real business logic. It exists purely to demonstrate this project's conventions (layering, MyBatis mapper style,
`ApiResponse`/`PageResponse` usage, exception handling, Swagger annotations end-to-end). When building real
features, copy its patterns into the real packages rather than extending `sample` itself. Full annotation-by-
annotation walkthrough: `docs/guide/swagger-guide.md`.

Its `SampleExceptionHandler` is deliberately scoped with
`@RestControllerAdvice(basePackages = "com.onestar.aicc.sample")` so it won't collide with the future global
exception handler in the top-level `exception` package — real code should use one global handler, not per-package
ones.

### Gotchas worth knowing before touching Swagger/response code

- `io.swagger.v3.oas.annotations.responses.ApiResponse` (the annotation) and this project's
  `com.onestar.aicc.commons.response.ApiResponse<T>` (the response envelope class) share a name. Convention: import
  the Swagger annotation by short name; reference the envelope class by FQN.
- Registering any `GroupedOpenApi` bean makes springdoc show *only* the registered groups in the Swagger UI
  dropdown — it stops auto-adding an "all APIs" group. `OpenApiConfig` therefore keeps an explicit
  `pathsToMatch("/**")` group named `all` alongside narrower ones; keep that when adding new groups.
- `HttpMessageNotReadableException` (malformed JSON body) is not caught by `@Valid`/`MethodArgumentNotValidException`
  handling — it falls through to a generic 500 unless handled separately as its own case.

## Config / secrets

Jasypt encrypts sensitive config values; the decryption password comes from env var `JASYPT_ENCRYPTOR_PASSWORD`.
DB and other environment-specific values are injected via env vars per profile, never hardcoded — see `dev`/`prod`
`application-*.yml`.
