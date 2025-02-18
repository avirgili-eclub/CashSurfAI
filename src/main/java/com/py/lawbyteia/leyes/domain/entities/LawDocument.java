package com.py.lawbyteia.leyes.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "law_document")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LawDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    private Date publicationDate;
    @Column
    private Integer publicationYear;
    private Boolean isActive;
    private String category;
    private String version;
    private String issuingAuthority;
    @Column(nullable = false)
    private String country;
    @Column(nullable = false)
    private String language;
    @ElementCollection
    private List<String> keywords = new ArrayList<>();
    private String summary;
    private Integer cantArticles;

    @ElementCollection
    private List<String> relatedDocuments= new ArrayList<>();

    @OneToMany(mappedBy = "lawDocument", cascade = CascadeType.ALL)
    private List<LegalCase> legalCases;

}
