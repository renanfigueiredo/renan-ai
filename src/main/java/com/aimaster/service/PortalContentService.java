package com.aimaster.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Carrega todo o conteúdo HTML do portal EVJ (EBD, sermões, cursos, metodologia)
 * e disponibiliza busca por relevância para injeção no contexto da IA.
 */
@Slf4j
@Service
public class PortalContentService {

    private final List<ContentEntry> contentEntries = new ArrayList<>();
    private final Map<String, String> bibleVerses = new LinkedHashMap<>();

    private static final Set<String> STOPWORDS = Set.of(
            "o", "a", "os", "as", "de", "do", "da", "dos", "das", "em", "no", "na",
            "por", "para", "com", "um", "uma", "que", "se", "me", "te", "eu", "ele", "ela",
            "nos", "vos", "meu", "seu", "sua", "como", "mais", "mas", "isso", "este", "esta",
            "esse", "essa", "aqui", "ali", "sobre", "entre", "qual", "quais", "pode", "ser",
            "foi", "sao", "tem", "ter", "muito", "bem", "tambem", "quando", "onde", "porque",
            "nao", "sim", "voce", "gente", "cada", "todos", "todas", "toda", "todo",
            "ainda", "assim", "ate", "aos", "ela", "eles", "elas", "pelo", "pela",
            "pelos", "pelas", "num", "numa", "deste", "desta", "desse", "dessa"
    );

    public record ContentEntry(
            String path,
            String category,    // ebd, sermoes, cursos, metodologia
            String title,
            String summary,     // primeiros ~600 chars do conteúdo
            String fullText,    // texto completo sem HTML
            Set<String> keywords
    ) {}

    @PostConstruct
    public void loadContent() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:static/portal/contents/**/*.html");

