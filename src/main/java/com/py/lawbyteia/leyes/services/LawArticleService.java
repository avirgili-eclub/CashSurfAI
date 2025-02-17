package com.py.lawbyteia.leyes.services;

import com.py.lawbyteia.ai.services.EmbeddingService;
import com.py.lawbyteia.leyes.domain.entities.LawArticle;
import com.py.lawbyteia.leyes.domain.entities.LawDocument;
import com.py.lawbyteia.leyes.domain.repository.LawArticleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LawArticleService {

    private final LawArticleRepository lawArticleRepository;
    private final EmbeddingService embeddingService;

    public LawArticle saveLawArticle(LawArticle lawArticle) {
        lawArticleRepository.save(lawArticle);

        if (lawArticle.getContent() != null) {
            List<Float> embedding = embeddingService.generateEmbedding(lawArticle.getContent());
            lawArticle.setEmbedding(embedding);
            lawArticleRepository.save(lawArticle);
        }

        return lawArticle;
    }

    /**
     * Guarda una lista de artículos en la base de datos, asociándolos con su `LawDocument`.
     */
    @Transactional
    public void saveArticles(List<LawArticle> articles, LawDocument lawDocument) {
        if (articles == null || articles.isEmpty()) {
            log.warn("No se proporcionaron artículos para guardar.");
            return;
        }
        int count = 0;
        // Asocia cada artículo con su LawDocument
        for (LawArticle article : articles) {
            count++;
            article.setLawDocument(lawDocument);
            // Generar embedding si está vacío o nulo
            if (article.getEmbedding() == null || article.getEmbedding().isEmpty()) {
                List<Float> embedding = embeddingService.generateEmbedding(article.getContent());

                if (embedding == null || embedding.size() != 1536) {
                    throw new RuntimeException("El embedding generado no tiene 1536 dimensiones.");
                }
                article.setEmbedding(embedding);
            }
            log.info("Procesando artículo {} - ID: {}", count, article.getId());
        }
        // Guardar en la base de datos
        lawArticleRepository.saveAll(articles);
        log.info("Se guardaron {} artículos para la ley ID {}", articles.size(), lawDocument.getId());
    }

    public List<LawArticle> findSimilarArticles(List<Float> queryEmbedding, int limit) {
        return lawArticleRepository.findMostSimilarArticles(queryEmbedding, limit);
    }

    public List<LawArticle> findByContent(String keyword) {
        return lawArticleRepository.findByContentContainingIgnoreCase(keyword);
    }

    public List<LawArticle> findByDocumentId(Long documentId) {
        return lawArticleRepository.findByLawDocumentId(documentId);
    }
}
