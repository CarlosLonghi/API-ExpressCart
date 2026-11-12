# ExecPlan: Phase 2, domain behavior and own DTOs

**Date**: 2026-11-12
**Status**: Complete (pending merge of the two PRs)
**Covers**: A1, A2, B8 leftovers (`docs/REFACTORING.md`)

## Goal

The cart rules live in the `Cart` entity instead of being repeated in `CartService`, and the HTTP responses are built from DTOs owned by this API, so neither the Mongo entity nor the Platzi model leaks to clients.

## Big Picture & Context

**Current state**: `Cart` was a bag of setters; `CartService` repeated the "SOLD is terminal" check in three methods and had to remember to recalculate the total. `CartResponse` embedded `entity.Product` and `ProductController` returned the Platzi DTO.
**Desired state**: `Cart` can only change through `open` / `replaceItems` / `clear` / `pay`; the service orchestrates. Responses use `CartItemResponse` / `ProductResponse`.
**Why now**: Phase 1 removed the behavior bugs; with the test suite pinning the HTTP contract this is a pure internal refactor.
**Affected modules**: `entity/`, `service/`, `controller/response`, `controller/ProductController`, `controller/api/docs`, tests, docs.

Delivered as two stacked PRs:

| PR | Branch | Items |
|---|---|---|
| 1 | `refactor/cart-domain-behavior` | A1, B8 (null item list) |
| 2 | `refactor/response-dtos` | A2 |

Pending a product decision (not in these PRs): duplicated product ids in a request, and whether a cart can be created empty (B8).

## Progress Checklist

- [x] PR 1: `Cart.open`, `replaceItems`, `clear`, `pay`, `ensureItemsCanChange`, `isSold`; no setters, no builder
- [x] PR 1: `Product` immutable with `subtotal()`
- [x] PR 1: `CartService` reduced to load → call entity → save
- [x] PR 2: `ProductResponse`, `CategoryResponse`, `CartItemResponse`; `client` package used by `ProductService` only
- [x] Docs updated per PR

## Surprises & Discoveries

**2026-11-12**: `CartServiceTest` passed without a single change after the entity rewrite, including `rejectsSoldCart` (which has no `ProductService` stub and therefore proves the SOLD check still runs before the catalog lookup).

**2026-11-12**: Spring Data MongoDB reads `Cart` through the `protected` no-arg constructor plus field access, and `Product` through its builder-generated all-args constructor, so neither needs public setters. Covered by the integration tests (carts are saved, reloaded and mutated again).

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-11-12 | Removed `@Setter`, `@Builder` and `@AllArgsConstructor` from `Cart`; the only way in is `Cart.open(clientId, items)` | A builder lets any caller create a `SOLD` cart without payment method or a cart whose total doesn't match its items. Tests that need a persisted-looking cart set `id` by reflection, which is what Mongo does. |
| 2026-11-12 | `Cart` throws `BusinessException` itself, with one message per operation | The three messages are part of the HTTP contract (pinned by tests) and they differ per operation, so a single generic "cart is sold" exception would change responses. Keeping them next to the rule is what A1 asked for. |
| 2026-11-12 | `ensureItemsCanChange()` is public and `CartService.updateCart` calls it before the catalog lookup | Without it, updating a sold cart would first hit Platzi (slow, and an unknown product would turn the 400 into a 404). `replaceItems` still checks on its own, so the rule cannot be bypassed. |
| 2026-11-12 | `pay(null)` and `open(..., null)` throw `NullPointerException` | The controller validates both (invariant I5.1); reaching the entity with null is a programming error and should surface as a logged 500, not be silently accepted as before. |
| 2026-11-12 | Items stored as `List.copyOf(...)`; `getProducts()` is unmodifiable | Guarantees invariant I3 (total always derived): nobody can add an item behind the entity's back. |
| 2026-11-12 | Kept the name `Product` for the cart line | Renaming to `CartItem` is desirable (see GLOSSARY) but touches the Mongo mapping and every test; it fits Phase 3 (organization) better. |
| 2026-11-12 | `ProductService` returns `ProductResponse` (a controller DTO) instead of a separate domain model for catalog products | Same convention as `CartService` returning `CartResponse`. The catalog has no behavior of its own here, so a third representation would only add a mapping step. |
| 2026-11-12 | The cached value changed type, so the caches were renamed to `catalog-products` / `catalog-product` | Redis may still hold `PlatziProductResponse` entries written by the previous version; reading them as `ProductResponse` would fail with `ClassCastException` until the 60 s TTL expired. New names mean old entries are simply never read. |
| 2026-11-12 | `Status` and `PaymentMethod` enums stay shared between entity and DTOs | They are domain vocabulary, also used in `PaymentRequest`; duplicating them would add conversions without decoupling anything real. |
| 2026-11-12 | Platzi records no longer implement `Serializable` | They are not cached anymore; keeping the marker would suggest otherwise. |

## Context & Orientation

**Key files**: `entity/Cart.java`, `entity/Product.java`, `service/CartService.java`, `controller/response/*`
**Related docs**: `.claude/ARCHITECTURE.md` invariants I2, I3, I5; Phase 1 ExecPlan

## Concrete Steps

```bash
./mvnw verify
```

## Validation & Acceptance

- [x] PR 1: no HTTP contract change (controller and integration tests untouched and green)
- [x] PR 1: every state rule has an entity-level test in `CartTest`
- [x] PR 2: JSON shape of every response unchanged (controller and integration tests assert the same paths as before)

## Outcomes & Retrospective

**What shipped**: A1 and A2. 86 tests, 99.4% instructions / 100% branches. No HTTP contract change in either PR.
**What we learned**: with the Phase 0 suite in place, both refactors could be validated by leaving the HTTP-level tests untouched.
**What's left**: B8 product decisions (duplicated ids in a request; whether a cart can be created empty). Renaming `entity.Product` to `CartItem` is a candidate for Phase 3.
