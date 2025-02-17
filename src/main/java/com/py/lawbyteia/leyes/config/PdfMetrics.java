package com.py.lawbyteia.leyes.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
//TODO: imeplementar PDFMEtrics
public class PdfMetrics {
    public static void logPdfStats(PDDocument document, MultipartFile file) {
        log.info("PDF Stats - Size: {}MB, Pages: {}, Memory Used: {}MB",
                file.getSize() / (1024 * 1024),
                document.getNumberOfPages(),
                Runtime.getRuntime().totalMemory() / (1024 * 1024)
        );
    }
}
