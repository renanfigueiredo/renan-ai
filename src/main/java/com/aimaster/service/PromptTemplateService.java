package com.aimaster.service;

import com.aimaster.model.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PromptTemplateService {

    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    public PromptTemplateService() {
        initBuiltInTemplates();
    }

    private void initBuiltInTemplates() {
        // CODING
        addBuiltIn("Code Review", "Review and improve the following code. Identify bugs, security issues, performance problems, and suggest refactoring:", "CODING", "TEXT", "bi-code-slash");
        addBuiltIn("Debug Assistant", "Analyze this error/bug and provide a detailed solution with explanation:\n\n[PASTE ERROR HERE]", "CODING", "TEXT", "bi-bug");
        addBuiltIn("Unit Test Generator", "Generate comprehensive unit tests for the following code. Include edge cases, happy path, and error scenarios:", "CODING", "TEXT", "bi-check-circle");
        addBuiltIn("Architecture Design", "Design a scalable architecture for: [DESCRIBE YOUR SYSTEM]. Include diagrams description, components, data flow, and technology choices.", "CODING", "TEXT", "bi-diagram-3");
        addBuiltIn("SQL Query Builder", "Write an optimized SQL query for: [DESCRIBE YOUR NEED]. Include indexes suggestions and explain the query.", "CODING", "TEXT", "bi-database");
        addBuiltIn("API Documentation", "Generate complete API documentation in OpenAPI/Swagger format for the following endpoint:", "CODING", "TEXT", "bi-file-code");

        // WRITING
        addBuiltIn("Blog Post Writer", "Write an engaging, SEO-optimized blog post about: [TOPIC]. Include an attention-grabbing title, introduction, 5 key sections, and a strong conclusion. Tone: professional yet conversational.", "WRITING", "TEXT", "bi-pen");
        addBuiltIn("Email Composer", "Write a professional email for: [PURPOSE]. Recipient: [RECIPIENT ROLE]. Tone: [formal/casual]. Include subject line.", "WRITING", "TEXT", "bi-envelope");
        addBuiltIn("Social Media Post", "Create 5 engaging social media posts for [PLATFORM] about: [TOPIC]. Include hashtags, emojis, and call-to-action.", "WRITING", "TEXT", "bi-share");
        addBuiltIn("Story Generator", "Write a compelling short story about: [THEME/CONCEPT]. Genre: [GENRE]. Length: ~1000 words. Include vivid characters and an unexpected twist.", "WRITING", "TEXT", "bi-book");
        addBuiltIn("Resume Builder", "Create a professional resume for a [JOB TITLE] position. Include: summary, skills, experience format, and achievements section.", "WRITING", "TEXT", "bi-file-person");

        // ANALYSIS
        addBuiltIn("Data Analyst", "Analyze the following data and provide insights, trends, patterns, and actionable recommendations:\n\n[PASTE DATA]", "ANALYSIS", "TEXT", "bi-graph-up");
        addBuiltIn("SWOT Analysis", "Perform a detailed SWOT analysis for: [COMPANY/PRODUCT/IDEA]. Be specific and actionable.", "ANALYSIS", "TEXT", "bi-grid");
        addBuiltIn("Competitive Analysis", "Analyze the competitive landscape for [PRODUCT/COMPANY] against competitors. Include strengths, weaknesses, market positioning, and opportunities.", "ANALYSIS", "TEXT", "bi-bar-chart");
        addBuiltIn("Legal Document Review", "Review the following agreement/contract and identify: potential issues, unfair clauses, missing protections, and suggest amendments:\n\n[PASTE DOCUMENT]", "ANALYSIS", "TEXT", "bi-file-text");
        addBuiltIn("Risk Assessment", "Conduct a comprehensive risk assessment for: [PROJECT/DECISION]. Categorize risks by probability and impact, and suggest mitigation strategies.", "ANALYSIS", "TEXT", "bi-shield-exclamation");

        // CREATIVE
        addBuiltIn("Logo Concept Generator", "Design concept description for a logo for: [BRAND NAME]. Industry: [INDUSTRY]. Style: [modern/classic/playful]. Colors: [COLORS]. Include visual metaphors.", "CREATIVE", "TEXT", "bi-palette");
        addBuiltIn("Script Writer", "Write a compelling script for a [duration]-minute [YouTube/podcast/presentation] about: [TOPIC]. Include intro hook, main points, transitions, and outro.", "CREATIVE", "TEXT", "bi-camera-video");
        addBuiltIn("Product Description", "Write 3 versions of a compelling product description for: [PRODUCT]. Version 1: SEO-optimized. Version 2: Emotional/storytelling. Version 3: Technical/features-focused.", "CREATIVE", "TEXT", "bi-shop");

        // BUSINESS
        addBuiltIn("Business Plan", "Create a comprehensive business plan for: [BUSINESS IDEA]. Include executive summary, market analysis, revenue model, go-to-market strategy, and financial projections.", "BUSINESS", "TEXT", "bi-briefcase");
        addBuiltIn("Meeting Summarizer", "Summarize the following meeting notes into: key decisions, action items (with owners), risks identified, and next steps:\n\n[PASTE NOTES]", "BUSINESS", "TEXT", "bi-calendar-check");
        addBuiltIn("Pitch Deck Outline", "Create a compelling investor pitch deck outline for: [STARTUP/PRODUCT]. Include all key slides with bullet points for each.", "BUSINESS", "TEXT", "bi-presentation");

        // IMAGE PROMPTS
        addBuiltIn("Retrato Fotorrealista", "Retrato ultra-realista de [SUJEITO], fotografia profissional, lente 85mm, fundo desfocado, iluminação de estúdio, resolução 8K, altamente detalhado, fotorrealista", "IMAGEM", "IMAGE", "bi-person");
        addBuiltIn("Paisagem Cinematográfica", "Paisagem cinematográfica épica de [CENA], luz dourada do pôr do sol, nuvens dramáticas, lente grande angular, resolução 8K, fotografia profissional, hiperrrealista", "IMAGEM", "IMAGE", "bi-mountains");
        addBuiltIn("Fotografia de Produto", "Fotografia profissional de produto de [PRODUTO], fundo branco, iluminação de estúdio, estilo fotográfico comercial, 8K, altamente detalhado, fotorrealista", "IMAGEM", "IMAGE", "bi-camera");
        addBuiltIn("Arte Fantasia", "[SUJEITO/CENA] em estilo de arte fantasia épica, altamente detalhado, iluminação dramática, atmosfera mágica, arte conceitual, resolução 4K", "IMAGEM", "IMAGE", "bi-magic");
        addBuiltIn("Design de Logo", "Logotipo minimalista e profissional para [EMPRESA], design vetorial limpo, paleta de cores [COR], tipografia moderna, fundo branco, identidade visual corporativa", "IMAGEM", "IMAGE", "bi-vector-pen");

        // VIDEO PROMPTS
        addBuiltIn("Vídeo Cinematográfico", "Vídeo cinematográfico de [duração] segundos de [CENA], movimento de câmera suave, iluminação dramática, qualidade ultra HD, cinematografia profissional", "VIDEO", "VIDEO", "bi-film");
        addBuiltIn("Demo de Produto", "Vídeo de demonstração profissional mostrando [PRODUTO], detalhes em close, transições suaves, iluminação de estúdio, qualidade comercial", "VIDEO", "VIDEO", "bi-play-circle");
        addBuiltIn("Time-lapse da Natureza", "Lindo time-lapse de [CENA DA NATUREZA], mudanças dramáticas no céu, movimento suave, ultra HD, visuais deslumbrantes", "VIDEO", "VIDEO", "bi-sunset");
    }

    private void addBuiltIn(String name, String content, String category, String type, String icon) {
        PromptTemplate template = PromptTemplate.builder()
                .name(name)
                .content(content)
                .category(category)
                .type(type)
                .icon(icon)
                .isBuiltIn(true)
                .build();
        templates.put(template.getId(), template);
    }

    public List<PromptTemplate> getAllTemplates() {
        return templates.values().stream()
                .sorted(Comparator.comparing(PromptTemplate::getName))
                .toList();
    }

    public List<PromptTemplate> getTemplatesByType(String type) {
        return templates.values().stream()
                .filter(t -> type.equals(t.getType()))
                .sorted(Comparator.comparing(PromptTemplate::getName))
                .toList();
    }

    public List<PromptTemplate> getTemplatesByCategory(String category) {
        return templates.values().stream()
                .filter(t -> category.equals(t.getCategory()))
                .sorted(Comparator.comparing(PromptTemplate::getName))
                .toList();
    }

    public PromptTemplate saveCustomTemplate(PromptTemplate template) {
        template.setBuiltIn(false);
        templates.put(template.getId(), template);
        return template;
    }

    public void deleteTemplate(String id) {
        PromptTemplate t = templates.get(id);
        if (t != null && !t.isBuiltIn()) {
            templates.remove(id);
        }
    }

    public Optional<PromptTemplate> findById(String id) {
        return Optional.ofNullable(templates.get(id));
    }

    public void toggleFavorite(String id) {
        PromptTemplate t = templates.get(id);
        if (t != null) {
            t.setFavorite(!t.isFavorite());
        }
    }

    public List<String> getCategories() {
        return templates.values().stream()
                .map(PromptTemplate::getCategory)
                .distinct()
                .sorted()
                .toList();
    }
}
