# Glossary: Domain Terminology

Deeper-dive reference for the domain summarized in the root `CLAUDE.md`. Code identifiers are in English; user-facing messages and docs are in Portuguese.

## Core entities

### Cart (Carrinho)
MongoDB document in the `carts` collection. `id` (ObjectId string), `clientId` (`Long`, not validated against any customer registry, since there is no auth/customer module), `products` (embedded `List<Product>`), `totalPrice` (`BigDecimal`, derived, no setter), `status` (`Status`), `paymentMethod` (`PaymentMethod`, `@JsonInclude(NON_NULL)`, null until paid).

### Product / item (Produto / item do carrinho)
Two different things share the name:
- **`entity.Product`**: a cart line embedded in `Cart`: `id`, `title`, `price`, `quantity`. Snapshot of the catalog at the time it was added (see invariant I4).
- **`client.response.PlatziProductResponse`**: the catalog product as returned by Platzi. Internal to `ProductService`.
- **`controller.response.ProductResponse`**: the catalog product as this API exposes it (`id`, `title`, `price`, `images`, `category`). What `GET /api/v1/product` returns and what is cached.
- **`controller.response.CartItemResponse`**: `entity.Product` as returned inside `CartResponse.products`.

### Status
`OPEN` (aberto, editable) → `SOLD` (vendido/pago, terminal). Only `PUT /cart/{id}/payment` moves to `SOLD`.

### PaymentMethod (método de pagamento)
`PIX`, `DEBIT`, `CREDIT`. Just a label stored on the cart; no payment gateway integration.

### Client (cliente)
Only exists as `clientId` on the cart. "Client" also appears as the Feign **HTTP client** (`client/` package), so don't confuse the two.

## External systems

### Platzi Fake Store API
`https://api.escuelajs.co/api/v1` (`expresscart.client.platzi`). Public, rate-unknown, data may be reset by its maintainers. Returns **400** (not 404) for an unknown product id. Never call it from tests; use WireMock.

### Redis
Cache only (`catalog-products-v2`, `catalog-product-v2::{id}`), JSON values (`ProductResponse`), TTL per cache from `expresscart.cache.*` (60 s). Configured in `config/CacheConfig`.

### MongoDB
Database `express-cart`, single collection `carts`.

## Error vocabulary

| Exception | HTTP | Typical message |
|---|---|---|
| `BusinessException` | 400 | `"Já existe um carrinho aberto para o cliente de id: X"`, `"Não é possivel alterar os dados de um carrinho já vendido."`, `"Operação cancelada. O carrinho informado já foi pago."`, `"Não é possivel limpar um carrinho já vendido."` |
| `DataNotFoundException` | 404 | `"Carrinho não encontrado."`, `"Produto não encontrado: id=X"` |
| `ExternalServiceException` | 502 | `"Erro ao consultar o catálogo de produtos (Platzi)"` |
| `MethodArgumentNotValidException` | 400 | list of `{field, message}` from the Bean Validation messages in `controller/request/*` |
| `HttpMessageNotReadableException` | 400 | `"Corpo da requisição inválido: verifique o JSON e os valores permitidos em cada campo"` |
| `MethodArgumentTypeMismatchException` | 400 | `"Valor inválido para o parâmetro 'id': abc"` |
| `HttpMediaTypeNotSupportedException` | 415 | `"Content-Type não suportado. Use application/json"` |

## Quick lookups

| Term | File |
|------|------|
| Cart state rules | `entity/Cart.java` |
| Cart use cases / one-open-cart rule | `service/CartService.java` |
| Total price calculation | `entity/Cart.java#setItems`, `entity/Product.java#subtotal` |
| Catalog | `service/ProductService.java` |
| Cache names, serialization, TTL | `config/CacheConfig.java`, `application.yml` (`expresscart.cache.*`) |
| Feign client / error mapping | `client/PlatziStoreClient.java`, `service/ProductService.java` |
| Error → HTTP | `config/ControllerAdvice.java` |
| Swagger docs per endpoint | `controller/api/docs/CartApi.java`, `ProductApi.java` |
| Validation messages | `controller/request/*.java` |
| Custom constraints | `controller/request/validation/` |
| Bug / debt catalog | `docs/REFACTORING.md` |
