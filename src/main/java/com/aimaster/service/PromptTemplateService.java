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
        // PROGRAMAÇÃO
        addBuiltIn("Revisão de Código", "Revise e melhore o seguinte código. Identifique bugs, problemas de segurança, problemas de desempenho e sugira refatorações:", "PROGRAMAÇÃO", "TEXT", "bi-code-slash");
        addBuiltIn("Assistente de Debug", "Analise este erro/bug e forneça uma solução detalhada com explicação:\n\n[COLE O ERRO AQUI]", "PROGRAMAÇÃO", "TEXT", "bi-bug");
        addBuiltIn("Gerador de Testes Unitários", "Gere testes unitários completos para o seguinte código. Inclua casos extremos, caminho feliz e cenários de erro:", "PROGRAMAÇÃO", "TEXT", "bi-check-circle");
        addBuiltIn("Design de Arquitetura", "Projete uma arquitetura escalável para: [DESCREVA SEU SISTEMA]. Inclua descrição dos diagramas, componentes, fluxo de dados e escolhas tecnológicas.", "PROGRAMAÇÃO", "TEXT", "bi-diagram-3");
        addBuiltIn("Construtor de Queries SQL", "Escreva uma query SQL otimizada para: [DESCREVA SUA NECESSIDADE]. Inclua sugestões de índices e explique a query.", "PROGRAMAÇÃO", "TEXT", "bi-database");
        addBuiltIn("Documentação de API", "Gere documentação completa de API no formato OpenAPI/Swagger para o seguinte endpoint:", "PROGRAMAÇÃO", "TEXT", "bi-file-code");

        // REDAÇÃO
        addBuiltIn("Escritor de Post para Blog", "Escreva um post de blog envolvente e otimizado para SEO sobre: [TÓPICO]. Inclua um título chamativo, introdução, 5 seções principais e uma conclusão forte. Tom: profissional, porém conversacional.", "REDAÇÃO", "TEXT", "bi-pen");
        addBuiltIn("Compositor de E-mail", "Escreva um e-mail profissional para: [FINALIDADE]. Destinatário: [CARGO DO DESTINATÁRIO]. Tom: [formal/informal]. Inclua o assunto.", "REDAÇÃO", "TEXT", "bi-envelope");
        addBuiltIn("Post para Redes Sociais", "Crie 5 posts envolventes para [PLATAFORMA] sobre: [TÓPICO]. Inclua hashtags, emojis e chamada para ação.", "REDAÇÃO", "TEXT", "bi-share");
        addBuiltIn("Gerador de Histórias", "Escreva uma história curta e envolvente sobre: [TEMA/CONCEITO]. Gênero: [GÊNERO]. Extensão: ~1000 palavras. Inclua personagens marcantes e uma reviravolta inesperada.", "REDAÇÃO", "TEXT", "bi-book");
        addBuiltIn("Criador de Currículo", "Crie um currículo profissional para a posição de [CARGO]. Inclua: resumo, habilidades, formato de experiência e seção de conquistas.", "REDAÇÃO", "TEXT", "bi-file-person");

        // ANÁLISE
        addBuiltIn("Analista de Dados", "Analise os seguintes dados e forneça insights, tendências, padrões e recomendações práticas:\n\n[COLE OS DADOS AQUI]", "ANÁLISE", "TEXT", "bi-graph-up");
        addBuiltIn("Análise SWOT", "Realize uma análise SWOT detalhada para: [EMPRESA/PRODUTO/IDEIA]. Seja específico e prático.", "ANÁLISE", "TEXT", "bi-grid");
        addBuiltIn("Análise Competitiva", "Analise o cenário competitivo de [PRODUTO/EMPRESA] em relação aos concorrentes. Inclua pontos fortes, fracos, posicionamento de mercado e oportunidades.", "ANÁLISE", "TEXT", "bi-bar-chart");
        addBuiltIn("Revisão de Documento Legal", "Revise o seguinte contrato/acordo e identifique: problemas potenciais, cláusulas injustas, proteções ausentes e sugira alterações:\n\n[COLE O DOCUMENTO AQUI]", "ANÁLISE", "TEXT", "bi-file-text");
        addBuiltIn("Avaliação de Riscos", "Realize uma avaliação de riscos completa para: [PROJETO/DECISÃO]. Categorize os riscos por probabilidade e impacto, e sugira estratégias de mitigação.", "ANÁLISE", "TEXT", "bi-shield-exclamation");

        // CRIATIVO
        addBuiltIn("Gerador de Conceito de Logo", "Descreva o conceito de um logo para: [NOME DA MARCA]. Setor: [SETOR]. Estilo: [moderno/clássico/divertido]. Cores: [CORES]. Inclua metáforas visuais.", "CRIATIVO", "TEXT", "bi-palette");
        addBuiltIn("Roteirista", "Escreva um roteiro envolvente para um [YouTube/podcast/apresentação] de [duração] minutos sobre: [TÓPICO]. Inclua gancho inicial, pontos principais, transições e encerramento.", "CRIATIVO", "TEXT", "bi-camera-video");
        addBuiltIn("Descrição de Produto", "Escreva 3 versões de uma descrição de produto envolvente para: [PRODUTO]. Versão 1: Otimizada para SEO. Versão 2: Emocional/narrativa. Versão 3: Técnica/focada em recursos.", "CRIATIVO", "TEXT", "bi-shop");

        // NEGÓCIOS
        addBuiltIn("Plano de Negócios", "Crie um plano de negócios completo para: [IDEIA DE NEGÓCIO]. Inclua sumário executivo, análise de mercado, modelo de receita, estratégia de entrada no mercado e projeções financeiras.", "NEGÓCIOS", "TEXT", "bi-briefcase");
        addBuiltIn("Resumidor de Reunião", "Resuma as seguintes anotações de reunião em: decisões principais, itens de ação (com responsáveis), riscos identificados e próximos passos:\n\n[COLE AS ANOTAÇÕES AQUI]", "NEGÓCIOS", "TEXT", "bi-calendar-check");
        addBuiltIn("Estrutura de Pitch Deck", "Crie uma estrutura convincente de pitch deck para investidores para: [STARTUP/PRODUTO]. Inclua todos os slides principais com pontos de destaque para cada um.", "NEGÓCIOS", "TEXT", "bi-presentation");

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
