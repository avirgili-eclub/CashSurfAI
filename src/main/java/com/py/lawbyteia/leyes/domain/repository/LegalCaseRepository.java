package com.py.lawbyteia.leyes.domain.repository;

import com.py.lawbyteia.leyes.domain.Enums.CaseStatus;
import com.py.lawbyteia.leyes.domain.entities.LegalCase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LegalCaseRepository extends JpaRepository<LegalCase, Long> {
    List<LegalCase> findByCaseNumber(String caseNumber);
    List<LegalCase> findByLawDocumentId(Long lawDocumentId);

    // Nuevos métodos
    // Búsqueda por estado del caso
    List<LegalCase> findByStatus(CaseStatus status);

    // Búsqueda por rango de fechas
    List<LegalCase> findByRulingDateBetween(LocalDateTime startDate, LocalDateTime endDate);

//    // Búsqueda por similitud de contenido usando embeddings
//    @Query(value = "SELECT * FROM legal_case ORDER BY case_embedding <-> :queryEmbedding LIMIT :limit",
//            nativeQuery = true)
//    List<LegalCase> findSimilarCases(List<Float> queryEmbedding, int limit);
// Búsqueda por similitud de contenido en casos legales
@Query(value = """
    SELECT * 
    FROM legal_case 
    ORDER BY case_embedding <-> CAST(:queryEmbedding AS vector) 
    LIMIT :limit
    """, nativeQuery = true)
List<LegalCase> findSimilarCases(@Param("queryEmbedding") String queryEmbedding, @Param("limit") int limit);

    // Búsqueda por palabras clave en la descripción
    List<LegalCase> findByDescriptionContainingIgnoreCase(String keyword);

    // Casos más recientes
    @Query("SELECT lc FROM LegalCase lc ORDER BY lc.rulingDate DESC")
    List<LegalCase> findMostRecentCases(Pageable pageable);

    //buscar por juridiscion
//     "AND (:jurisdiction IS NULL OR ld.jurisdiction = :jurisdiction) " +

    // Estadísticas por estado
    @Query("SELECT lc.status, COUNT(lc) FROM LegalCase lc GROUP BY lc.status")
    List<Object[]> getCaseStatsByStatus();
}
