package com.py.lawbyteia.leyes.domain.repository;

import com.py.lawbyteia.leyes.domain.entities.LawDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LawDocumentRepository  extends JpaRepository<LawDocument, Long> {
    List<LawDocument> findByTitleContaining(String keyword);

    Optional<LawDocument> findByTitle(String title);

    // Nuevos métodos
    // Búsqueda por categoría legal
    List<LawDocument> findByCategory(String category);

    //TODO: verificar si hace falta agregar al law_document
    // Búsqueda por jurisdicción
//    List<LawDocument> findByJurisdiction(String jurisdiction);

    // Búsqueda por año de publicación
    List<LawDocument> findByPublicationYear(Integer year);

//    List<LawDocument> findByPublicationDate(Date publicationDate);

    // Búsqueda por rango de años
    List<LawDocument> findByPublicationYearBetween(Integer startYear, Integer endYear);

    // Búsqueda por estado (activo/derogado)
    List<LawDocument> findByIsActive(Boolean isActive);

    // Búsqueda por similitud de contenido
    @Query(value = "SELECT * FROM law_document ORDER BY document_embedding <-> :queryEmbedding LIMIT :limit",
            nativeQuery = true)
    List<LawDocument> findSimilarDocuments(List<Float> queryEmbedding, int limit);

    // Documentos más recientes
    @Query("SELECT ld FROM LawDocument ld ORDER BY ld.publicationDate DESC")
    List<LawDocument> findMostRecentDocuments(Pageable pageable);

    // Búsqueda combinada
    @Query("SELECT ld FROM LawDocument ld " +
            "WHERE (:category IS NULL OR ld.category = :category) " +
//            "AND (:jurisdiction IS NULL OR ld.jurisdiction = :jurisdiction) " +
            "AND (:keyword IS NULL OR LOWER(ld.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<LawDocument> findByMultipleCriteria(
            @Param("category") String category,
            /*@Param("jurisdiction") String jurisdiction,*/
            @Param("keyword") String keyword
    );

    // Conteo por categoría
    @Query("SELECT ld.category, COUNT(ld) FROM LawDocument ld GROUP BY ld.category")
    List<Object[]> getDocumentCountByCategory();
}
