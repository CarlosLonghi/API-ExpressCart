# Architecture: Module Map & Invariants

Deeper-dive reference for the layered architecture summarized in the root `CLAUDE.md`. Read that first. Human-facing diagrams (sequence, state machine) are in `docs/ARCHITECTURE.md`.

## Directory structure

```
src/main/java/br/com/expresscart/
├── ExpressCartApplication.java          # @SpringBootApplication + @EnableFeignClients
│
├── client/
│   ├── PlatziStoreClient.java           # Feign: GET /products, GET /products/{id}; default error decoder (FeignException)
│   └── response/                        # PlatziProductResponse, PlatziCategoryResponse (Serializable, cached in Redis)
│
├── config/
│   ├── ControllerAdvice.java            # Global exception → HTTP translator (GenericErrorResponse / ValidationErrorResponse list)
│   └── OpenApiConfig.java               # OpenAPI metadata
│
├── controller/
│   ├── api/docs/                        # OpenAPI contract interfaces (CartApi, ProductApi), annotations only
│   ├── request/                         # CartRequest (POST), CartUpdateRequest (PUT), ProductRequest, PaymentRequest (records + Bean Validation)
│   ├── response/                        # CartResponse, PaymentResponse (records, built from Cart)
│   ├── CartController.java              # /api/v1/cart
│   └── ProductController.java           # /api/v1/product
│
├── entity/
│   ├── Cart.java                        # @Document("carts"); calculateTotalPrice()
│   ├── Product.java                     # embedded cart item (snapshot of title/price)
│   ├── Status.java                      # OPEN, SOLD
│   └── PaymentMethod.java               # PIX, DEBIT, CREDIT
│
├── exception/
│   ├── handler/                         # BusinessException (→400), DataNotFoundException (→404), ExternalServiceException (→502). Misnamed package (A3)
│   └── response/                        # GenericErrorResponse, ValidationErrorResponse
│
├── repository/
│   └── CartRepository.java              # MongoRepository + findByClientIdAndStatus
│
└── service/
    ├── CartService.java                 # all cart rules (create/find/update/pay/clear)
    └── ProductService.java              # @Cacheable("products"), @Cacheable("product", key=#id); translates FeignException; @EnableCaching lives here
```

## Module responsibilities

- **controller/**: HTTP boundary only. Every `@RequestBody` is `@Valid`. No business logic.
- **service/**: `CartService` owns every cart rule; `ProductService` is the only caller of `PlatziStoreClient` and owns caching.
- **client/**: external contract. Its DTOs should not leak past `service/` (today they do, A2).
- **entity/**: persistence model. `Cart` has setters for most fields; the only domain behavior is `calculateTotalPrice()`.
- **config/**: cross-cutting (errors, OpenAPI). Changes here affect every endpoint.

## Architectural invariants (current behavior: keep or change deliberately)

- **I1: One open cart per client.** `CartService.createCart` rejects a second `OPEN` cart for the same `clientId` (`BusinessException` → 400). Service-level check only; no unique index (B7).
- **I2: SOLD is terminal.** `updateCart`, `clearCart` and `payCart` reject a `SOLD` cart (`BusinessException` → 400). There is no transition back to `OPEN`.
- **I3: Total is derived.** `totalPrice` has no setter; every item change must call `cart.calculateTotalPrice()` before saving.
- **I4: Item price is a snapshot.** `title`/`price` are copied from Platzi when the item list is built; carts are never re-priced afterwards.
- **I5: Update replaces items.** `PUT /cart/{id}` (`CartUpdateRequest`, `products` only) rebuilds the whole list; owner and status never change. A `clientId` sent in the body is silently ignored (unknown JSON properties are not rejected).
- **I5.1: Every request body is validated at the controller.** Services assume valid input (e.g. `payCart` does not re-check a null `paymentMethod`).
- **I6: Every product id must exist upstream.** Unknown id → Platzi 400 → `ProductService` → `DataNotFoundException("Produto não encontrado: id=X")` → 404. Failed lookups are not cached.
- **I6.1: Upstream failures are 502, never 500.** Any other `FeignException` → `ExternalServiceException` → 502, logged with its cause. A cart is not saved when a lookup fails.
- **I7: Error shape.** Validation errors → 400 with a JSON **array** of `{field, message}`; every other handled error → `{timestamp, status, message}`.
- **I7.1: Client mistakes are 4xx.** Unreadable JSON, unknown enum value, wrong field type, non-numeric path id → 400; wrong `Content-Type` → 415. A 500 means an unexpected server error and is always logged with its stack trace; its response body never carries exception details.
- **I8: Products are cached.** `ProductService` results live in Redis for `spring.cache.redis.time-to-live` (60 s). Repeated lookups within the TTL don't hit Platzi.

## Data flow

```
HTTP → Controller (@Valid DTO) → CartService ──► CartRepository → MongoDB
                                     │
                                     └──► ProductService ──(cache miss)──► PlatziStoreClient → Platzi API
                                              ▲ Redis cache                    └ FeignException → translated in ProductService
Exceptions → config/ControllerAdvice → JSON error body
```

## Adding / changing an endpoint: checklist

1. **Request/Response DTO**: record in `controller/request` or `controller/response`, with Bean Validation on the request.
2. **Service**: business rule + exception (`BusinessException` / `DataNotFoundException`, or a new one mapped in `ControllerAdvice`).
3. **Controller**: method with `@Valid @RequestBody`.
4. **Contract docs**: matching method in `controller/api/docs/*Api.java`, with real error messages in the examples.
5. **Tests**: service unit test + `@WebMvcTest` slice; integration test if it touches Mongo/Redis/Platzi.
6. **Docs**: update `docs/ARCHITECTURE.md` §6 (HTTP contract) and this file if an invariant changes.
