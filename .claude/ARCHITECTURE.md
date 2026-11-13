# Architecture: Module Map & Invariants

Deeper-dive reference for the layered architecture summarized in the root `CLAUDE.md`. Read that first. Human-facing diagrams (sequence, state machine) are in `docs/ARCHITECTURE.md`.

## Directory structure

```
src/main/java/br/com/expresscart/
├── ExpressCartApplication.java          # @SpringBootApplication only
│
├── client/
│   ├── PlatziStoreClient.java           # Feign: GET /products, GET /products/{id}; default error decoder (FeignException)
│   └── response/                        # PlatziProductResponse, PlatziCategoryResponse (used by ProductService only)
│
├── config/
│   ├── ApplicationControllerAdvice.java # Global exception → HTTP translator (GenericErrorResponse / ValidationErrorResponse list)
│   ├── FeignConfig.java                 # @EnableFeignClients
│   ├── CacheConfig.java                 # @EnableCaching; Redis caches as JSON, typed serializer + TTL per cache; cache name constants
│   └── OpenApiConfig.java               # OpenAPI metadata
│
├── controller/
│   ├── api/docs/                        # OpenAPI contract interfaces (CartApi, ProductApi), annotations only
│   ├── request/                         # CartRequest (POST), CartUpdateRequest (PUT), ProductRequest, PaymentRequest (records + Bean Validation)
│   │   └── validation/                  # custom constraints: @UniqueProductIds
│   ├── response/                        # CartResponse + CartItemResponse, PaymentResponse (built from Cart); ProductResponse + CategoryResponse (Serializable, cached in Redis)
│   ├── CartController.java              # /api/v1/cart
│   └── ProductController.java           # /api/v1/product
│
├── entity/
│   ├── Cart.java                        # @Document("carts"); open / replaceItems / clear / pay; owns the state rules and the total
│   ├── CartItem.java                    # embedded cart line, immutable (snapshot of title/price); subtotal()
│   ├── Status.java                      # OPEN, SOLD
│   └── PaymentMethod.java               # PIX, DEBIT, CREDIT
│
├── exception/
│   ├── BusinessException.java           # → 400
│   ├── DataNotFoundException.java       # → 404
│   ├── ExternalServiceException.java    # → 502
│   └── response/                        # GenericErrorResponse, ValidationErrorResponse
│
├── repository/
│   └── CartRepository.java              # MongoRepository + findByClientIdAndStatus
│
└── service/
    ├── CartService.java                 # cart use cases: load → call Cart → save; one-open-cart-per-client check
    └── ProductService.java              # catalog: calls Platzi, translates FeignException, maps to ProductResponse; cached via the names in CacheConfig
```

## Module responsibilities

