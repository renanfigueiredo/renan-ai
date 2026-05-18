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
6. **Tasks em memória** em `EvjAiApiController` (`ConcurrentHashMap`) — perde
   tudo no restart. Persistir no BD com TTL.
7. **Rate limit no `requestPasswordReset`** (cooldown de 5 min).
8. **Retry** em `EmailService.send()` (Mailgun cair = usuário não recebe email
    de aprovação/reset).

---

## 5. Experiência de IA (Pacotes A + B + C)

Pesquisa de padrões em ChatGPT, Claude.ai, Perplexity, Logos, Magisterium AI
sintetizada em 4 pacotes. Aplicado nesta branch: **A + B + C parcial**. Pacote
**D** e **RAG semântico real** ficam como Fase 2.

### Aplicado nesta branch — Pacote A: Quick wins UX no chat

- **Slash commands** (`/sermao`, `/devocional`, `/versiculo`, `/ebd`,
  `/exegese`, `/duvida`, `/aconselhar`, `/melhorar`). Menu de auto-complete
  com navegação por teclado (↑↓ Enter Esc), substitui o texto do textarea
  por um template parametrizado e seleciona o primeiro `{placeholder}`.
- **Hover actions** em cada resposta do assistente:
  - Copiar conteúdo
  - Ler em voz alta (Web Speech API, voz pt-BR — sem custo de Polly)
  - Regenerar (re-envia a última pergunta)
  - Exportar como Markdown (download `.md`)
- **Stop generation**: botão "parar" aparece durante streaming, usa
  `AbortController` no fetch SSE.
- **Follow-up suggestions**: depois que cada resposta termina, o frontend
  chama `POST /api/chat/followups` que pede ao Claude 3 perguntas curtas
  para aprofundar o estudo. Renderizadas como chips clicáveis abaixo da
  resposta — clicar dispara nova mensagem automaticamente.
- **Histórico agrupado por data** na sidebar: "Fixadas / Hoje / Ontem /
  7 dias / 30 dias / Mais antigas". Funciona com itens server-rendered
  (Thymeleaf) e com renderização via JS (busca).

Backend novo:
- `TextGenerationService.generateFollowups()` — pede 3 perguntas em JSON
  array, sanitiza, retorna lista vazia em qualquer falha (best-effort).
- `POST /api/chat/followups` em `ChatController`.

Frontend novo:
- `static/js/chat-enhancements.js` (auto-init, IIFE, sem depender de jQuery)
- `static/css/chat-enhancements.css`

### Aplicado nesta branch — Pacote B: Engajamento diário + home da IA

- **Versículo do dia** determinístico (mesmo dia → mesmo versículo, cria
  hábito devocional estável). Usa o pool curado em `verses.js`.
- **4 prompts sugeridos** rotativos por dia, em buckets pré-definidos
  (sermão, devocional, exegese, aconselhamento, EBD…). 7 buckets = quase
  uma rotação semanal.
- **Card visual** na home do `/chat` quando a conversa é nova. Clicar em
  qualquer chip preenche o textarea e dá foco — não envia automaticamente
  para o usuário ainda poder editar.

Backend novo:
- `PortalContentService.getVerseOfTheDay()` (determinístico via dia do ano).
- `GET /api/chat/daily` em `ChatController` (devolve verse + suggestions +
  data ISO).
- 7 buckets de sugestões em constante `SUGGESTION_BUCKETS`.

Frontend novo:
- `static/js/chat-daily.js`

### Aplicado nesta branch — Pacote C parcial: Custom Instructions

- 3 novos campos em `UserPreference` (Bean + DB):
  - `bibleVersion` (ARA, ARC, NAA, NVI, ACF, NTLH, KJA)
  - `userRole` (PASTOR, LIDER, PROFESSOR_EBD, JOVEM, MEMBRO, INTERESSADO)
  - `denomination` (texto livre, 64 chars)
