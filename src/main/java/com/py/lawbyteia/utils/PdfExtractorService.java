package com.py.lawbyteia.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class PdfExtractorService {

    public static String extractTextFromPdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {  // Usando Loader.loadPDF()
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setSortByPosition(true); // Asegura el orden correcto del texto
            return pdfStripper.getText(document).replaceAll("\\s+", " ").trim(); // Limpieza básica del texto
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el PDF: " + file.getName(), e);
        }
    }
}
