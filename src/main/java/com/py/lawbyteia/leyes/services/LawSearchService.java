package com.py.lawbyteia.leyes.services;

import com.py.lawbyteia.ai.services.EmbeddingService;
import com.py.lawbyteia.ai.services.OllamaService;
import com.py.lawbyteia.leyes.domain.entities.LawArticle;
import com.py.lawbyteia.leyes.domain.entities.LawDocument;
import com.py.lawbyteia.leyes.domain.entities.LegalCase;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LawSearchService {
    private final LawArticleService lawArticleService;
    private final LawDocumentServiceImpl lawDocumentServiceImpl;
    private final LegalCaseService legalCaseService;
    private final OllamaService ollamaService;
    private final EmbeddingService embeddingService;

    public String processLegalQuery(String userQuery) {
        // 1. Análisis inicial de la consulta
        List<Float> queryEmbedding = embeddingService.generateEmbedding(userQuery);
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            log.warn("No se pudieron generar embeddings para la consulta: {}", userQuery);
            return "No pude procesar tu pregunta correctamente. ¿Podrías reformularla?";
        }
        // 2. Búsqueda de múltiples fuentes relevantes
        SearchResult searchResult = findRelevantLegalInformation(queryEmbedding, userQuery);

        // 3. Construcción del prompt para Ollama
        String prompt = buildLegalPrompt(searchResult, userQuery);

        // 4. Obtener respuesta del modelo
        return ollamaService.askDeepSeek(prompt, userQuery);
    }

    private SearchResult findRelevantLegalInformation(List<Float> queryEmbedding, String query) {
        // Buscar en múltiples fuentes
        //TODO: Buscar en LawDocuments
        List<LawDocument> documents = lawDocumentServiceImpl.findSimilarDocuments(queryEmbedding, 3);
        List<LawArticle> articles = lawArticleService.findSimilarArticles(queryEmbedding, 3);
        //List<LegalCase> cases = legalCaseService.findSimilarCases(queryEmbedding, 2);

        return SearchResult.builder()
                .relevantArticles(articles)
                //.relevantCases(cases)
                .relevantDocuments(documents)
                .build();
    }

    private String buildLegalPrompt(SearchResult searchResult, String userQuery) {
        StringBuilder prompt = new StringBuilder();

        // Instrucciones para el modelo
        prompt.append("Eres un asistente legal especializado. Analiza la siguiente información y proporciona una respuesta clara y fundamentada. ");
        prompt.append("Cita las leyes y los artículos específicos cuando sea relevante.\n\n");

        // Documentos legales completos
        if (!searchResult.getRelevantDocuments().isEmpty()) {
            prompt.append("Documentos Legales Relacionados:\n");
            for (LawDocument document : searchResult.getRelevantDocuments()) {
                prompt.append("Título: ").append(document.getTitle()).append("\n")
                        .append("Categoría: ").append(document.getCategory()).append("\n")
                        .append("Autoridad Emisora: ").append(document.getIssuingAuthority()).append("\n")
                        .append("Contenido:\n").append(document.getContent()).append("\n\n");
            }
        }


        // Contexto legal relevante
        prompt.append("Contexto Legal Relevante:\n");
        for (LawArticle article : searchResult.getRelevantArticles()) {
            prompt.append("Artículo ").append(article.getArticleNumber())
                    .append(" de ").append(article.getLawDocument().getTitle())
                    .append(":\n").append(article.getContent()).append("\n\n");
        }

        // Casos precedentes si existen
        if (searchResult.getRelevantCases() != null && !searchResult.getRelevantCases().isEmpty()) {
            prompt.append("Casos Relacionados:\n");
            for (LegalCase legalCase : searchResult.getRelevantCases()) {
                prompt.append("Caso ").append(legalCase.getCaseNumber())
                      .append(": ").append(legalCase.getDescription()).append("\n\n");
            }
        }

        // Consulta del usuario
        prompt.append("Consulta del Usuario:\n").append(userQuery).append("\n\n");

        // Instrucciones específicas para la respuesta
        prompt.append("Por favor, proporciona una respuesta que:\n");
        prompt.append("1. Explique el marco legal aplicable\n");
        prompt.append("2. Cite artículos específicos relevantes\n");
        prompt.append("3. Mencione casos similares si aplica\n");
        prompt.append("4. Ofrezca una conclusión clara\n");

        return prompt.toString();
    }


    @Data
    @Builder
    private static class SearchResult {
        private List<LawArticle> relevantArticles;
        private List<LegalCase> relevantCases;
        private List<LawDocument> relevantDocuments;
    }
}

//
//public class LawSearchService {
//    private final LawArticleService lawArticleService;
//    private final LawDocumentServiceImpl lawDocumentService;
//    private final LegalCaseService legalCaseService;
//    private final OllamaService ollamaService;
//    private final EmbeddingService embeddingService;
//
//    public String processLegalQuery(String userQuery) {
//        List<Float> queryEmbedding = embeddingService.generateEmbedding(userQuery);
//        SearchResult searchResult = findRelevantLegalInformation(queryEmbedding, userQuery);
//        String prompt = buildLegalPrompt(searchResult, userQuery);
//        return ollamaService.askDeepSeek(prompt, userQuery);
//    }
//
//    @Data
//    @Builder
//    private static class SearchResult {
//        private List<LawArticle> relevantArticles;
//        private List<LegalCase> relevantCases;
//        private List<LawDocument> relevantDocuments;
//    }
//
//    private SearchResult findRelevantLegalInformation(List<Float> queryEmbedding, String query) {
//        return SearchResult.builder()
//                .relevantArticles(lawArticleService.findSimilarArticles(queryEmbedding, 3))
//                .relevantCases(legalCaseService.findSimilarCases(queryEmbedding, 2))
//                .relevantDocuments(lawDocumentService.findSimilarDocuments(queryEmbedding, 2))
//                .build();
//    }
//
//// ... resto del código buildLegalPrompt igual que antes ...