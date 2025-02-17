package com.py.lawbyteia.leyes.controller;

import com.py.lawbyteia.leyes.domain.entities.LawDocument;
import com.py.lawbyteia.leyes.services.LawDocumentServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/laws")
public class LawDocumentController {

    @Autowired
    private LawDocumentServiceImpl lawDocumentServiceImpl;

    @PostMapping
    public ResponseEntity<LawDocument> createLawDocument(@RequestBody LawDocument lawDocument) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lawDocumentServiceImpl.createLawDocument(lawDocument));
    }

    @GetMapping("/search")
    public ResponseEntity<List<LawDocument>> searchLawDocuments(@RequestParam String keyword) {
        return ResponseEntity.ok(lawDocumentServiceImpl.searchLawDocuments(keyword));
    }
}