- **controller/**: HTTP boundary only. Every `@RequestBody` is `@Valid`. No business logic.
- **service/**: `CartService` orchestrates (load, catalog lookup, save) and owns the only rule that needs the repository (I1); `ProductService` is the only caller of `PlatziStoreClient` and owns caching.
- **client/**: external contract. Its DTOs do not leave `ProductService`.
- **entity/**: persistence model **and** domain behavior. `Cart` has no setters/builder; state changes only through its business methods. Spring Data reads it via the protected no-arg constructor and field access.
- **config/**: cross-cutting (errors, cache, Feign registration, OpenAPI). `@Enable*` annotations belong here. Changes here affect every endpoint.

## Architectural invariants (current behavior: keep or change deliberately)

- **I1: One open cart per client.** `CartService.createCart` rejects a second `OPEN` cart for the same `clientId` (`BusinessException` → 400). Service-level check only; no unique index (B7).
- **I2: SOLD is terminal.** `Cart.replaceItems`, `clear` and `pay` reject a `SOLD` cart (`BusinessException` → 400, one message per operation). There is no transition back to `OPEN`. `CartService.updateCart` calls `cart.ensureItemsCanChange()` **before** the catalog lookup, so a sold cart is refused without calling Platzi.
- **I3: Total is derived.** `totalPrice` is recomputed inside `Cart` whenever items change; the item list is an unmodifiable copy, so it cannot drift. No caller computes or sets the total.
- **I4: Item price is a snapshot.** `title`/`price` are copied from Platzi when the item list is built; carts are never re-priced afterwards.
- **I5: Update replaces items.** `PUT /cart/{id}` (`CartUpdateRequest`, `products` only) rebuilds the whole list; owner and status never change. A `clientId` sent in the body is silently ignored (unknown JSON properties are not rejected).
- **I5.2: One line per product.** A request listing the same product id twice is rejected with 400 (`@UniqueProductIds` on both cart request records). It is a boundary rule: `Cart` itself does not deduplicate, so any new entry point that builds item lists must apply the same constraint.
- **I5.3: A cart is born with items but may become empty.** `POST /cart` requires at least one item (`@NotEmpty` on `CartRequest.products`); `PUT /cart/{id}` with `[]` and `/clear` are both valid ways to empty an open cart. `Cart.open` itself accepts an empty list: the rule is about the create endpoint, not about the entity's valid states.
- **I5.1: Every request body is validated at the controller.** Services assume valid input (e.g. `payCart` does not re-check a null `paymentMethod`).
- **I6: Every product id must exist upstream.** Unknown id → Platzi 400 → `ProductService` → `DataNotFoundException("Produto não encontrado: id=X")` → 404. Failed lookups are not cached.
- **I6.1: Upstream failures are 502, never 500.** Any other `FeignException` → `ExternalServiceException` → 502, logged with its cause. A cart is not saved when a lookup fails.
- **I7: Error shape.** Validation errors → 400 with a JSON **array** of `{field, message}`; every other handled error → `{timestamp, status, message}`.
- **I7.1: Client mistakes are 4xx.** Unreadable JSON, unknown enum value, wrong field type, non-numeric path id → 400; wrong `Content-Type` → 415. A 500 means an unexpected server error and is always logged with its stack trace; its response body never carries exception details.
- **I7.2: Responses are owned DTOs.** No controller returns an entity or a Platzi record; a change in Mongo mapping or in Platzi's payload cannot silently change this API's JSON.
- **I9: The stored cart document is a contract.** Fields: `_id`, `clientId`, `totalPrice`, `products` (each `_id`, `title`, `price`, `quantity`), `status`, `paymentMethod`, `_class`. Classes may be renamed (embedded items carry no `_class`), but renaming a **field** needs a data migration. Pinned by `CartIntegrationTest.storedDocumentKeepsItsFieldNames` and `cartWrittenBeforeTheRenameIsStillReadable`.
- **I8: Products are cached.** `ProductService` results live in Redis as JSON, in the caches named by `CacheConfig` (`catalog-product-v2::{id}`, `catalog-products-v2`), each with its TTL from `expresscart.cache.*` (60 s by default). Repeated lookups within the TTL don't hit Platzi; failed lookups are never cached.

## Data flow

```
HTTP → Controller (@Valid DTO) → CartService ──► CartRepository → MongoDB
                                     │
                                     └──► ProductService ──(cache miss)──► PlatziStoreClient → Platzi API
                                              ▲ Redis cache                    └ FeignException → translated in ProductService
Exceptions → config/ApplicationControllerAdvice → JSON error body
```

## Adding / changing an endpoint: checklist

1. **Request/Response DTO**: record in `controller/request` or `controller/response`, with Bean Validation on the request.
2. **Service**: business rule + exception (`BusinessException` / `DataNotFoundException`, or a new one in `exception/`, mapped in `ApplicationControllerAdvice`).
3. **Controller**: method with `@Valid @RequestBody`.
4. **Contract docs**: matching method in `controller/api/docs/*Api.java`, with real error messages in the examples.
5. **Tests**: service unit test + `@WebMvcTest` slice; integration test if it touches Mongo/Redis/Platzi.
6. **Docs**: update `docs/ARCHITECTURE.md` §6 (HTTP contract) and this file if an invariant changes.
