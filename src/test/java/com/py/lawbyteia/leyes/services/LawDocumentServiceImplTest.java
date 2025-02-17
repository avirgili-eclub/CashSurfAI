package com.py.lawbyteia.leyes.services;

import com.py.lawbyteia.ai.services.EmbeddingService;
import com.py.lawbyteia.leyes.domain.dto.LawDocumentUploadRequest;
import com.py.lawbyteia.leyes.domain.entities.LawDocument;
import com.py.lawbyteia.leyes.domain.repository.LawDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class LawDocumentServiceImplTest {


    @Autowired
    private LawDocumentService lawDocumentService;

    @Autowired
    private LawDocumentRepository lawDocumentRepository;

    @Mock  // Reemplazo de @MockBean
    private EmbeddingService embeddingService;

    @InjectMocks  // Inyecta los mocks en la clase de prueba
    private LawDocumentServiceImpl lawDocumentServiceWithMocks;

    @Test
    public void testProcessUploadedPdf() throws Exception {

        String nombreArchivo = "Codigo Civil Ley 1183-85.pdf"; // Cambiamos el nombre del archivo aquí
        byte[] archivo = obtenerArchivoComoBytes("C:/temp/" + nombreArchivo);

        // Simulación de un archivo PDF como MultipartFile
        MockMultipartFile file = new MockMultipartFile("Ley 1183.pdf", "Codigo Civil Ley 1183-85.pdf", "application/pdf", archivo);

        // Metadata simulada
        LawDocumentUploadRequest metadata = new LawDocumentUploadRequest();
        metadata.setTitle("Ley Codigo Civil 1183");
        metadata.setPublicationDate(new Date());
        metadata.setYear(2017);
        metadata.setCountry("Paraguay");
        metadata.setLanguage("ES");
        metadata.setKeywords(List.of("ley", "codigo civil"));

        // Ejecutar el metodo
        lawDocumentService.processUploadedPdf(file, metadata);

        // Verificar que el documento se haya guardado en la BD
        Optional<LawDocument> savedDocument = lawDocumentRepository.findByTitle("Ley Codigo Civil 1183");

        assertTrue(savedDocument.isPresent());
    }

    @Test
    void createLawDocument() {
    }

    @Test
    void searchLawDocuments() {
    }

    @Test
    void findSimilarDocuments() {
    }

    @Test
    void findByCategory() {
    }

    @Test
    void findByJurisdiction() {
    }

    @Test
    void processLawPdf() {
    }

    private byte[] obtenerArchivoComoBytes(String filePath) {
        try {
            Path path = Path.of(filePath);
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo: " + filePath, e);
        }
    }

}