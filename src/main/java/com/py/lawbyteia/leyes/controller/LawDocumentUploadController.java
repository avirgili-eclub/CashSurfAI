package com.py.lawbyteia.leyes.controller;

import com.py.lawbyteia.leyes.domain.dto.LawDocumentUploadRequest;
import com.py.lawbyteia.leyes.services.LawDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/laws/upload")
@Slf4j
public class LawDocumentUploadController {
    private final LawDocumentService lawDocumentService;

    public LawDocumentUploadController(LawDocumentService lawDocumentService) {
        this.lawDocumentService = lawDocumentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadLawDocument(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") LawDocumentUploadRequest metadata) {

        try {
            lawDocumentService.processUploadedPdf(file, metadata);
            return ResponseEntity.ok("Archivo procesado correctamente");
        } catch (Exception e) {
            log.error("Error al procesar el archivo PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar el archivo: " + e.getMessage());
        }
    }
}
