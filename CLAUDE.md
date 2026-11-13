# Express Cart API

Java 21 + Spring Boot 3.5 REST API that manages shopping carts. Products are **not** stored locally: they come from the public Platzi Fake Store API (OpenFeign) and are cached in Redis; carts are persisted in MongoDB. Package root: `br.com.expresscart`.

The project is **being refactored** in phases. The diagnosis (bugs B1–B8, design debts A1–A6) and the phase plan live in `docs/REFACTORING.md`; the human-facing description of the current behavior is `docs/ARCHITECTURE.md` (both in Portuguese). Before touching code, check which phase the work belongs to and whether an ExecPlan exists in `.claude/execplans/`.

## Build / run / test

- Infra: `docker compose up -d` starts MongoDB (`mongo:4`, 27017) and Redis (`redis`, 6379). The app itself is not in the compose file.
- Run: `./mvnw spring-boot:run` (or the IDE). No profiles; every setting is hard-coded in `src/main/resources/application.yml`.
- Compile only: `./mvnw -q -DskipTests package`.
- Tests: `./mvnw test`. Unit tests need nothing; integration tests (`*IntegrationTest`) use **Testcontainers** (Mongo + Redis) and **WireMock** (fake Platzi API), so they need a running Docker daemon. They never call the real Platzi API.
- Coverage report: `./mvnw verify` → `target/site/jacoco/index.html` (no minimum enforced yet; the Phase 0 suite sits at ~99% instructions / 100% branches).
- Swagger UI: `/swagger-ui/index.html`; OpenAPI spec: `/v3/api-docs`.

## Commit convention

- **Semantic commits** (Conventional Commits): `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`, `build:`, `ci:`. Imperative, concise subject. Scopes as already used in history: `refactor(cart): ...`, `feat(dto): ...`.
- **Split the work into smaller, cohesive commits**: never one giant commit with the whole feature, but don't commit file by file either.
- **Do not add AI credits** to commits: no `Co-Authored-By: Claude`, `Generated with Claude Code`, `Claude-Session:` or equivalent trailers.
- The body (when useful) explains the *why*, not a restatement of the diff.
- Work happens on branches (`refactor/...`, `fix/...`, `test/...`, `docs/...`) merged into `main` through PRs.

## Layered architecture

`controller` (record DTOs) → `service` (use-case orchestration) → `repository` (Spring Data MongoDB) → `entity` (Mongo document that owns the cart state rules, + enums). `ProductService` → `client/PlatziStoreClient` (Feign) for the catalog.

- Controllers implement OpenAPI contract interfaces in `controller/api/docs/*Api.java`. When an endpoint contract changes, update the interface (signature + `@ApiResponse` docs) together with the controller.
- Controllers only return DTOs from `controller/response` (`CartResponse` + `CartItemResponse`, `PaymentResponse`, `ProductResponse` + `CategoryResponse`). Never return an entity or a `client/response/Platzi*` record. The `client` package is used by `ProductService` only, which maps Platzi's model to `ProductResponse`. The `Status` / `PaymentMethod` enums are shared with the entity on purpose.
- Business exceptions live directly in `exception/` (`BusinessException`, `DataNotFoundException`, `ExternalServiceException`); error body records in `exception/response`. Global error translation is `config/ApplicationControllerAdvice`: `BusinessException` → 400, `DataNotFoundException` → 404, `ExternalServiceException` → 502, `MethodArgumentNotValidException` → 400 with a **list** of `{field, message}`, unreadable body / wrong type → 400, unsupported media type → 415, anything else → 500 `{timestamp, status, message}` (logged, fixed message, no internal details in the response).
- `ProductService` is the **only** place that translates Feign errors: Platzi 400 (its answer for unknown ids) → `DataNotFoundException("Produto não encontrado: id=X")` → 404; any other `FeignException` (5xx, timeout, connection) → `ExternalServiceException` → 502. There is no custom Feign `ErrorDecoder`; don't add one that throws domain exceptions, or the service's `catch` blocks become dead code again (old bug B1).
- Cart state rules live in `entity/Cart`, which has **no setters and no builder**: it only changes through `Cart.open(...)`, `replaceItems(...)`, `clear()` and `pay(...)`. Each one rejects a `SOLD` cart and keeps the total in sync. `CartService` only loads, calls the entity and saves. Don't add setters back; add a method that states the business operation.

## Domain model (summary)

