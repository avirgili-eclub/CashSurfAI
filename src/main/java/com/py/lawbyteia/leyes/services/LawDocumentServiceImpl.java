package com.py.lawbyteia.leyes.services;

import com.py.lawbyteia.ai.services.EmbeddingService;
import com.py.lawbyteia.leyes.domain.dto.LawDocumentUploadRequest;
import com.py.lawbyteia.leyes.domain.entities.LawArticle;
import com.py.lawbyteia.leyes.domain.entities.LawDocument;
import com.py.lawbyteia.leyes.domain.repository.LawDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LawDocumentServiceImpl implements LawDocumentService {

    private final LawDocumentRepository lawDocumentRepository;
    private final LawArticleService lawArticleService;
    private final EmbeddingService embeddingService;

    @Override
    public LawDocument createLawDocument(LawDocument document) {
        // Construcción del texto a partir del documento
       // String text = document.getTitle() + " " + document.getContent();

        // Generación del embedding
        //List<Float> embedding = embeddingService.generateEmbedding(text);
//        document.setEmbedding(embedding);

        LawDocument lawDocument = lawDocumentRepository.saveAndFlush(document);

        embeddingService.processAndStoreLawDocument(lawDocument.getId());

        return lawDocument;
    }

    @Override
    public List<LawDocument> searchLawDocuments(String keyword) {
        return lawDocumentRepository.findByTitleContaining(keyword);
    }

    @Override
    public List<LawDocument> findSimilarDocuments(List<Float> queryEmbedding, int limit) {

        // Convertimos la lista de floats a un string en formato de vector de PostgreSQL
        String vectorString = "[" + queryEmbedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";

        return lawDocumentRepository.findSimilarDocuments(vectorString, limit);
    }

    @Override
    public List<LawDocument> findByCategory(String category) {
        return lawDocumentRepository.findByCategory(category);
    }

//    @Override
//    public List<LawDocument> findByJurisdiction(String jurisdiction) {
//        return lawDocumentRepository.findByJurisdiction(jurisdiction);
//    }

    @Override
    public String processLawPdf(MultipartFile file) {
        try {

            if (file.getSize() > 120_000_000) { // 150MB limit
                throw new FileSizeLimitExceededException("El archivo excede el límite de 150MB", file.getSize(),
                        120);
            }

            // Configurar opciones de memoria
            //TODO: como usar esto con loadPdf
            MemoryUsageSetting memorySettings = MemoryUsageSetting.setupTempFileOnly(); // Usa archivos temporales en lugar de memoria

            // Cargar el PDF usando el array de bytes
            PDDocument document = Loader.loadPDF(file.getBytes());
            PDFTextStripper pdfStripper = new PDFTextStripper();

            // Procesar el PDF por partes si es muy grande
            StringBuilder completeText = new StringBuilder();
            int pageCount = document.getNumberOfPages();

            // Procesar páginas en bloques
            int blockSize = 50; // Procesar 50 páginas a la vez
            for (int i = 0; i < pageCount; i += blockSize) {
                pdfStripper.setStartPage(i + 1);
                pdfStripper.setEndPage(Math.min(i + blockSize, pageCount));
                completeText.append(pdfStripper.getText(document)).append("\n");
            }

            document.close();

//            String text = pdfStripper.getText(document);
            document.close();

            // Aquí se podría agregar lógica para extraer metadatos de la ley (título, año, etc.)
            LawDocument lawDocument = new LawDocument();
            lawDocument.setTitle(extractTitle(completeText.toString()));
            lawDocument.setContent(completeText.toString());
            lawDocument.setCountry("PARAGUAY");
            lawDocument.setLanguage("ES");

            List<Float> embedding = embeddingService.generateEmbedding(completeText.toString());
//            lawDocument.setEmbedding(embedding);

            lawDocument = lawDocumentRepository.save(lawDocument);

            return lawDocument.getId().toString();
        } catch (IOException e) {
            log.error("Error procesando el archivo PDF", e);
            throw new RuntimeException("Error al procesar el PDF: " + e.getMessage());
        }
    }

    @Override
    public String processUploadedPdf(MultipartFile file, LawDocumentUploadRequest metadata) {
        try {
            if (file.getSize() > 120_000_000) { // 150MB limit
                throw new FileSizeLimitExceededException("El archivo excede el límite de 120MB", file.getSize(),
                        120);
            }
            // 1. Extraer contenido del PDF
            String text = extractTextFromPdf(file);

            // 2. Parsear los artículos del texto
            List<LawArticle> articles = extractArticles(text);

            Optional<LawDocument> lawDocumentExist = lawDocumentRepository.findByTitle(metadata.getTitle());

            boolean exists = lawDocumentExist.isPresent();

            // 3. Crear el LawDocument con metadatos si no existe
            LawDocument lawDocument = lawDocumentExist.orElse(LawDocument.builder()
                    .title(metadata.getTitle())
                    .content(text)
                    .publicationDate(metadata.getPublicationDate())
                    .publicationYear(metadata.getPublicationDate().getYear())
                    .country(metadata.getCountry())
                    .language(metadata.getLanguage())
                    .keywords(metadata.getKeywords())
                    .isActive(true)
                    .cantArticles(articles.size())
                    .build());

            if (!exists) {
                // 4. Guardar en la base de datos
                lawDocument = lawDocumentRepository.save(lawDocument);

                // 5. Guardar los artículos asociados
                lawArticleService.saveArticles(articles, lawDocument);
            }

            // 6. Generar embeddings para la ley
            embeddingService.processAndStoreLawDocument(lawDocument.getId());

            return  lawDocument.getId().toString();

        } catch (Exception e) {
            log.error("Error procesando el PDF", e);
            throw new RuntimeException("No se pudo procesar el documento", e);
        }
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {  // Carga sin escribir en disco
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setSortByPosition(true); // Asegura el orden correcto del texto
//            return pdfStripper.getText(document).replaceAll("\\s+", " ").trim(); // Limpieza básica
            return pdfStripper.getText(document);
        }
    }

//    private LawDocument parseLawDocument(String text) {
//        // Implementa un algoritmo para extraer título, número de ley, artículos, etc.
//        String title = extractTitle(text);
//        List<LawArticle> articles = extractArticles(text);
//
//        return LawDocument.builder()
//                .title(title)
//                .content(text)
//                .articles(articles)
//                .build();
//    }

    private String extractTitle(String text) {
        // Implementar lógica para extraer título basado en encabezados claros
        String[] lines = text.split("\n");

        // Suponiendo que el título está en la primera línea
        return lines.length > 0 ? lines[0] : "Título desconocido";
        //return text.split("\n")[0];
    }

    private List<LawArticle> extractArticles(String text) {
        List<LawArticle> articles = new ArrayList<>();
        Pattern pattern = Pattern.compile("(Art\\.\\s*\\d+°?\\.\\-)(.*?)((?=Art\\.\\s*\\d+°?\\.\\-)|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String articleNumber = matcher.group(1).replace("Art.", "").replace("°.-", "").trim().replace(".-","");
            String content = matcher.group(2).trim();

            LawArticle article = new LawArticle();
            article.setArticleNumber(Integer.parseInt(articleNumber));
            article.setTitle("Artículo " + articleNumber);
            article.setContent(content);
            articles.add(article);
        }
        return articles;
    }

//    private List<LawArticle> extractArticles(String text) {
//        List<LawArticle> articles = new ArrayList<>();
//
//        // Implementar lógica para detectar artículos (puede ser con expresiones regulares)
//        Pattern pattern = Pattern.compile("(?m)^Artículo \\d+: (.*)$");
//        Matcher matcher = pattern.matcher(text);
//
//        while (matcher.find()) {
//            String articleContent = matcher.group(1);
//            //TODO: hacer la creacion de lawArticle
//            LawArticle lawArticle = new LawArticle();
//            articles.add(lawArticle);
//        }
//        return articles;
//    }
}