            for (Resource resource : resources) {
                try {
                    String html = new BufferedReader(
                            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));

                    String title = extractTitle(html);
                    String plainText = stripHtml(html);
                    String summary = plainText.length() > 600
                            ? plainText.substring(0, 600) + "..."
                            : plainText;
                    Set<String> keywords = extractKeywords(title + " " + plainText);
                    String category = detectCategory(resource);

                    contentEntries.add(new ContentEntry(
                            resource.getFilename(), category, title, summary, plainText, keywords));

                } catch (Exception e) {
                    log.warn("Falha ao carregar conteúdo de {}: {}", resource.getFilename(), e.getMessage());
                }
            }

            log.info("Portal EVJ: {} arquivos de conteúdo indexados para contexto da IA", contentEntries.size());

        } catch (Exception e) {
            log.error("Falha ao escanear conteúdo do portal", e);
        }

        loadVerses();
    }

    /**
     * Carrega versículos bíblicos curados pelo EVJ a partir de verses.js.
     */
    private void loadVerses() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:static/portal/verses.js");
            if (!resource.exists()) {
                log.warn("verses.js não encontrado no classpath");
                return;
            }

            String js = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            // Extrai pares "referência": "texto" do objeto JavaScript
            Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            Matcher matcher = pattern.matcher(js);

            while (matcher.find()) {
                String ref = matcher.group(1);
                String text = matcher.group(2)
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\\\", "\\");
                bibleVerses.put(ref, text);
            }

            log.info("Portal EVJ: {} versículos bíblicos carregados de verses.js", bibleVerses.size());

        } catch (Exception e) {
            log.error("Falha ao carregar versículos de verses.js", e);
        }
    }

    /**
     * Busca versículos bíblicos relevantes para a mensagem do usuário.
     * Retorna texto formatado para injeção no system prompt.
     */
    public String findRelevantVerses(String userMessage, int maxResults) {
        if (bibleVerses.isEmpty() || userMessage == null || userMessage.isBlank()) return "";

        String normalizedQuery = normalize(userMessage);
        Set<String> queryTerms = new HashSet<>(Arrays.asList(normalizedQuery.split("\\s+")));
        queryTerms.removeAll(STOPWORDS);
        queryTerms.removeIf(t -> t.length() < 3);

        if (queryTerms.isEmpty()) return "";

        List<Map.Entry<String, Integer>> scored = new ArrayList<>();

        for (Map.Entry<String, String> entry : bibleVerses.entrySet()) {
            String normalizedRef = normalize(entry.getKey());
            String normalizedText = normalize(entry.getValue());

            int score = 0;
            for (String term : queryTerms) {
                if (normalizedRef.contains(term)) score += 3;
                if (normalizedText.contains(term)) score += 2;
            }

            if (score > 2) {
                scored.add(Map.entry(entry.getKey(), score));
            }
        }

        scored.sort((a, b) -> b.getValue() - a.getValue());

        if (scored.isEmpty()) return "";

        StringBuilder context = new StringBuilder();
        context.append("VERSÍCULOS BÍBLICOS RELEVANTES (curadoria EVJ — use na resposta):\n\n");

        int count = 0;
        for (var se : scored) {
            if (count >= maxResults) break;
            context.append(se.getKey()).append("\n");
            context.append(bibleVerses.get(se.getKey())).append("\n\n");
            count++;
        }

        return context.toString();
    }

    /**
     * Retorna o total de versículos carregados.
     */
    public int getVerseCount() {
        return bibleVerses.size();
    }

    /**
     * Busca conteúdos relevantes para a mensagem do usuário.
     * Retorna texto formatado para injeção no system prompt.
     */
    public String findRelevantContent(String userMessage, int maxResults) {
        if (contentEntries.isEmpty() || userMessage == null || userMessage.isBlank()) return "";

        String normalizedQuery = normalize(userMessage);
        Set<String> queryTerms = new HashSet<>(Arrays.asList(normalizedQuery.split("\\s+")));
        queryTerms.removeAll(STOPWORDS);
        queryTerms.removeIf(t -> t.length() < 3);

        if (queryTerms.isEmpty()) return "";

        List<ScoredEntry> scored = new ArrayList<>();
        for (ContentEntry entry : contentEntries) {
            int score = 0;
            String normalizedTitle = normalize(entry.title());
            String normalizedFull = normalize(entry.fullText());

            for (String term : queryTerms) {
                // título = peso alto
                if (normalizedTitle.contains(term)) score += 5;
                // keyword set = peso médio
                if (entry.keywords().contains(term)) score += 3;
                // corpo do texto = peso baixo (mas confirma relevância)
                if (normalizedFull.contains(term)) score += 1;
            }

            if (score > 2) { // threshold mínimo para evitar ruído
                scored.add(new ScoredEntry(entry, score));
            }
        }

        scored.sort((a, b) -> b.score - a.score);

        if (scored.isEmpty()) return "";

        StringBuilder context = new StringBuilder();
        context.append("CONTEÚDO RELEVANTE DO PORTAL EVJ (use para enriquecer sua resposta):\n\n");

        int count = 0;
        for (ScoredEntry se : scored) {
            if (count >= maxResults) break;
            ContentEntry ce = se.entry;
            context.append("── ").append(categoryLabel(ce.category()))
                    .append(": ").append(ce.title()).append(" ──\n");
            context.append(ce.summary()).append("\n\n");
            count++;
        }

        return context.toString();
    }

    /**
     * Retorna o catálogo completo de conteúdos disponíveis no portal,
     * organizado por categoria. Usado para o system prompt base.
     */
    public String getContentCatalog() {
        Map<String, List<ContentEntry>> byCategory = contentEntries.stream()
                .collect(Collectors.groupingBy(ContentEntry::category));

        StringBuilder catalog = new StringBuilder();

        // EBD
        List<ContentEntry> ebd = byCategory.getOrDefault("ebd", List.of());
        if (!ebd.isEmpty()) {
            catalog.append("ESTUDOS BÍBLICOS (EBD) disponíveis no portal: ")
                    .append(ebd.size()).append(" lições\n");
            for (ContentEntry e : ebd) {
                catalog.append("  • ").append(e.title()).append("\n");
            }
            catalog.append("\n");
        }

        // Sermões / Aprendizados
        List<ContentEntry> sermoes = byCategory.getOrDefault("sermoes", List.of());
        if (!sermoes.isEmpty()) {
            catalog.append("APRENDIZADOS E DISCUSSÕES disponíveis no portal: ")
                    .append(sermoes.size()).append(" temas\n");
            for (ContentEntry e : sermoes) {
                catalog.append("  • ").append(e.title()).append("\n");
            }
            catalog.append("\n");
        }

        // Cursos
        List<ContentEntry> cursos = byCategory.getOrDefault("cursos", List.of());
        if (!cursos.isEmpty()) {
            catalog.append("CURSOS disponíveis no portal: ")
                    .append(cursos.size()).append(" aulas/páginas\n");
            for (ContentEntry e : cursos) {
                catalog.append("  • ").append(e.title()).append("\n");
            }
            catalog.append("\n");
        }

        // Metodologia
        List<ContentEntry> metodo = byCategory.getOrDefault("metodologia", List.of());
        if (!metodo.isEmpty()) {
            catalog.append("GUIA PRÁTICO / METODOLOGIA disponível no portal\n");
            for (ContentEntry e : metodo) {
                catalog.append("  • ").append(e.title()).append("\n");
            }
            catalog.append("\n");
        }

        // Versículos
        if (!bibleVerses.isEmpty()) {
            catalog.append("VERSÍCULOS BÍBLICOS: ").append(bibleVerses.size())
                    .append(" versículos curados pelo EVJ disponíveis para consulta\n\n");
        }

        return catalog.toString();
    }

    // ── Utilidades internas ──

    private record ScoredEntry(ContentEntry entry, int score) {}

    private String detectCategory(Resource resource) {
        try {
            String uri = resource.getURI().toString();
            if (uri.contains("/ebd/")) return "ebd";
            if (uri.contains("/sermoes/")) return "sermoes";
            if (uri.contains("/cursos/")) return "cursos";
            if (uri.contains("metodologia")) return "metodologia";
        } catch (Exception ignored) {}
        return "outro";
    }

    private String categoryLabel(String category) {
        return switch (category) {
            case "ebd" -> "EBD";
            case "sermoes" -> "Aprendizado";
            case "cursos" -> "Curso";
            case "metodologia" -> "Guia Prático";
            default -> "Portal";
        };
    }

    private String extractTitle(String html) {
        // Tenta classe sermon-title primeiro
        Pattern p = Pattern.compile("<h1[^>]*class=\"[^\"]*sermon-title[^\"]*\"[^>]*>(.*?)</h1>", Pattern.DOTALL);
        Matcher m = p.matcher(html);
        if (m.find()) return stripHtml(m.group(1)).trim();

        // Tenta qualquer h1
        p = Pattern.compile("<h1[^>]*>(.*?)</h1>", Pattern.DOTALL);
        m = p.matcher(html);
        if (m.find()) return stripHtml(m.group(1)).trim();

        // Tenta tag title
        p = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL);
        m = p.matcher(html);
        if (m.find()) return stripHtml(m.group(1)).trim();

        return "Sem título";
    }

    private String stripHtml(String html) {
        String text = html.replaceAll("(?s)<script[^>]*>.*?</script>", " ");
        text = text.replaceAll("(?s)<style[^>]*>.*?</style>", " ");
        text = text.replaceAll("<[^>]+>", " ");
        text = text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&mdash;", "—")
                .replace("&ndash;", "–").replace("&nbsp;", " ");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    private String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> extractKeywords(String text) {
        String normalized = normalize(text);
        return Arrays.stream(normalized.split("\\s+"))
                .filter(w -> w.length() >= 3)
                .filter(w -> !STOPWORDS.contains(w))
                .collect(Collectors.toSet());
    }
}
