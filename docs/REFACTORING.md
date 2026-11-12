# 🔧 Diagnóstico e Plano de Refatoração

Levantamento técnico do estado atual do código (branch `main`, após o PR #9 de DTOs de resposta). Cada item tem **severidade**, **onde está** e **sugestão**. Para entender o funcionamento atual, veja [ARCHITECTURE.md](ARCHITECTURE.md).

**Legenda:** 🔴 bug / comportamento incorreto · 🟠 risco ou dívida importante · 🟡 qualidade / consistência · ⚪ cosmético

---

## 1. Bugs e comportamentos incorretos

### 🔴 B1. `CustomErrorDecoder` torna código do `ProductService` inalcançável
- **Onde:** `config/CustomErrorDecoder.java`, `service/ProductService.java` (`getProductById`), `controller/ProductController.java`
- **Problema:** o decoder converte o 400 da Platzi em `DataNotFoundException` **antes** de chegar ao service. Por isso o `catch (FeignException e)` com `status() == 400 → Optional.empty()` nunca executa, e o `orElseGet(() -> notFound())` do controller e o `orElseThrow(... "Produto não encontrado: id=X")` do `CartService` também não. Na prática, a mensagem de erro sempre é `"Produto não encontrado (Platzi Client)"`, sem o ID.
- **Sugestão:** escolher **uma** estratégia. Ou o decoder traduz os erros e o service deixa de retornar `Optional`, ou o decoder sai e o service trata a `FeignException`.

### 🔴 B2. Decoder devolve `Exception` checada para erros não-400
- **Onde:** `config/CustomErrorDecoder.java` (`default: return new Exception(...)`)
- **Problema:** como a interface Feign não declara exceção checada, o proxy a embrulha em `UndeclaredThrowableException`. Isso vira 500 sem log nenhum (ver B5). Também trata 5xx e 404 da Platzi do mesmo jeito.
- **Sugestão:** retornar uma `RuntimeException` própria (ex.: `ExternalServiceException`) mapeada para **502/503**, e usar `ErrorDecoder.Default` como fallback.

### 🔴 B3. `@Valid` ausente em `PUT /cart/{id}` e `PUT /cart/{id}/payment`
- **Onde:** `controller/CartController.java` (`updateCart`, `payCart`)
- **Problema:** quantidade 0, negativa ou > 100, `products: null` (→ NPE → 500) e `paymentMethod: null` (o carrinho fica `SOLD` sem método de pagamento) passam direto.
- **Sugestão:** adicionar `@Valid` e criar um DTO próprio para update, sem `clientId` (veja Q2).

### 🔴 B4. Entrada malformada responde 500 em vez de 400
- **Onde:** `config/ControllerAdvice.java`
- **Problema:** não há handler para `HttpMessageNotReadableException` (JSON inválido, `paymentMethod: "BOLETO"`) nem para `MethodArgumentTypeMismatchException` (`GET /product/abc`). Os dois caem no handler genérico e viram **500**.
- **Sugestão:** adicionar handlers que retornem 400.

### 🟠 B5. Handler genérico engole exceções sem log
- **Onde:** `config/ControllerAdvice.java` (`handleException`)
- **Problema:** qualquer erro inesperado vira 500 **sem stack trace no log**, o que impede o diagnóstico.
- **Sugestão:** `log.error("...", exception)` no handler genérico.

### 🟠 B6. Senha do Redis não bate com o docker-compose (verificar)
- **Onde:** `application.yml` (`spring.data.redis.password: password`) × `docker-compose.yml`
- **Problema:** o container Redis oficial sobe **sem senha**, e `ALLOW_EMPTY_PASSWORD` só é lida por imagens Bitnami. Mandar `AUTH` para um Redis sem senha costuma gerar erro de conexão, que só aparece na primeira chamada que usa o cache.
- **Sugestão:** alinhar os dois arquivos (ex.: `command: redis-server --requirepass ${REDIS_PASSWORD}`) e ler a senha de variável de ambiente.

### 🟠 B7. Condição de corrida em "um carrinho aberto por cliente"
- **Onde:** `service/CartService.java` (`createCart`)
- **Problema:** a checagem (`find`) e a gravação (`save`) não são atômicas. Duas requisições simultâneas podem criar dois carrinhos `OPEN` para o mesmo cliente.
- **Sugestão:** índice único parcial no Mongo (`{clientId:1}` com `partialFilterExpression: {status: "OPEN"}`) e tratar `DuplicateKeyException` → 400/409.

### 🟡 B8. Pequenos bugs
- `ControllerAdvice.handleNoHandlerFound`: `Optional.of(...)` lança NPE se a URL for nula. Deveria ser `Optional.ofNullable`.
- `CartRequest.clientId`: `@Min(1)` com a mensagem *"não pode ser menor que 0"*.
- `CartRequest.products`: `@NotNull` aceita lista vazia, então é possível criar carrinho sem itens. Decidir se isso é intencional (`@NotEmpty`).
- Itens com o mesmo `id` repetido no pedido não são agrupados e viram duas linhas no carrinho.
- `Cart.calculateTotalPrice()` lança NPE se `products` for nulo.

---

## 2. Design e arquitetura

### 🟠 A1. Modelo de domínio anêmico; regra "carrinho vendido" repetida 3×
- **Onde:** `CartService.updateCart / payCart / clearCart`
- **Sugestão:** levar o comportamento para a entidade, por exemplo `cart.replaceItems(items)`, `cart.clear()` e `cart.pay(method)`. Cada um valida o estado internamente (`ensureOpen()`) e recalcula o total. Assim o service fica só com a orquestração e os setters públicos de `status`/`paymentMethod` podem sair.

### 🟠 A2. DTOs vazam entidades e modelos externos
- `CartResponse` expõe `entity.Product` e `entity.Status` diretamente.
- `ProductController` devolve `PlatziProductResponse`, o DTO **do cliente externo**. Se a Platzi mudar o contrato, a API do Express Cart muda junto.
- **Sugestão:** criar `ProductResponse` / `CartItemResponse` próprios e um mapper (manual ou MapStruct). O pacote `client` só deve ser usado pelo `ProductService`.

### 🟡 A3. Pacotes e nomes
- `exception.handler` contém exceções, não handlers. O handler real (`ControllerAdvice`) está em `config`.
- `ControllerAdvice` tem o mesmo nome da anotação do Spring, o que confunde. Sugestão: `GlobalExceptionHandler` em `exception/`.
- `controller.request/response` → pode virar `dto.request/response` ou ficar por feature (`cart/`, `product/`).

### 🟡 A4. Configuração de cache
- `@EnableCaching` está em `ProductService`. Deveria ficar numa classe `@Configuration` (`CacheConfig`).
- Não há `RedisCacheConfiguration`, então o cache usa **serialização Java** (por isso `PlatziProductResponse implements Serializable`). JSON (`GenericJackson2JsonRedisSerializer`) é mais legível e não quebra quando a classe muda.
- O mesmo TTL (60s) vale para os caches `products` e `product`. Dá para configurar por cache.
- `@Cacheable` em método que retorna `Optional` funciona (o Spring desembrulha o valor), mas soma com B1. Rever junto.

### 🟡 A5. HTTP / semântica REST
- `PUT /cart/{id}/payment` e `PUT /cart/{id}/clear` são **ações**, não substituição de recurso. Alternativas: `POST /cart/{id}/payment` e `DELETE /cart/{id}/items`.
- `GET /product` devolve `204` para lista vazia. O mais comum em coleções é `200 []`.
- `GET /product/{id}` (no caminho inalcançável de B1) devolve 404 **sem corpo**, fora do padrão `GenericErrorResponse`.
- Erros de validação (lista) e demais erros (objeto) têm formatos diferentes. Considerar `ProblemDetail` (RFC 9457), que o Spring 6 já suporta.

### 🟡 A6. Configuração fixa no arquivo
- Hosts, portas, senha e URL externa estão fixos no `application.yml`. Sugestão: `${MONGO_HOST:localhost}` etc. e perfis `dev`/`test`.
- O Feign não tem timeout configurado nem retry/circuit breaker. Com a Platzi lenta, a thread fica presa.
- `buildProductListFromRequest` faz **uma chamada por item** (N chamadas). O cache ajuda, mas o primeiro acesso é sequencial.

---

## 3. Documentação Swagger desatualizada (`controller/api/docs/CartApi.java`)

| Endpoint | Problema |
|---|---|
| `GET /cart/{id}` | Schema do 200 ainda aponta para `Cart.class` (TODO antigo), mas o retorno é `CartResponse` |
| `PUT /cart/{id}` | 400 documentado como "Carrinho duplicado", mas o erro real é "carrinho já vendido" |
| `POST` / `PUT` | Exemplo de validação usa o campo `email`, que não existe |
| `POST` | Mensagem documentada difere da real (`"...para o cliente de id: X"`) |
| `PUT /clear` | 200 documentado **sem conteúdo**, mas retorna `CartResponse` |
| `PUT /payment` | Descrição do 200 diz "Dados do Carrinho" e não há exemplo de `PaymentRequest` |
| `CartController` | Import não usado de `entity.Cart` |

Textos com acentuação inconsistente ("possivel" × "possível").

---

## 4. Build e infraestrutura

| Item | Problema | Sugestão |
|---|---|---|
| `pom.xml` | `spring-cloud-starter-openfeign` fixado em `4.2.2`, ignorando o BOM `2025.0.0` | Remover `<version>` e deixar o BOM gerenciar |
| `pom.xml` | Blocos vazios (`<url/>`, `<licenses>`, `<scm>`, `<developers>`) | Preencher ou remover |
| `docker-compose.yml` | `mongo:4` (fora de suporte); `redis` sem tag; chave `version` obsoleta | Fixar versões atuais (ex.: `mongo:7`, `redis:7`) |
| `docker-compose.yml` | A app não está no compose | Adicionar `Dockerfile` + serviço `app` |
| `OpenApiConfig` | URL da licença aponta para `springdoc.org` | Usar a URL da Apache 2.0 ou remover |
| README | Dizia Java 17 (o projeto usa 21) | ✅ corrigido |

---

## 5. Testes

🟠 **Não há testes de comportamento.** O único teste (`contextLoads`) só sobe o contexto. Refatorar sem testes aumenta muito o risco de regressão.

**Cobertura mínima sugerida:**
- `CartServiceTest` (unitário, Mockito): R1–R5 de [ARCHITECTURE.md §4](ARCHITECTURE.md#4-regras-de-negócio-cartservice), cálculo do total, produto inexistente.
- `CartTest` (unitário): `calculateTotalPrice` e, depois de A1, as transições de estado.
- `CartControllerTest` (`@WebMvcTest`): status HTTP, validações e formato dos erros.
- Integração com **Testcontainers** (Mongo + Redis) e **WireMock** para a Platzi.

---

## 6. Plano sugerido (ordem de execução)

Cada fase pode virar um PR pequeno, seguindo o padrão de branches que o repositório já usa (`refactor/...`, `fix/...`, `test/...`).

| Fase | Objetivo | Itens |
|---|---|---|
| **0. Rede de segurança** | Testes de caracterização do comportamento atual | §5 |
| **1. Correções** | Corrigir bugs sem mudar o design | B1, B2, B3, B4, B5, B6, B8 |
| **2. Domínio** | Comportamento na entidade, DTOs próprios | A1, A2 |
| **3. Organização** | Pacotes, nomes, config de cache | A3, A4 |
| **4. API** | Semântica REST, `ProblemDetail`, Swagger atualizado | A5, §3 |
| **5. Robustez** | Índice único, timeouts/resiliência, config externa | B7, A6 |
| **6. Infra** | Versões, Dockerfile, compose completo | §4 |

> A fase 4 muda o contrato HTTP. Se houver consumidores da API, vale versionar (`/api/v2`) ou anunciar a mudança antes.
