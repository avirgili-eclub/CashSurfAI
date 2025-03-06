package com.py.lawbyteia.finanzas.controller;

import com.py.lawbyteia.ai.services.OllamaService;
import com.py.lawbyteia.finanzas.domain.models.dto.ExpenseDTO;
import com.py.lawbyteia.finanzas.domain.models.entity.Expense;
import com.py.lawbyteia.finanzas.services.ExpenseService;
import lombok.extern.log4j.Log4j2;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import nu.pattern.OpenCV; // Para cargar las bibliotecas nativas con org.openpnp
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.imgscalr.Scalr;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/invoice")
@Log4j2
public class InvoiceController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private OllamaService ollamaService;

    // Cargar las bibliotecas nativas de OpenCV al inicio (solo una vez)
    static {
        OpenCV.loadShared(); // Método recomendado por org.openpnp para cargar las bibliotecas nativas
    }

    @PostMapping("/upload-invoice")
    public ResponseEntity<String> uploadInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean useOpenCV) {
        try {
            String extractedText = extractTextFromFile(file, useOpenCV);
            Expense expense = ollamaService.parseInvoiceWithAI(extractedText);
            expense.setUserId(userId);
            //ExpenseDTO savedExpense = expenseService.saveExpense(expense);
//            return ResponseEntity.ok("Gasto registrado: " + savedExpense.getDescription());
            return ResponseEntity.ok("Gasto registrado: " + expense.getDescription());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error procesando la factura: " + e.getMessage());
        }
    }

    @GetMapping("/get-expenses")
    public ResponseEntity<List<ExpenseDTO>> getExpenses(@RequestParam Long userId) {
        return ResponseEntity.ok(expenseService.getExpensesByUser(userId));
    }

    // Método unificado para extraer texto según el tipo de archivo
    private String extractTextFromFile(MultipartFile file, boolean useOpenCV) throws Exception {
        String contentType = file.getContentType();
        String fileName = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();

        // Determinar si es PDF o imagen
        if (contentType.equals("application/pdf") || fileName.endsWith(".pdf")) {
            return extractTextFromPDF(file);
        } else if (contentType.startsWith("image/") || fileName.matches(".*\\.(jpg|jpeg|png)")) {
            return useOpenCV ? performOCRWithOpenCV(file) : performOCR(file);
        } else {
            throw new IllegalArgumentException("Formato no soportado: " + contentType);
        }
    }

    private String performOCR(MultipartFile file) throws IOException, TesseractException {
        // Configurar Tesseract
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("src/main/resources/tessdata"); // Ruta a los datos de entrenamiento
        tesseract.setLanguage("eng+spa"); // Soporte para inglés y español

        // Convertir el archivo subido a una imagen
        BufferedImage image = ImageIO.read(file.getInputStream());

        // Opcional: Preprocesar la imagen para mejorar la precisión
        // Preprocesamiento: Escala de grises y aumento de contraste
        BufferedImage processedImage = Scalr.resize(image,  Scalr.Method.QUALITY, 300); // Redimensionar a 300 DPI
        // Aquí podrías añadir más filtros si necesitas

        return tesseract.doOCR(processedImage);
    }

    // Método con OpenCV + Tesseract
    private String performOCRWithOpenCV(MultipartFile file) throws Exception {

//        // Convertir MultipartFile a BufferedImage
//        BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
//
//        // Convertir BufferedImage a Mat
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        ImageIO.write(bufferedImage, "png", baos);
//        MatOfByte matOfByte = new MatOfByte(baos.toByteArray());
        // Convertir los bytes del MultipartFile a Mat usando MatOfByte
        MatOfByte matOfByte = new MatOfByte(file.getBytes());
        Mat image = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);

        // Verificar si la imagen se cargó correctamente
        if (image.empty()) {
            throw new IllegalStateException("No se pudo cargar la imagen en OpenCV");
        }

        // Preprocesamiento con OpenCV
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY); // Escala de grises
        Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU); // Binarización automática

        // Guardar temporalmente la imagen procesada
        String tempFile = "temp_processed.png";
        Imgcodecs.imwrite(tempFile, gray);

        // Usar Tesseract sobre la imagen procesada
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("src/main/resources/tessdata");
        tesseract.setLanguage("eng+spa");
        String result = tesseract.doOCR(new File(tempFile));

        // Eliminar archivo temporal
        new File(tempFile).delete();

        return result;
    }

    // Método para extraer texto de PDFs con PDFBox 3.0.1
    private String extractTextFromPDF(MultipartFile file) throws IOException {
        // Cargar el PDF usando Loader en PDFBox 3.x
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(file.getInputStream()))) {
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                if (text.trim().isEmpty()) {
                    // Si no hay texto (PDF escaneado), convertir a imagen y usar OCR
                    return extractTextFromPDFImages(document);
                }
                return text;
            } else {
                throw new IOException("El PDF está encriptado y no se puede procesar");
            }
        } catch (TesseractException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Método para PDFs escaneados (convierte páginas a imágenes y aplica OCR)
    private String extractTextFromPDFImages(PDDocument document) throws IOException, TesseractException {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("src/main/resources/tessdata");
        tesseract.setLanguage("eng+spa");

        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage image = pdfRenderer.renderImageWithDPI(i, 300); // 300 DPI
            String pageText = tesseract.doOCR(image);
            result.append(pageText).append("\n");
        }
        return result.toString();
    }

    //opcion 2 para extractTextFromPdfImages
