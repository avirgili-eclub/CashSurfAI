package com.py.lawbyteia.leyes.domain.entities;

import com.py.lawbyteia.ai.configuration.PgVectorType;
import com.py.lawbyteia.leyes.domain.Enums.CaseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "legal_case")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LegalCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String caseNumber;
    private String caseName;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    @Enumerated(EnumType.STRING)
    private CaseStatus status;
    @ElementCollection
    private List<String> involvedParties = new ArrayList<>();
    private String caseSummary;
    private String verdict;
    private Date rulingDate;
    private String jurisdiction;
    private String language;
    @ElementCollection
    private List<String> legalReferences = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "law_document_id")
    private LawDocument lawDocument;

    @Type(PgVectorType.class)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
//    @ElementCollection
//    @CollectionTable(name = "law_legal_case_direct_embedding") // nombre diferente
    private List<Float> embedding = new ArrayList<>();

    // Getters and setters
}
