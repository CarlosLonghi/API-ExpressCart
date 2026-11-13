# 🛒 Express Cart
Um serviço simples e eficiente de gerenciamento de carrinho de compras, construído com tecnologias modernas para oferecer performance e escalabilidade.

---

## 🔗 Sumário
- [📖 Sobre o Projeto](#-sobre-o-projeto)
- [🛠 Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [✨ Funcionalidades](#-funcionalidades)
- [📐 Arquitetura do Projeto](#-arquitetura-do-projeto)
- [🚀 Como Rodar o Projeto](#-como-rodar-o-projeto)
- [📚 Documentação da API](#-documentação-da-api)
- [🧭 Documentação Técnica](#-documentação-técnica)

---

## 📖 Sobre o Projeto
O **Express Cart** gerencia um carrinho de compras de forma eficiente, integrando dados de uma API externa para disponibilizar produtos.

O sistema foi projetado para **alta performance e escalabilidade**, combinando as seguintes tecnologias:

- **Cache (Redis):** Reduz a latência e a carga sobre a API externa.
- **Banco de dados NoSQL (MongoDB):** Oferece flexibilidade e escalabilidade para a persistência dos dados.
- **Contêineres (Docker):** Simplifica o deploy e garante a consistência do ambiente.

### 🎯 Principais Objetivos
- Oferecer uma experiência de usuário rápida e fluida.
- Minimizar chamadas desnecessárias à API externa através do cache inteligente.
- Facilitar o deploy em ambientes containerizados.

---

## 🛠 Tecnologias Utilizadas
- **Java 21** + **Spring Boot 3.5** → Linguagem e framework base da aplicação.  
  👉 [Documentação oficial](https://docs.oracle.com/en/java/)

- **Lombok** → Elimina código boilerplate, como getters, setters e construtores.  
  👉 [Documentação oficial](https://projectlombok.org/)

- **Redis** → Banco de dados em memória usado como cache para otimizar requisições.  
  👉 [Introdução ao Redis](https://redis.io/docs/)

- **MongoDB** → Banco de dados NoSQL para persistência flexível dos dados.  
  👉 [Documentação oficial](https://www.mongodb.com/docs/)

- **OpenFeign** → Cliente HTTP declarativo que simplifica chamadas a APIs externas.  
  👉 [Documentação oficial](https://spring.io/projects/spring-cloud-openfeign)

- **Docker** → Containerização para um deploy simples e consistente.  
  👉 [Documentação oficial](https://docs.docker.com/)

- **springdoc-openapi (Swagger)** → Documentação interativa da API.  
  👉 [Documentação oficial](https://springdoc.org/)

- **API Externa** → [Platzi Fake Store API](https://fakeapi.platzi.com/), fonte de dados de produtos.

---

## ✨ Funcionalidades
- **Listar Produtos:** Busca produtos disponíveis na API externa.
- **Gerenciar Carrinho:** Controle total para criar, alterar, pagar e limpar o carrinho.
- **Cache Inteligente (Redis):** Reduz o tempo de resposta e o uso de recursos.
- **Persistência com MongoDB:** Armazena o carrinho de forma segura e flexível.
- **Integração com API Externa:** Mantém os produtos atualizados em tempo real.
- **Suporte a Docker:** Facilita a execução em qualquer ambiente.

---

## 📐 Arquitetura do Projeto
```tree
src/
 ├── main/
 │   ├── java/
 │   │   └── br/com/expresscart/
 │   │       ├── client/
 │   │       │   ├── response/
 │   │       │   │   ├── PlatziCategoryResponse
 │   │       │   │   └── PlatziProductResponse
 │   │       │   └── PlatziStoreClient
 │   │       │
 │   │       ├── config/
 │   │       │   ├── ApplicationControllerAdvice
 │   │       │   ├── CacheConfig
 │   │       │   ├── FeignConfig
 │   │       │   └── OpenApiConfig
 │   │       │
 │   │       ├── controller/
 │   │       │   ├── api/docs/
 │   │       │   │   ├── CartApi
 │   │       │   │   └── ProductApi
 │   │       │   ├── request/
 │   │       │   │   ├── validation/
 │   │       │   │   │   └── UniqueProductIds
 │   │       │   │   ├── CartRequest
 │   │       │   │   ├── CartUpdateRequest
 │   │       │   │   ├── PaymentRequest
 │   │       │   │   └── ProductRequest
 │   │       │   ├── response/
 │   │       │   │   ├── CartItemResponse
 │   │       │   │   ├── CartResponse
 │   │       │   │   ├── CategoryResponse
 │   │       │   │   ├── PaymentResponse
 │   │       │   │   └── ProductResponse
 │   │       │   ├── CartController
 │   │       │   └── ProductController
 │   │       │
 │   │       ├── entity/
 │   │       │   ├── Cart
 │   │       │   ├── PaymentMethod
 │   │       │   ├── Product
 │   │       │   └── Status
 │   │       │
 │   │       ├── exception/
 │   │       │   ├── response/
 │   │       │   │   ├── GenericErrorResponse
 │   │       │   │   └── ValidationErrorResponse
 │   │       │   ├── BusinessException
 │   │       │   ├── DataNotFoundException
 │   │       │   └── ExternalServiceException
 │   │       │
 │   │       ├── repository/
 │   │       │   └── CartRepository
 │   │       │
 │   │       ├── service/
 │   │       │   ├── CartService
 │   │       │   └── ProductService
 │   │       │
 │   │       └── ExpressCartApplication
 │   │
 │   └── resources/
 │       └── application.yml
 │
 └── test/
     └── java/
         └── br/com/expresscart/
             └── (testes unitários, de slice web e de integração)
```

> Detalhes de camadas, fluxos e regras de negócio em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## 🚀 Como Rodar o Projeto

### 🔧 Pré-requisitos
- **Docker** (ou instâncias locais de Redis e MongoDB)
- **Java 21+**
- **Maven** (ou utilize `mvnw`)

### ▶️ Rodando a aplicação
```bash
# 1. Clone este repositório
git clone https://github.com/CarlosLonghi/API-ExpressCart

# 2. Acesse a pasta do projeto
cd API-ExpressCart

# 3. Suba os serviços do Docker (Redis e MongoDB)
docker-compose up -d

# 4. Compile e rode a aplicação
./mvnw spring-boot:run
```

---

## 📚 Documentação da API
A documentação interativa da API foi gerada com **Swagger** e pode ser acessada após a inicialização da aplicação.

Ela permite que você **visualize e teste todos os endpoints disponíveis**.

### Como acessar
1. Certifique-se de que a aplicação está rodando (após seguir os passos em ["Como Rodar o Projeto"](#-como-rodar-o-projeto)).
2. Abra seu navegador e navegue até a seguinte URL:

👉 [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### O que você pode fazer na documentação
- **Visualizar Endpoints:** Veja todos os endpoints da API, com seus métodos (GET, POST, PUT, DELETE), parâmetros e descrições.
- **Testar Requisições:** Use a funcionalidade *Try it out* para enviar requisições e ver as respostas em tempo real.
- **Consultar Schemas:** Entenda a estrutura das entidades (DTOs) utilizadas nas requisições e respostas.

---

## 🧭 Documentação Técnica
- [**Arquitetura**](docs/ARCHITECTURE.md): camadas, modelo de dados, máquina de estados do carrinho, regras de negócio, fluxos, contrato HTTP, formato de erros e configuração.
- [**Diagnóstico e Plano de Refatoração**](docs/REFACTORING.md): bugs conhecidos, dívidas técnicas e plano de refatoração em fases.