/*    private String extractTextFromPDFImages(PDDocument document) throws IOException, TesseractException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage image = pdfRenderer.renderImageWithDPI(i, 300);
            Mat mat = Imgcodecs.imdecode(new MatOfByte(toByteArray(image)), Imgcodecs.IMREAD_COLOR);
            Mat gray = new Mat();
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
            String tempFile = "temp_pdf_page_" + i + ".png";
            Imgcodecs.imwrite(tempFile, gray);
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("src/main/resources/tessdata");
            tesseract.setLanguage("eng+spa");
            result.append(tesseract.doOCR(new File(tempFile))).append("\n");
            new File(tempFile).delete();
        }
        return result.toString();
    }

    private byte[] toByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }*/


    private Expense parseInvoiceText(String text) {
        Expense expense = new Expense();

        // Extraer datos con expresiones regulares o reglas simples
        expense.setAmount(extractAmount(text));
        expense.setDate(extractDate(text));
        expense.setDescription(extractDescription(text));
        expense.setCategory(categorizeExpense(expense.getDescription()));

        return expense;
    }

    private double extractAmount(String text) {
        // Buscar patrones como "$50.00", "Total: 25,50", etc.
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.toLowerCase().contains("total") || line.matches(".*\\$\\d+.*") || line.matches(".*\\d+[.,]\\d{2}.*")) {
                String cleaned = line.replaceAll("[^0-9.,]", "");
                try {
                    return Double.parseDouble(cleaned.replace(",", "."));
                } catch (NumberFormatException e) {
                    // Ignorar si no se puede parsear
                }
            }
        }
        return 0.0; // Valor por defecto si no se encuentra
    }

    private LocalDate extractDate(String text) {
        // Buscar fechas en formatos como "dd/mm/yyyy", "mm-dd-yyyy", etc.
        String[] lines = text.split("\n");
        for (String line : lines) {
            Matcher matcher = Pattern.compile("\\d{2}[/-]\\d{2}[/-]\\d{4}").matcher(line);
            if (matcher.find()) {
                String dateStr = matcher.group();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                try {
                    return LocalDate.parse(dateStr.replace("-", "/"), formatter);
                } catch (DateTimeParseException e) {
                    // Probar otro formato si falla
                    formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                    return LocalDate.parse(dateStr.replace("-", "/"), formatter);
                }
            }
        }
        return LocalDate.now(); // Fecha actual como fallback
    }

    private String extractDescription(String text) {
        // Tomar la primera línea que parezca un nombre de comercio o descripción
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.trim().length() > 5 && !line.toLowerCase().contains("total") && !line.matches(".*\\d+[.,]\\d{2}.*")) {
                return line.trim();
            }
        }
        return "Gasto sin descripción";
    }

    private String categorizeExpense(String description) {
        // Reglas simples para categorizar
        description = description.toLowerCase();
        if (description.contains("restaurant") || description.contains("comida") || description.contains("supermercado")) {
            return "comida";
        } else if (description.contains("uber") || description.contains("taxi") || description.contains("transporte")) {
            return "transporte";
        } else if (description.contains("luz") || description.contains("agua") || description.contains("internet")) {
            return "servicios";
        }
        return "otros";
    }
}