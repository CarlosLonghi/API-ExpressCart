# ExecPlan: Phase 0, characterization tests

**Date**: 2026-11-12
**Status**: Complete
**Covers**: `docs/REFACTORING.md` §5 (tests) and Phase 0 of §6; verifies B6

## Goal

Every behavior the API has today (rules, status codes, error bodies, cache, known bugs) is pinned by an automated test, so the next refactor phases can change code and see exactly what changed.

## Big Picture & Context

**Current state**: the only test was `contextLoads`, which also needed Mongo/Redis from docker-compose to be meaningful.
**Desired state**: unit + slice + integration suite that runs with `./mvnw verify` and only needs a Docker daemon. No production code changes.
**Why now**: phases 1-6 change error handling, the domain model and the HTTP contract; without tests regressions would go unnoticed.
**Affected modules**: `pom.xml` (test deps + JaCoCo), `src/test/**`, docs.

## Progress Checklist

- [x] Test dependencies: Testcontainers (junit-jupiter, mongodb), WireMock standalone, JaCoCo report
- [x] `support/TestFixtures`, `support/AbstractIntegrationTest`, `support/PlatziStubs`
- [x] `entity/CartTest`
- [x] `service/CartServiceTest`, `service/ProductServiceTest`
- [x] `config/CustomErrorDecoderTest`
- [x] `controller/CartControllerTest`, `controller/ProductControllerTest` (`@WebMvcTest`)
- [x] `integration/CartIntegrationTest`, `integration/ProductIntegrationTest`
- [x] `ExpressCartApplicationTests` now runs on the containers
- [x] Docs updated (`docs/REFACTORING.md`, `docs/ARCHITECTURE.md`, `CLAUDE.md`)

## Surprises & Discoveries

**2026-11-12**: `./mvnw` fails with "Permission denied": the wrapper is committed as `100644`. Ran everything with `bash ./mvnw`. Fixing the mode is a separate one-line change.

**2026-11-12**: Testcontainers 1.21.2 (managed by Boot 3.5.3) cannot talk to Docker 29: `client version 1.32 is too old. Minimum supported API version is 1.40`. Pinned `testcontainers.version` to 1.21.4, same as the Eletro Longhi API.

**2026-11-12**: B6 is **not** a functional bug. `redis-cli AUTH password` is rejected by a password-less Redis, but Lettuce authenticates via `HELLO 3 AUTH default password`, which is accepted. The integration tests keep `spring.data.redis.password: password` from `application.yml` against a password-less container, and the cache tests pass (WireMock sees a single upstream call). Downgraded B6 to a config inconsistency.

**2026-11-12**: `@WebMvcTest` slices need `@MockitoBean PlatziStoreClient` because `@EnableFeignClients` is on `ExpressCartApplication`. Moving it to a `@Configuration` class (Phase 3, A4) removes that need.

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-11-12 | Tests pin bugs as they are (e.g. malformed JSON → 500), tagged with the bug id in a comment | That is what "characterization" means: a later fix must flip the assertion on purpose, in the same commit, which makes the behavior change visible in review. |
| 2026-11-12 | WireMock instead of mocking `PlatziStoreClient` in integration tests | Mocking the Feign interface bypasses `CustomErrorDecoder`, and B1/B2 are precisely about how the decoder and `ProductService` interact. |
| 2026-11-12 | Singleton containers + `@DynamicPropertySource` instead of `@Testcontainers`/`@ServiceConnection` | `@Container` static fields are stopped per test class, which breaks Spring's cached context across classes. `@ServiceConnection` would also override the Redis password and hide B6. |
| 2026-11-12 | Same images as `docker-compose.yml` (`mongo:4`, `redis:latest`) | Characterize against what actually runs. Bump both together in Phase 6. |
| 2026-11-12 | JaCoCo report only, no `check` threshold yet | Coverage is ~99%, but enforcing a minimum is a team rule to decide separately; adding it later is one execution block. |
| 2026-11-12 | Kept unit tests for the unreachable `Optional.empty()` paths (B1) | They document what the code *would* do; Phase 1 decides which strategy stays and deletes the other tests. |

## Context & Orientation

**Assumptions**: Docker daemon available; no network access to the real Platzi API needed.
**Key files**: `src/test/java/br/com/expresscart/support/*`, `pom.xml`
**Related docs**: `.claude/ARCHITECTURE.md` (invariants I1-I8 are each covered by a test), `docs/REFACTORING.md`

## Concrete Steps

```bash
bash ./mvnw verify
```
**Expected output**:
```
Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
Coverage report: `target/site/jacoco/index.html`.

## Validation & Acceptance

**Acceptance criteria**:
- [x] `verify` green with no docker-compose services running
- [x] No file under `src/main` changed
- [x] Every invariant I1-I8 and every bug B1-B4, B8 has at least one test naming it

**Bug → test map** (flip these when fixing):

| Bug | Tests pinning it |
|---|---|
| B1 | `CartIntegrationTest.unknownProductReturns404WithDecoderMessage`, `ProductIntegrationTest.unknownProductReturns404FromDecoder`, `ProductServiceTest.getProductById_returnsEmptyOnFeignBadRequest`, `ProductControllerTest.getById_emptyOptionalReturns404WithoutBody`, `CartServiceTest.CreateCart.throwsNotFoundWhenProductServiceReturnsEmpty` |
| B2 | `CustomErrorDecoderTest.anyOtherStatus_becomesCheckedGenericException`, `CartIntegrationTest.platziServerErrorReturns500` |
| B3 | `CartControllerTest.update_skipsBeanValidation`, `pay_nullPaymentMethodIsNotValidated`, `CartServiceTest.PayCart.acceptsNullPaymentMethod` |
| B4 | `CartControllerTest.create_malformedJsonReturns500`, `pay_unknownPaymentMethodReturns500`, `ProductControllerTest.getById_nonNumericIdReturns500` |
| B8 | `CartTest.calculateTotalPrice_throwsNpeWhenProductsIsNull`, `CartControllerTest.create_clientIdMinMessageSaysZero`, `CartServiceTest.CreateCart.keepsDuplicatedProductIdsAsSeparateLines`, `allowsEmptyProductList` |

## Outcomes & Retrospective

**What shipped**: 71 tests (58 unit/slice, 13 integration). JaCoCo: 99.3% instructions, 100% branches (only `main()` uncovered).
**What we learned**: B6 downgraded; two environment gotchas (mvnw mode, Testcontainers × Docker 29) recorded in `CLAUDE.md`.
**What's left**: B5 (no logging) and B7 (race on open cart) are not pinned: B5 has no observable output and B7 needs a concurrency test that belongs with its fix in Phase 5.
