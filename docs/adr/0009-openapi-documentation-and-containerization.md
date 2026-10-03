# ADR 0009: Documentação Interativa com OpenAPI/Swagger e Containerização com Docker

- **Status:** Aceito
- **Data:** 2026-09-28
- **Decisores:** Samuel Baldasso

---

## 1. Contexto

Para que outros desenvolvedores, equipes de frontend e serviços parceiros possam consumir o `ledger-core`, precisamos de:
1. **Contrato de API Interativo e Padronizado:** Documentação viva que reflita exatamente o estado dos endpoints, parâmetros, headers obrigatórios (`Idempotency-Key`) e respostas de erro (`ProblemDetail`).
2. **Ambiente Reprodutível e Seguro (Docker):** Capacidade de subir a aplicação completa e o banco de dados PostgreSQL com um único comando, sem atritos de instalação local ou disparidade de versões de JDK.

---

## 2. Decisão

### 1. Documentação OpenAPI 3.0 via Springdoc:
- Adicionamos a biblioteca `springdoc-openapi-starter-webmvc-ui` (v2.6.0).
- Criamos a classe de configuração `OpenApiConfig`, fornecendo metadados do projeto, licença e descrição da arquitetura.
- Anotamos os controllers (`AccountController`, `TransferController`) com as anotações do Swagger (`@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`).
- A interface Swagger UI fica disponível em `/swagger-ui.html` e a especificação JSON em `/v3/api-docs`.

### 2. Multi-Stage Dockerfile Seguro:
- **Build Stage:** Utiliza a imagem `maven:3.9.8-eclipse-temurin-21-alpine` com cache de dependências de dependências (`mvn dependency:go-offline`).
- **Runtime Stage:** Utiliza a imagem enxuta `eclipse-temurin:21-jre-alpine`.
- **Segurança (Non-Root User):** A aplicação roda sob o usuário sem privilégios `appuser:appgroup` (UID 10001), eliminando riscos de escalada de privilégios no host.
- **Flags de JVM para Contêineres:** Configurado `-XX:+UseContainerSupport` e `-XX:MaxRAMPercentage=75.0` para que a JVM respeite os limites de memória de cgroups impostos pelo Docker/Kubernetes.

### 3. Docker Compose (`compose.yaml`):
- Serviço `postgres`: Banco de dados PostgreSQL 16 com volume persistente (`pgdata`) e healthcheck ativo (`pg_isready`).
- Serviço `app`: Constrói a imagem localmente e aguarda o PostgreSQL estar saudável (`condition: service_healthy`) antes de iniciar.

---

## 3. Consequências

### Positivas:
- **Zero Configuração de Ambiente:** Qualquer desenvolvedor pode clonar o repositório e rodar `docker compose up --build` para ter o sistema 100% funcional.
- **Documentação Sempre Sincronizada:** Qualquer alteração em DTOs ou controllers reflete instantaneamente no Swagger UI.

### Trade-offs:
- O build da imagem inicial com Docker requer conexão com a internet para baixar as dependências do Maven e do Alpine.
