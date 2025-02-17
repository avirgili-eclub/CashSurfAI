package com.py.lawbyteia.leyes.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LawDocumentUploadRequest {
    private String title;
    private Integer year;  // Año de publicación
    private String country;
    private String language;
    private Date publicationDate;
    private List<String> keywords;
}