- Modal de preferências (`layout.html`) ganhou os 3 novos campos com
  selects/input específicos.
- `UserPreferenceController` aceita e devolve os novos campos.
- `UserPreferenceService.buildAiContext()` injeta tudo no system prompt:
  papel calibra profundidade, denominação influencia exemplos, versão
  da Bíblia indica qual tradução citar.
- Schema novo é compatível com a configuração `ddl-auto=update` em dev
  (em prod com `validate`, será preciso uma migration manual quando
  você decidir promover).

### Pendente — Fase 2 (Pacote D + RAG real)

Não foi implementado nesta branch porque mexem em DB e merecem uma sessão
dedicada com você acompanhando os testes em produção.

#### Pacote D — Projects/Pastas + Artifacts

Modelo proposto:
- Tabela `project` (`id`, `userId`, `name`, `systemPrompt`, `createdAt`).
- Tabela `project_file` (`id`, `projectId`, `filename`, `mimeType`,
  `s3Key`, `extractedText`).
- `Conversation` ganha FK opcional `projectId` para herdar o system
  prompt e os arquivos do projeto.
- Tabela `artifact` para canvas editável (esboço de sermão), com
  `revision` para histórico de versões.
- Endpoint `GET /artifacts/{uuid}` público (read-only, para compartilhar
  esboço no WhatsApp da igreja).
- Export PDF via `openhtmltopdf-pdfbox` (já está no `pom.xml`).

#### RAG semântico real (substitui o TF-IDF atual)

Hoje `PortalContentService.findRelevantContent()` faz busca por interseção
de tokens normalizados — funciona para títulos/keywords mas perde
sinônimos, paráfrases e contexto. A proposta:

1. **Embeddings com Bedrock Titan** (`amazon.titan-embed-text-v2:0`) ou
   `cohere.embed-multilingual-v3` — ambos baratos.
2. **Persistência**: extensão `pgvector` no PostgreSQL (Heroku Postgres
   suporta), tabela `content_chunk` com coluna `embedding vector(1024)`.
3. **Pipeline de indexação** (job `@Scheduled` ou comando CLI):
   - Lê `static/portal/contents/**/*.html`
   - Chunk semântico (800-1000 tokens, overlap 150)
   - Gera embedding e salva
4. **Corpus reformado adicional** (curado, sem violar copyright):
   - Confissão de Fé de Westminster (domínio público)
   - Catecismos Maior e Breve (DP)
   - Cânones de Dort, Confissão Belga, Heidelberg (DP)
   - Institutas de Calvino — tradução PT-BR (Cunha Lima 1985, ainda
     sob copyright; usar texto inglês traduzido em runtime ou indexar
     resumos próprios)
   - Comentários Matthew Henry — tradução PT-BR pública
5. **Busca**: `SELECT ... ORDER BY embedding <-> :queryEmbedding LIMIT 5`
   (cosine distance). Retorna chunks com metadata `{autor, obra, locator}`
   para citação.
6. **Citação inline**: o backend marca cada chunk com ID único, system
   prompt instrui o modelo a citar `[1]`, `[2]` etc, e o frontend
   renderiza como tooltip clicável (estilo Magisterium AI).

Estimativa: ~3-4 dias de implementação + 1 dia de tuning.

#### Outras ideias guardadas para fase 2

- **Bedrock Guardrails** com denied topics doutrinários
- **Voz** (Polly Neural pt-BR + Transcribe — STT completa, não só Web Speech)
- **Compartilhar conversa via link público** (`/c/{uuid}` read-only)
- **Web search** integrado quando a pergunta for sobre evento/notícia
- **Memória persistente** entre conversas (extração de fatos do usuário,
  UI "Manage memories" tipo ChatGPT)

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
src/main/resources/static/css/chat-enhancements.css
src/main/resources/static/js/chat-enhancements.js
src/main/resources/static/js/chat-daily.js
AUDITORIA.md   (este arquivo)
```
