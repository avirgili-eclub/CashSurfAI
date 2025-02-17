package com.py.lawbyteia.leyes.domain.entities;

import com.py.lawbyteia.ai.configuration.PgVectorConverter;
import com.py.lawbyteia.ai.configuration.PgVectorType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

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

    @Type(PgVectorType.class)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
//    @CollectionTable(name = "law_article_embedding")
    private List<Float> embedding = new ArrayList<>();

}
