//package com.py.lawbyteia.ai.domain.repository;
//
//import com.py.lawbyteia.ai.domain.entity.LawDocumentEmbedding;
//import com.py.lawbyteia.leyes.domain.entities.LawDocument;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface LawDocumentEmbeddingRepository extends JpaRepository<LawDocumentEmbedding, Long> {
//
//    /**
//     * Encuentra los embeddings de un documento legal por su ID.
//     */
//    @Query("SELECT e FROM LawDocumentEmbedding e WHERE e.lawDocument.id = :documentId")
//    List<LawDocumentEmbedding> findByLawDocumentId(@Param("documentId") Long documentId);
//
//    /**
//     * Busca los documentos más cercanos a un embedding dado usando la función `cosine_distance` de `pgvector`.
//     *
//     * La consulta asume que el embedding es un vector de floats en la base de datos.
//     */
//    @Query(value = """
//        SELECT * FROM law_document_embedding
//        ORDER BY embedding <-> cast(:embedding AS vector)
//        LIMIT :limit
//        """, nativeQuery = true)
//    List<LawDocumentEmbedding> findClosestEmbeddings(@Param("embedding") String embedding, @Param("limit") int limit);
//
//    boolean existsByLawDocument(LawDocument lawDocument);
//
//}
//
