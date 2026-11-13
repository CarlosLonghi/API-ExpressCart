# ExecPlan: Phase 3, organization (config, packages, names)

**Date**: 2026-11-12
**Status**: In Progress
**Covers**: A3, A4 (`docs/REFACTORING.md`)

## Goal

Infrastructure annotations live in `@Configuration` classes, the Redis cache stores readable JSON with a TTL per cache, and packages/class names say what they contain. No HTTP contract change.

## Big Picture & Context

**Current state**: `@EnableCaching` sat on `ProductService` and `@EnableFeignClients` on the main class; the cache used JDK serialization with one global TTL; `exception.handler` held exceptions while the real handler was `config/ControllerAdvice`; `entity.Product` (a cart line) shared its name with the catalog product.
**Desired state**: `config/CacheConfig`, `config/FeignConfig`; exceptions directly under `exception/`; handler named for what it is; cart line called `CartItem`.
**Why now**: phases 1-2 settled behavior and domain; moving files earlier would have made those diffs unreadable.
**Affected modules**: `config/`, `exception/`, `entity/`, `service/`, tests, docs.

Delivered as three stacked PRs:

| PR | Branch | Items |
|---|---|---|
| 1 | `refactor/cache-config` | A4 |
| 2 | `refactor/package-structure` | A3 (exceptions, handler, Feign config) |
| 3 | `refactor/rename-cart-item` | `entity.Product` → `CartItem` |

## Progress Checklist

- [x] PR 1: `CacheConfig` with `@EnableCaching`, JSON values, TTL per cache
- [x] PR 2: exceptions in `exception/`, `ApplicationControllerAdvice`, `FeignConfig`
- [ ] PR 3: `CartItem`
- [ ] Docs updated per PR

## Surprises & Discoveries

**2026-11-12**: After moving/renaming classes, `./mvnw verify` failed with `NoClassDefFoundError` for the old package: stale `.class` files in `target/` (the old `ControllerAdvice.class` was still picked up by component scan). `./mvnw clean verify` fixes it; always clean after a move.

**2026-11-12**: `GenericJackson2JsonRedisSerializer` (the suggestion in A4) is a poor fit for records: its default typing only adds type information to non-final classes, and records are final, so values come back as `LinkedHashMap` and fail with `ClassCastException`. Used one typed `Jackson2JsonRedisSerializer` per cache instead.

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-11-12 | One typed `Jackson2JsonRedisSerializer` per cache (`ProductResponse`, `List<ProductResponse>`), registered through `RedisCacheManagerBuilderCustomizer` | Avoids the record/default-typing problem above and keeps class names out of Redis, so moving or renaming the DTO does not invalidate the cache. The customizer keeps Boot's auto-configured `RedisCacheManager` (connection, statistics) instead of replacing it. |
| 2026-11-12 | Caches renamed to `catalog-product-v2` / `catalog-products-v2` | The stored format changed from JDK bytes to JSON; entries written by the previous version would fail to parse. A version suffix makes the "rename on format change" rule explicit for next time. |
| 2026-11-12 | TTLs moved to `expresscart.cache.product-ttl` / `product-list-ttl`; `spring.cache.redis.time-to-live` removed | Each cache config is now built explicitly, so the Boot property would no longer apply to them; leaving it would be a setting that does nothing. Both default to the previous 60 s. |
| 2026-11-12 | Uses the application's `ObjectMapper` | Same number/date handling as the HTTP responses, so a cached product serializes to the client exactly like a fresh one. |
| 2026-11-12 | `disableCachingNullValues()` | The catalog methods never return null (absence is an exception), and the typed JSON serializer has no representation for Spring's `NullValue` marker. |
| 2026-11-12 | Handler renamed to `config/ApplicationControllerAdvice`, and `controller/request` + `controller/response` kept, instead of the diagnosis' `exception/GlobalExceptionHandler` and `dto/*` | Same names and locations as the API-Eletro-Longhi project, so both codebases read the same way. The diagnosis only said the old names were confusing, not that these specific targets were required. |
| 2026-11-12 | `@EnableFeignClients(basePackageClasses = PlatziStoreClient.class)` in `config/FeignConfig` | Off the main class, `@WebMvcTest` slices stop trying to build the Feign client, so the `@MockitoBean PlatziStoreClient` workaround from Phase 0 is gone. `basePackageClasses` is needed because the annotation's default scan root would now be `config`. |

## Context & Orientation

**Key files**: `config/CacheConfig.java`, `service/ProductService.java`, `application.yml`
**Related docs**: `.claude/ARCHITECTURE.md` invariant I8; Phase 2 ExecPlan (first cache rename)

## Concrete Steps

```bash
./mvnw verify
```

## Validation & Acceptance

- [x] PR 1: values in Redis are JSON (asserted by reading the key with `StringRedisTemplate`), each with a TTL; cache hits return the same payload as a miss
- [ ] PR 2 and PR 3: pure moves/renames, every test green without assertion changes

## Outcomes & Retrospective

**What shipped**: (fill in when the three PRs are merged)
