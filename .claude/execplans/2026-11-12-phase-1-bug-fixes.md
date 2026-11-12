# ExecPlan: Phase 1, bug fixes without redesign

**Date**: 2026-11-12
**Status**: In Progress
**Covers**: B1, B2, B3, B4, B5, B8 (`docs/REFACTORING.md`)

## Goal

The API answers with the right status code and a useful message for every input and upstream failure, and unexpected errors are logged. No structural redesign (that is Phase 2+).

## Big Picture & Context

**Current state**: Phase 0 pinned the bugs with characterization tests.
**Desired state**: each bug fixed, with its test flipped in the same commit.
**Why now**: these are behavior bugs visible to API clients; later phases move code around and should not carry them along.
**Affected modules**: `service/ProductService`, `client/`, `config/ControllerAdvice`, `controller/`, `controller/request`, `exception/handler`, tests, docs.

Delivered as three stacked PRs, one per scope:

| PR | Branch | Bugs |
|---|---|---|
| 1 | `fix/platzi-error-handling` | B1, B2 |
| 2 | `fix/request-validation` | B3, B8 (validation messages) |
| 3 | `fix/malformed-input-handling` | B4, B5, B8 (`Optional.of`) |

## Progress Checklist

- [x] PR 1: remove `CustomErrorDecoder`; `ProductService` translates Feign errors; 502 for upstream failures
- [x] PR 2: `@Valid` on `PUT /cart/{id}` and `PUT /cart/{id}/payment`; `CartUpdateRequest`; fixed `clientId` message
- [ ] PR 3: 400 for malformed input; log unexpected errors
- [ ] Docs updated per PR (`docs/REFACTORING.md`, `docs/ARCHITECTURE.md`, `.claude/*`, `CLAUDE.md`)

## Surprises & Discoveries

**2026-11-12**: WireMock `Fault.CONNECTION_RESET_BY_PEER` surfaces in Feign as `RetryableException`, which extends `FeignException`, so one `catch (FeignException)` covers HTTP errors and connection failures alike.

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-11-12 | B1: delete `CustomErrorDecoder` and translate in `ProductService`, instead of keeping the decoder and dropping the service logic | Only the service knows the product id, so only it can produce "Produto não encontrado: id=X". A decoder is global to the client and would also turn a 400 from `GET /products` into "product not found". |
| 2026-11-12 | `ProductService.getProductById` returns the product directly instead of `Optional` | With one translation point, "absent" is always an exception; the `Optional` only fed two dead branches (controller `orElseGet`, `CartService.orElseThrow`). The cached value type is unchanged (Spring unwraps `Optional` before caching), so existing Redis entries stay readable. |
| 2026-11-12 | Upstream failures → 502 via new `ExternalServiceException`, logged in `ControllerAdvice` | 500 says "our bug"; 502 tells the client the dependency failed and a retry may work. Logging the cause here covers the part of B5 that matters most. |
| 2026-11-12 | Message for unknown product changed from "Produto não encontrado (Platzi Client)" to "Produto não encontrado: id=X" | It is the message the code always intended (it was in the unreachable branch) and it tells which item of a multi-item cart failed. Status code is unchanged (404). |
| 2026-11-12 | B3: new `CartUpdateRequest` (items only) instead of `@Valid CartRequest` on PUT | Validating `CartRequest` would make `clientId` mandatory on update even though it is ignored, breaking clients that never sent it. Clients that do send it keep working because unknown JSON properties are ignored. |
| 2026-11-12 | Empty `products` list still accepted on create and update | An empty cart is already a valid state (`clear` produces it). Requiring one item on create is a product decision, left open in `docs/REFACTORING.md` B8. |
| 2026-11-12 | Removed the service-level test for null `paymentMethod` | The boundary now rejects it; the service has no guard of its own. Phase 2 (A1) moves the rule into `Cart.pay`. |

## Context & Orientation

**Key files**: `service/ProductService.java`, `config/ControllerAdvice.java`, `controller/CartController.java`, `controller/request/*`
**Related docs**: Phase 0 ExecPlan (bug → test map), `.claude/ARCHITECTURE.md` invariants I6, I6.1, I7

## Concrete Steps

```bash
./mvnw verify
```

## Validation & Acceptance

- [x] PR 1: unknown product → 404 with id (create cart, update cart, `GET /product/{id}`); Platzi 5xx / connection reset → 502; failed lookups not cached; cart not saved when a lookup fails
- [x] PR 2: invalid body on PUT endpoints → 400 with field list; cart untouched after a rejected update/payment
- [ ] PR 3: malformed JSON, unknown enum value, non-numeric id → 400

## Outcomes & Retrospective

**What shipped**: (fill in when the three PRs are merged)
