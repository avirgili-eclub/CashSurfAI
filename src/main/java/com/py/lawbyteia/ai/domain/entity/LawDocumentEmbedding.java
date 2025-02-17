package com.py.lawbyteia.ai.domain.entity;

import com.py.lawbyteia.ai.configuration.PgVectorConverter;
import com.py.lawbyteia.leyes.domain.entities.LawDocument;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "law_document_embedding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawDocumentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "law_document_id", nullable = false)
    private LawDocument lawDocument;

    @Column(name = "embedding", columnDefinition = "vector(1536)", nullable = false)
    @Convert(converter = PgVectorConverter.class) // Usando el convertidor
    private List<Float> embedding; // Se almacena como un vector en PostgreSQL con pgvector

    @Column(name = "created_at", updatable = false)
    private Long createdAt; // Timestamp en epoch format

    @PrePersist
    protected void onCreate() {
        this.createdAt = System.currentTimeMillis();
    }
}
