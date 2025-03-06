package com.py.lawbyteia.ai.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.py.lawbyteia.ai.domain.dto.OllamaEmbeddingResponse;
import com.py.lawbyteia.ai.domain.entity.LawDocumentEmbedding;
import com.py.lawbyteia.ai.domain.repository.LawDocumentEmbeddingRepository;
import com.py.lawbyteia.leyes.domain.entities.LawDocument;
import com.py.lawbyteia.leyes.domain.repository.LawDocumentRepository;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class EmbeddingService {

    private static final String OLLAMA_EMBEDDING_URL = "http://localhost:11434/api/embeddings";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final LawDocumentRepository lawDocumentRepository;
    private final LawDocumentEmbeddingRepository lawDocumentEmbeddingRepository;
    private final RestTemplate restTemplate;

    @Value("${spring.ai.ollama.embedding.model}")
    private String LLM_MODEL;

    public EmbeddingService(LawDocumentRepository lawDocumentRepository, LawDocumentEmbeddingRepository lawDocumentEmbeddingRepository, RestTemplate restTemplate) {
        this.lawDocumentRepository = lawDocumentRepository;
        this.lawDocumentEmbeddingRepository = lawDocumentEmbeddingRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public void processAndStoreLawDocument(Long documentId) {
        LawDocument document = lawDocumentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.warn("Documento con ID {} no encontrado.", documentId);
                    return new RuntimeException("Documento no encontrado");
                });

        // Construcción del texto para embedding con mejor formato
        String text = String.format("Categoría: %s\nAutoridad: %s\nTítulo: %s\nContenido: %s",
                document.getCategory(), document.getIssuingAuthority(), document.getTitle(), document.getContent());

        // Generar embedding con Ollama
        List<Float> embedding = generateEmbedding(text);

        if (embedding == null || embedding.isEmpty()) {
            log.error("No se pudo generar el embedding para el documento ID {}", documentId);
            return;
        }

        // Verificar si ya existe un embedding previo para evitar duplicados
        if (lawDocumentEmbeddingRepository.existsByLawDocument(document)) {
            log.warn("El documento ID {} ya tiene un embedding generado. No se sobrescribirá.", documentId);
            return;
        }

        // Crear y guardar el embedding en la base de datos
        LawDocumentEmbedding documentEmbedding = new LawDocumentEmbedding();
        documentEmbedding.setLawDocument(document);
        documentEmbedding.setEmbedding(embedding);

        lawDocumentEmbeddingRepository.save(documentEmbedding);
        log.info("Embedding generado y almacenado para el documento ID {}", documentId);
    }

    /**
     * Genera el embedding de un texto utilizando la API de Ollama.
     */
    public List<Float> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            log.warn("El texto para generar embedding está vacío");
            return Collections.nCopies(1536, 0.0f); // Retorna un vector de ceros en lugar de null
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Crear un objeto con la estructura JSON
        Map<String, String> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", LLM_MODEL);
        requestBodyMap.put("prompt", text);
        // Llamada a la API de Ollama
        try {
            // Serializar a JSON correctamente
            String requestBody = objectMapper.writeValueAsString(requestBodyMap);
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<OllamaEmbeddingResponse> response = restTemplate.postForEntity(
                    OLLAMA_EMBEDDING_URL, requestEntity, OllamaEmbeddingResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Float> embedding = response.getBody().getEmbedding();
                log.info("Embedding size is: {}", embedding.size());
                return response.getBody().getEmbedding();
            } else {
                log.error("Error al obtener embeddings de Ollama. Código de respuesta: {}", response.getStatusCode());
                return Collections.nCopies(1536, 0.0f);
            }
        } catch (Exception e) {
            log.error("Error al comunicarse con la API de Ollama", e);
            return Collections.nCopies(1536, 0.0f);
        }
    }
}


//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class EmbeddingService {
//    private final LawDocumentRepository lawDocumentRepository;
//    private final LawDocumentEmbeddingRepository lawDocumentEmbeddingRepository;
//    private final RestTemplate restTemplate = new RestTemplate(); // Cliente HTTP para llamar a Ollama
//
//    private static final String OLLAMA_EMBEDDING_URL = "http://localhost:11434/api/embeddings"; // URL de Ollama Embeddings
//
//    /**
//     * Genera y almacena embeddings para un documento legal.
//     */
//    @Transactional
//    public void processAndStoreLawDocument(Long documentId) {
//        Optional<LawDocument> optionalDocument = lawDocumentRepository.findById(documentId);
//        if (optionalDocument.isEmpty()) {
//            log.warn("Documento con ID {} no encontrado.", documentId);
//            throw new RuntimeException("Documento no encontrado");
//        }
//
//        LawDocument document = optionalDocument.get();
//        String text = document.getContent();
//
//        List<Float> embedding = generateEmbedding(text); // Generar embedding con Ollama
//        if (embedding == null) {
//            log.error("No se pudo generar el embedding para el documento ID {}", documentId);
//            return;
//        }
//
//        LawDocumentEmbedding documentEmbedding = new LawDocumentEmbedding();
//        documentEmbedding.setLawDocument(document);
//        documentEmbedding.setEmbedding(embedding);
//
//        lawDocumentEmbeddingRepository.save(documentEmbedding);
//        log.info("Embedding generado y almacenado para el documento ID {}", documentId);
//    }
//
//        /**
//         * Llama a la API de Ollama para generar embeddings.
//         */
//        private List<Float> generateEmbedding(String text) {
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            String requestBody = String.format("{\"model\": \"deepseek\", \"prompt\": \"%s\"}", text);
//
//            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
//
//            try {
//                ResponseEntity<OllamaEmbeddingResponse> response = restTemplate.postForEntity(
//                        OLLAMA_EMBEDDING_URL, requestEntity, OllamaEmbeddingResponse.class
//                );
//
//                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                    return response.getBody().getEmbedding();
//                } else {
//                    log.error("Error al obtener embeddings de Ollama. Código de respuesta: {}", response.getStatusCode());
//                    return null;
//                }
//            } catch (Exception e) {
//                log.error("Error al comunicarse con la API de Ollama", e);
//                return null;
//            }
//        }
//    }

//import com.py.lawbyteia.leyes.domain.repository.LawDocumentRepository;
//import org.springframework.ai.embedding.EmbeddingClient;
//import org.springframework.stereotype.Service;
//import java.util.List;
//
//@Service
//public class EmbeddingService {
//    private final EmbeddingClient embeddingClient;
//    private final LawDocumentRepository lawDocumentRepository;
//
//    public EmbeddingService(EmbeddingClient embeddingClient, LawDocumentRepository lawDocumentRepository) {
//        this.embeddingClient = embeddingClient;
//        this.lawDocumentRepository = lawDocumentRepository;
//    }
//
//    // 1️⃣ Genera el embedding de un texto usando Ollama
//    public double[] generateEmbedding(String text) {
//        List<Double> embeddingList = embeddingClient.embed(text);
//        return embeddingList.stream().mapToDouble(Double::doubleValue).toArray();
//    }
//
//    // 2️⃣ Guarda el documento con su embedding en la base de datos
//    public void saveDocumentEmbedding(LawDocument document, double[] embedding) {
//        document.setEmbedding(embedding);
//        lawDocumentRepository.save(document);
//    }
//
//    // 3️⃣ Método principal que procesa y almacena el documento legal
//    public void processAndStoreLawDocument(LawDocument document) {
//        double[] embedding = generateEmbedding(document.getContent());
//        saveDocumentEmbedding(document, embedding);
//    }
//}
