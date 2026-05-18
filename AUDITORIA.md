# Auditoria do Projeto EVJ AI / Estação Vida Jovem

Branch: `melhorias/analise-completa-2026-05`
Data: 2026-05-17

Esta auditoria cobre 4 áreas: backend Java/Spring, segurança, conteúdo teológico
(EBD/sermões/cursos) e frontend/SEO. O que foi corrigido nesta branch está
listado em **"Aplicado nesta branch"**. O resto fica como roadmap.

---

## 1. Conteúdo teológico

### Aplicado nesta branch

- **Aula 01 — Reforma:** corrigido erro histórico relevante.
  - Antes: *"313 d.C. — Constantino decretou a junção do Cristianismo com a política"*
    e *"607 — Figura do Papa inventada pelo imperador"*.
  - Depois: distinção entre Édito de Milão (313, legalização) e Édito de
    Tessalônica (380, religião oficial); papado descrito como **consolidação
    gradual** entre os séc. 5–7 (Leão Magno, Gregório Magno), não invenção em 607.
- **Linha do tempo da Deforma**: datas pontuais (375, 593, 600, 607...) substituídas
  por períodos historicamente defensáveis ("séc. 6", "séc. 6-7", "1215 — IV Latrão",
  "1439 — Concílio de Florença").
- **Aulas 01 a 06 dos 5 Solas**: cada uma agora tem
  - Seção **"Como isso muda meu dia a dia?"** com 4-5 aplicações práticas concretas.
  - Seção **"Vozes da Reforma"** com 2 citações (Lutero, Calvino, Spurgeon, Confissão de
    Westminster, ou texto bíblico) por aula.
- **Aula 01 — Reforma**: dinâmica recebeu uma pergunta-desafio sobre "indulgências
  modernas" para conectar com o cotidiano da igreja brasileira.
- **Curso Namoro com Propósito (Aula 01)**: nova seção
  **"Conexão com a Reforma"** que aplica os 5 Solas ao namoro cristão.

### Pendente (roadmap)

- Adicionar perguntas de reflexão **intercaladas** ao longo de cada aula (não apenas
  na dinâmica final).
- Infográfico que conecte os 5 Solas convergindo em Cristo.
- Glossário de termos técnicos (transubstanciação, infalibilidade, predestinação...).
- Acessibilidade: alt-text em ícones e auditoria de contraste WCAG AA.

---

## 2. Segurança

### Aplicado nesta branch

