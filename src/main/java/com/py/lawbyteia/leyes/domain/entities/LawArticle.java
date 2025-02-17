package com.py.lawbyteia.leyes.domain.entities;

import com.py.lawbyteia.ai.configuration.PgVectorConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "law_article")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LawArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private Integer articleNumber;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    @ManyToOne
    @JoinColumn(name = "law_document_id")
    private LawDocument lawDocument;

    @Convert(converter = PgVectorConverter.class)
    @Column(columnDefinition = "vector(1536)")
    @ElementCollection
    @CollectionTable(name = "law_article_direct_embedding") // nombre diferente
    private List<Float> embedding = new ArrayList<>();

}
