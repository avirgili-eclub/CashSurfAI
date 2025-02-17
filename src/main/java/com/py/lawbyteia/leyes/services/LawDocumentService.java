package com.py.lawbyteia.leyes.services;

import com.py.lawbyteia.leyes.domain.dto.LawDocumentUploadRequest;
import com.py.lawbyteia.leyes.domain.entities.LawDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LawDocumentService {

    LawDocument createLawDocument(LawDocument document);
    String processLawPdf(MultipartFile file);
    String processUploadedPdf(MultipartFile file, LawDocumentUploadRequest metadata);
    List<LawDocument> searchLawDocuments(String keyword);
    List<LawDocument> findSimilarDocuments(List<Float> queryEmbedding, int limit);
    List<LawDocument> findByCategory(String category);
//    List<LawDocument> findByJurisdiction(String jurisdiction);
}