- **Cart** (`carts` collection): `id`, `clientId`, `products` (embedded, unmodifiable `Product` list), `totalPrice` (derived inside the entity), `status` (`OPEN` → `SOLD`), `paymentMethod` (`PIX`/`DEBIT`/`CREDIT`, null until paid).
- **Product** (embedded, immutable): `id`, `title`, `price`, `quantity`, `subtotal()`. Title and price are a **snapshot** copied from Platzi when the item is added.
- One `OPEN` cart per `clientId` (checked in the service, no DB index, so there is a race, B7).
- `PUT /cart/{id}` **replaces** the item list; it doesn't merge. Its body is `CartUpdateRequest` (items only), not `CartRequest`.

Details: `.claude/GLOSSARY.md`, `.claude/ARCHITECTURE.md`, `docs/ARCHITECTURE.md`.

## Testing conventions

- Tests mirror the main package layout under `src/test/java/br/com/expresscart/{entity,service,controller,...}`.
- Unit tests: JUnit 5 + Mockito + AssertJ. Controller slices use `@WebMvcTest` + `@MockitoBean`.
- Integration tests live in `integration/` and extend `support/AbstractIntegrationTest` (`@SpringBootTest` + MockMvc; Testcontainers Mongo + Redis and a WireMock server on a random port, wired through `@DynamicPropertySource`). Containers start once per JVM and are shared; Mongo, the caches and the WireMock stubs are reset before each test.
- Shared builders/fixtures: `support/TestFixtures` (`cart(Status, items...)` returns a cart as it would come from Mongo: id set by reflection, paid with PIX when `SOLD`); Platzi stubs: `support/PlatziStubs`.
- Framework switches (`@EnableCaching`, `@EnableFeignClients`) live in `config/*Config` classes, never on the main class or on a service: `@WebMvcTest` slices don't load `@Configuration` classes, so they start without Redis or Feign and only need `@MockitoBean` for the service under the controller.
- **Phase 0 tests are characterization tests:** they pin the *current* behavior, bugs included. Tests that pin a known bug are named/commented with the bug id (e.g. `// B3: ...`). When a refactor fixes that bug, update the test deliberately in the same commit, and never just to make it pass.

## Deeper-dive references

- `.claude/ARCHITECTURE.md`: module map, invariants, data flow, "adding a feature" checklist.
- `.claude/GLOSSARY.md`: domain terms and quick file lookups.
- `.claude/PLANS.md` + `.claude/execplans/TEMPLATE.md`: write an ExecPlan for each refactor phase or any change crossing 3+ modules.
- `docs/REFACTORING.md`: bug/debt catalog. Mark items as done there when a PR fixes them.

## Gotchas

- `application.yml` sets `spring.data.redis.password: password`, but the compose Redis has no password (B6). It works anyway: Lettuce authenticates with `HELLO ... AUTH default <pwd>`, which a password-less Redis accepts. The integration tests run with exactly this combination on purpose.
- `testcontainers.version` is pinned to 1.21.4 in `pom.xml`: the version managed by Boot 3.5.3 (1.21.2) fails against Docker 29+ ("client version 1.32 is too old").
- After moving or renaming a class, run `./mvnw clean verify`: stale `.class` files in `target/` are still component-scanned and fail with `NoClassDefFoundError`.
- Several source files are stored with **CRLF** line endings (no `* text=auto` in `.gitattributes`). Keep each file's existing endings when editing with scripts, or the diff shows the whole file as rewritten; check with `git diff --stat` before committing.
- The Redis cache is configured in `config/CacheConfig` (`@EnableCaching` lives there): values are stored as **JSON** through one typed `Jackson2JsonRedisSerializer` per cache, each with its own TTL (`expresscart.cache.*`). Don't switch to `GenericJackson2JsonRedisSerializer`: records are final, so its default typing stores no type info and values come back as `LinkedHashMap`. If you change the type or JSON shape of a cached value, **bump the version suffix** of the cache name (`CacheConfig.PRODUCT_CACHE` / `PRODUCT_LIST_CACHE`), so entries written by the previous version are never read; they expire by TTL.
- A new cache must be registered in `CacheConfig` with its serializer; an unregistered name would fall back to Boot's default (JDK serialization, no TTL).
- `spring.mvc.throw-exception-if-no-handler-found` + `spring.web.resources.add-mappings: false` make unknown routes return JSON 404 through `ApplicationControllerAdvice`. Keep both if you touch web config.
