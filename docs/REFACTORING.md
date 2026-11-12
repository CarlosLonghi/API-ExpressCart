# 🔧 Diagnóstico e Plano de Refatoração

Levantamento técnico do estado atual do código (branch `main`, após o PR #9 de DTOs de resposta). Cada item tem **severidade**, **onde está** e **sugestão**. Para entender o funcionamento atual, veja [ARCHITECTURE.md](ARCHITECTURE.md).

**Legenda:** 🔴 bug / comportamento incorreto · 🟠 risco ou dívida importante · 🟡 qualidade / consistência · ⚪ cosmético

---

## 1. Bugs e comportamentos incorretos

### ✅ B1. `CustomErrorDecoder` tornava código do `ProductService` inalcançável (corrigido)
- **Era:** o decoder convertia o 400 da Platzi em `DataNotFoundException` antes de chegar ao service, então o `catch (FeignException)` do service, o `orElseGet` do controller e o `orElseThrow` do `CartService` nunca executavam. A mensagem de erro não trazia o id.
- **Correção:** o `CustomErrorDecoder` foi removido. O `ProductService` é o único lugar que traduz erros do Feign e não retorna mais `Optional`. Produto inexistente → `DataNotFoundException("Produto não encontrado: id=X")` → 404.

### ✅ B2. Decoder devolvia `Exception` checada para erros não-400 (corrigido)
- **Era:** qualquer erro da Platzi diferente de 400 virava `UndeclaredThrowableException` e 500 genérico, sem log.
- **Correção:** falhas da Platzi (5xx, outros 4xx, timeout, conexão recusada) viram `ExternalServiceException` → **502** com `"Erro ao consultar o catálogo de produtos (Platzi)"`, e a causa vai para o log.

### ✅ B3. `@Valid` ausente em `PUT /cart/{id}` e `PUT /cart/{id}/payment` (corrigido)
- **Era:** quantidade 0, negativa ou > 100, `products: null` (NPE → 500) e `paymentMethod: null` (carrinho `SOLD` sem método de pagamento) passavam direto.
- **Correção:** os dois endpoints validam o corpo com `@Valid`. O `PUT /cart/{id}` passou a usar `CartUpdateRequest`, só com `products`. Um `clientId` enviado no corpo continua sendo aceito e ignorado.

### ✅ B4. Entrada malformada respondia 500 em vez de 400 (corrigido)
- **Era:** JSON inválido, valor fora do enum (`paymentMethod: "BOLETO"`), tipo errado num campo e id não numérico na URL caíam no handler genérico e viravam 500.
- **Correção:** handlers próprios no `ControllerAdvice`: `HttpMessageNotReadableException` → 400, `MethodArgumentTypeMismatchException` → 400, `HttpMediaTypeNotSupportedException` → 415.

### ✅ B5. Handler genérico engolia exceções sem log (corrigido)
- **Era:** qualquer erro inesperado virava 500 sem stack trace no log.
- **Correção:** o handler genérico registra `log.error("Erro inesperado", exception)`. A resposta continua com a mensagem fixa, sem expor detalhes internos.

### 🟡 B6. Senha do Redis não bate com o docker-compose (verificado: funciona)
- **Onde:** `application.yml` (`spring.data.redis.password: password`) × `docker-compose.yml`
- **Problema:** o container Redis oficial sobe **sem senha**, e `ALLOW_EMPTY_PASSWORD` só é lida por imagens Bitnami. **Não quebra a aplicação:** o Lettuce autentica com `HELLO ... AUTH default <senha>`, que um Redis sem senha aceita. Os testes de integração rodam com essa mesma combinação e o cache funciona. É só uma configuração enganosa: parece haver senha, mas não há.
- **Sugestão:** alinhar os dois arquivos (ex.: `command: redis-server --requirepass ${REDIS_PASSWORD}`) e ler a senha de variável de ambiente.

### 🟠 B7. Condição de corrida em "um carrinho aberto por cliente"
- **Onde:** `service/CartService.java` (`createCart`)
- **Problema:** a checagem (`find`) e a gravação (`save`) não são atômicas. Duas requisições simultâneas podem criar dois carrinhos `OPEN` para o mesmo cliente.
- **Sugestão:** índice único parcial no Mongo (`{clientId:1}` com `partialFilterExpression: {status: "OPEN"}`) e tratar `DuplicateKeyException` → 400/409.

### 🟡 B8. Pequenos bugs
- ✅ `ControllerAdvice.handleNoHandlerFound`: trocado `Optional.of(...)` por `Optional.ofNullable`.
- ✅ `CartRequest.clientId`: a mensagem do `@Min(1)` dizia *"não pode ser menor que 0"*. Agora diz *"deve ser maior que 0"*.
- `CartRequest.products`: `@NotNull` aceita lista vazia, então é possível criar carrinho sem itens. **Mantido por enquanto:** carrinho vazio já é um estado válido (é o resultado do `clear`). Decidir se a criação deve exigir ao menos um item (`@NotEmpty`).
- Itens com o mesmo `id` repetido no pedido não são agrupados e viram duas linhas no carrinho.
- ✅ `Cart.calculateTotalPrice()` lançava NPE com `products` nulo. O método público não existe mais: o total é calculado dentro da entidade e a lista de itens nunca fica nula.

---

## 2. Design e arquitetura

### ✅ A1. Modelo de domínio anêmico; regra "carrinho vendido" repetida 3× (corrigido)
- **Era:** o `CartService` repetia a checagem de `SOLD` em `updateCart`, `payCart` e `clearCart`, alterava o carrinho por setters e precisava lembrar de recalcular o total.
- **Correção:** a entidade `Cart` só muda por `Cart.open(...)`, `replaceItems(...)`, `clear()` e `pay(...)`. Cada método valida o estado e recalcula o total. Não há mais setters nem builder. O `CartService` só carrega, chama a entidade e salva.

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
| `PUT /cart/{id}` | ✅ corrigido junto com B3 |
| `POST` / `PUT` | Exemplo de validação usa o campo `email`, que não existe |
| `POST` | Mensagem documentada difere da real (`"...para o cliente de id: X"`) |
| `PUT /clear` | 200 documentado **sem conteúdo**, mas retorna `CartResponse` |
| `PUT /payment` | Descrição do 200 diz "Dados do Carrinho" e não há exemplo de `PaymentRequest` |
| `CartController` | ✅ import não usado removido |

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

✅ **Fase 0 concluída.** A suíte de caracterização tem 71 testes (58 unitários/slice e 13 de integração) e cobre 99% das instruções e 100% dos branches. Ela registra o comportamento atual, **incluindo os bugs**: cada teste que fixa um bug tem o id no comentário (`// B3: ...`).

| Tipo | Classes | Infra |
|---|---|---|
| Unitário | `CartTest`, `CartServiceTest`, `ProductServiceTest`, `CustomErrorDecoderTest` | nenhuma |
| Slice web (`@WebMvcTest`) | `CartControllerTest`, `ProductControllerTest` | nenhuma |
| Integração | `CartIntegrationTest`, `ProductIntegrationTest`, `ExpressCartApplicationTests` | Docker (Testcontainers: Mongo + Redis) e WireMock no lugar da Platzi |

**Ao corrigir um bug:** altere o teste correspondente no mesmo commit. A tabela bug → testes está no ExecPlan [`.claude/execplans/2026-11-12-phase-0-characterization-tests.md`](../.claude/execplans/2026-11-12-phase-0-characterization-tests.md).

**Ainda sem teste:** B7 (corrida, precisa de teste de concorrência junto com a correção).

**Pendências de build encontradas na Fase 0:**
- O `mvnw` não é executável no git (`./mvnw` → *Permission denied*). Use `bash ./mvnw` até corrigir o modo do arquivo.
- O JaCoCo só gera relatório. Falta decidir se haverá cobertura mínima obrigatória no `verify`.

---

## 6. Plano sugerido (ordem de execução)

Cada fase pode virar um PR pequeno, seguindo o padrão de branches que o repositório já usa (`refactor/...`, `fix/...`, `test/...`).

| Fase | Objetivo | Itens |
|---|---|---|
| **0. Rede de segurança** ✅ | Testes de caracterização do comportamento atual | §5 |
| **1. Correções** ✅ | Corrigir bugs sem mudar o design | B1, B2, B3, B4, B5 e parte do B8 (o restante vai para a Fase 2) |
| **2. Domínio** | Comportamento na entidade, DTOs próprios | A1, A2, B8 (itens repetidos, lista nula, carrinho vazio na criação) |
| **3. Organização** | Pacotes, nomes, config de cache | A3, A4 |
| **4. API** | Semântica REST, `ProblemDetail`, Swagger atualizado | A5, §3 |
| **5. Robustez** | Índice único, timeouts/resiliência, config externa | B7, A6 |
| **6. Infra** | Versões, Dockerfile, compose completo, senha do Redis | §4, B6 |

> A fase 4 muda o contrato HTTP. Se houver consumidores da API, vale versionar (`/api/v2`) ou anunciar a mudança antes.