- **`SecurityHeadersFilter`** novo: adiciona em todas as respostas
  - `X-Frame-Options: DENY` (anti-clickjacking)
  - `X-Content-Type-Options: nosniff`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Permissions-Policy` desligando câmera/mic/geo
  - `Content-Security-Policy` permitindo apenas CDNs realmente usados (Bootstrap,
    Google Fonts, jsDelivr, cdnjs).
- **Cookie `GUEST_TOKEN`**: agora com `SameSite=Lax` (mitiga CSRF cross-site).
- **`DataInitializer`**: senha admin **não é mais hardcoded** (`Evj_inven.`).
  Agora lê `ADMIN_INITIAL_PASSWORD`. Se vazia, gera senha aleatória forte e
  imprime no log apenas na criação. `ADMIN_EMAIL` também configurável.
- **Profiles** `dev` e `prod` separados:
  - `prod`: `ddl-auto=validate` (em vez de `update`), logging em `INFO`,
    cookies `Secure+HttpOnly+SameSite=Lax`, `server.compression`, actuator
    health/info expostos.
  - `dev`: `ddl-auto=update`, logging `DEBUG`, devtools.
  - Default em `application.properties` agora é seguro (`validate`/`INFO`).
- **`@EnableMethodSecurity`** habilitado em `SecurityConfig` + `@PreAuthorize("hasRole('ADMIN')")`
  defensivo nos endpoints `/api/admin/agenda/**` (defesa em profundidade — não
  depende mais só do mapping no `SecurityConfig`).
- **`GlobalApiExceptionHandler`** novo: stack traces nunca mais vazam para clientes
  REST. Mapeia `MethodArgumentNotValidException`, `ConstraintViolationException`,
  `IllegalArgumentException`, `AccessDeniedException`, `AuthenticationException`,
  `MaxUploadSizeExceededException` e fallback genérico.
- **IDOR em `/api/image/{id}`**: `deleteImage`, `toggleFavorite` e `downloadImage`
  agora validam ownership (`findByIdAndUserId`). Antes, qualquer usuário podia
  apagar/baixar imagens de outro pelo ID.

### Pendente (roadmap)

- **Rate limiting** em `/api/v1/**`, `/login`, `/forgot-password` (Bucket4j).
- **Validação MIME real** em uploads (não só tamanho).
- **Sanitização de prompt** ao chamar Bedrock (prompt injection).
- **CSRF** em endpoints de admin: hoje está exempto pelo padrão `/api/**`.
- **Testes** — não existe `src/test/`. Stack mínimo: Spring Boot Test +
  Testcontainers Postgres.
- **Flyway/Liquibase** para gerenciar schema em produção em vez de
  `ddl-auto`.

---

## 3. Frontend / SEO / PWA

### Aplicado nesta branch

- **`/manifest.webmanifest`** criado — app instalável (PWA básico).
- **`/robots.txt`** criado — bloqueia `/admin`, `/api`, `/actuator`, áreas
  autenticadas; permite portal e conteúdos.
- **`/sitemap.xml`** com todas as ~45 páginas estáticas do portal (EBD,
  sermões, cursos, agenda, metodologia).
- **`portal/index.html`**:
  - Open Graph (`og:title`, `og:description`, `og:image`, `og:url`).
  - Twitter Card.
  - JSON-LD `Organization`.
  - `theme-color`, `keywords`, `author`.
  - Link para `manifest.webmanifest`.
- **`templates/layout.html`** (app autenticado): `theme-color`,
  `description`, `robots: noindex,nofollow` (não deve ser indexado),
  link para manifest.

### Pendente (roadmap)

- **Service Worker** para offline básico.
- **Minificação** de CSS/JS (~438 KB sem compressão; com `server.compression`
  agora habilitado em prod, parte disso é mitigado em runtime).
- **Sanitização XSS** em `index-scripts.js` linha 336 (`innerHTML` sem escape).
- **Hierarquia de headings** quebrada em portal/index.html (h1 → h3).
- **Lazy loading** em imagens (`loading="lazy"`).
- **`logo_evj.jpg` em WebP** (94 KB → ~40 KB).

---

## 4. Backend Java / Spring

### Aplicado nesta branch

- Tudo de segurança listado na seção 2.
- `application.properties` reorganizado em 3 arquivos: base (defaults seguros),
  `application-dev.properties`, `application-prod.properties`.

### Pendente (roadmap, em ordem de impacto)

1. **Criar `src/test/`** — zero testes hoje. Começar por `UserServiceTest`,
   `AuthControllerTest`, `SecurityConfigTest`.
2. **Migrations Flyway** em vez de `ddl-auto`.
3. **Refatorar `TextGenerationService`** (950 linhas, 5 modelos no mesmo arquivo) —
   `BedrockModelInvoker` interface + uma classe por modelo.
4. **Cache** (`@Cacheable`) em `PortalContentService` — chamado a cada chat.
5. **`Conversation.messages`** está `EAGER` — mudar para `LAZY` + repositório
   paginado.
6. **Spring Boot 4.0.5 → 3.3.x LTS** (4.x ainda é cedo demais para produção).
7. **Java 25 → Java 21 LTS**.
8. **Tasks em memória** em `EvjAiApiController` (`ConcurrentHashMap`) — perde
   tudo no restart. Persistir no BD com TTL.
9. **Rate limit no `requestPasswordReset`** (cooldown de 5 min).
10. **Retry** em `EmailService.send()` (Mailgun cair = usuário não recebe email
    de aprovação/reset).

---

## Como rodar com profile prod

```bash
SPRING_PROFILES_ACTIVE=prod \
ADMIN_INITIAL_PASSWORD=algo-bem-forte \
mvn spring-boot:run
```

Em Heroku: `heroku config:set SPRING_PROFILES_ACTIVE=prod ADMIN_INITIAL_PASSWORD=...`.

## Arquivos novos criados nesta branch

```
src/main/java/com/aimaster/config/SecurityHeadersFilter.java
src/main/java/com/aimaster/controller/api/GlobalApiExceptionHandler.java
src/main/resources/application-dev.properties
src/main/resources/application-prod.properties
src/main/resources/static/manifest.webmanifest
src/main/resources/static/robots.txt
src/main/resources/static/sitemap.xml
AUDITORIA.md   (este arquivo)
```
