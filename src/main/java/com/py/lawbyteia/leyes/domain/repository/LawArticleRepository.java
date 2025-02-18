package com.py.lawbyteia.leyes.domain.repository;

import com.py.lawbyteia.leyes.domain.entities.LawArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LawArticleRepository extends JpaRepository<LawArticle, Long> {
    // Búsqueda por número de artículo
    List<LawArticle> findByArticleNumber(Integer articleNumber);

    // Búsqueda por documento legal
    List<LawArticle> findByLawDocumentId(Long lawDocumentId);

    // Búsqueda por contenido (útil para búsquedas textuales)
    List<LawArticle> findByContentContainingIgnoreCase(String keyword);

    // Búsqueda de artículos por documento legal y número de artículo
    Optional<LawArticle> findByLawDocumentIdAndArticleNumber(Long lawDocumentId, Integer articleNumber);

    // Búsqueda por similitud de embeddings usando pgvector
//    @Query(value = "SELECT * FROM law_article ORDER BY embedding <-> :queryEmbedding LIMIT :limit", nativeQuery = true)
//    List<LawArticle> findMostSimilarArticles(List<Float> queryEmbedding, int limit);
    // Búsqueda por similitud de embeddings en artículos de ley
    @Query(value = """
    SELECT * 
    FROM law_article 
    ORDER BY embedding <-> CAST(:queryEmbedding AS vector) 
    LIMIT :limit
    """, nativeQuery = true)
    List<LawArticle> findMostSimilarArticles(@Param("queryEmbedding") String queryEmbedding, @Param("limit") int limit);
}
