package com.py.lawbyteia.leyes.services;

import com.py.lawbyteia.ai.services.EmbeddingService;
import com.py.lawbyteia.leyes.domain.Enums.CaseStatus;
import com.py.lawbyteia.leyes.domain.entities.LegalCase;
import com.py.lawbyteia.leyes.domain.repository.LegalCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalCaseService {

    private final EmbeddingService embeddingService;
    private final LegalCaseRepository legalCaseRepository;

    public LegalCase createLegalCase(LegalCase legalCase) {
        // Construcción del texto a partir del caso
        String text = legalCase.getCaseName() + " " + legalCase.getDescription();

        // Generación del embedding
        List<Float> embedding = embeddingService.generateEmbedding(text);
        legalCase.setEmbedding(embedding);

        return legalCaseRepository.save(legalCase);
    }

    public List<LegalCase> getCasesByLawDocument(Long lawDocumentId) {
        return legalCaseRepository.findByLawDocumentId(lawDocumentId);
    }

    public List<LegalCase> getCasesByCaseNumber(String caseNumber) {
        return legalCaseRepository.findByCaseNumber(caseNumber);
    }

    public List<LegalCase> findSimilarCases(List<Float> queryEmbedding, int limit) {
        return legalCaseRepository.findSimilarCases(queryEmbedding, limit);
    }

    public List<LegalCase> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return legalCaseRepository.findByRulingDateBetween(startDate, endDate);
    }

    public List<LegalCase> findByStatus(CaseStatus status) {
        return legalCaseRepository.findByStatus(status);
    }

    public List<Object[]> getCaseStatistics() {
        return legalCaseRepository.getCaseStatsByStatus();
    }
}

