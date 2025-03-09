package com.py.cashsurfai.finanzas.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.py.cashsurfai.ai.services.OllamaService;
import com.py.cashsurfai.ai.services.SpeechToTextService;
import com.py.cashsurfai.finanzas.domain.models.dto.ExpenseDTO;
import com.py.cashsurfai.finanzas.domain.models.dto.ExpenseRequest;
import com.py.cashsurfai.finanzas.domain.models.dto.SharedGroupRequest;
import com.py.cashsurfai.finanzas.domain.models.entity.Category;
import com.py.cashsurfai.finanzas.domain.models.entity.Expense;
import com.py.cashsurfai.finanzas.domain.models.entity.SharedExpenseGroup;
import com.py.cashsurfai.finanzas.domain.models.entity.User;
import com.py.cashsurfai.finanzas.domain.repository.SharedExpenseGroupRepository;
import com.py.cashsurfai.finanzas.domain.repository.UserRepository;
import com.py.cashsurfai.finanzas.services.CategoryService;
import com.py.cashsurfai.finanzas.services.ExpenseService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/invoice")
@Log4j2
public class InvoiceController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private OllamaService ollamaService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SharedExpenseGroupRepository sharedExpenseGroupRepository;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SpeechToTextService speechToTextService;

    // Cargar las bibliotecas nativas de OpenCV al inicio (solo una vez)
    static {
        OpenCV.loadShared(); // Método recomendado por org.openpnp para cargar las bibliotecas nativas
    }

    // 1. Entrada Manual
    @PostMapping("/manual")
    public ResponseEntity<ExpenseDTO> addManualExpense(@RequestBody ExpenseRequest expenseRequest) {
        try {
            User user = userRepository.findById(expenseRequest.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            Category category = categoryService.findOrCreateCategory(expenseRequest.getCategory(), "📝"); // Emoji por defecto
            Expense expense = new Expense(
                    expenseRequest.getAmount(),
                    expenseRequest.getDate(),
                    expenseRequest.getDescription(),
                    category,
                    user
            );
            expense.setSource("manual");
            if (expenseRequest.getSharedGroupId() != null) {
                SharedExpenseGroup group = sharedExpenseGroupRepository
                        .findById(expenseRequest.getSharedGroupId())
                        .orElseThrow(() -> new IllegalArgumentException("Shared group not found"));
                expense.setSharedExpenseGroup(group);
            }
            ExpenseDTO savedExpense = expenseService.saveExpense(expense);
            return ResponseEntity.ok(savedExpense);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }


    //2. Entrada por PDF/Imagen
    @PostMapping("/upload-invoice")
    public ResponseEntity<String> uploadInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long userId,
            @RequestParam(value = "sharedGroupId", required = false) Long sharedGroupId,
            @RequestParam(defaultValue = "false") boolean useOpenCV) {
        try {
            String extractedText = extractTextFromFile(file, useOpenCV);
            Expense expense = ollamaService.parseInvoiceWithAI(extractedText);
            expense.setUser(new User());
            if (sharedGroupId != null) {
                SharedExpenseGroup group = sharedExpenseGroupRepository
                        .findById(sharedGroupId)
                        .orElseThrow(() -> new IllegalArgumentException("Shared group not found"));
                expense.setSharedExpenseGroup(group);
            }
            ExpenseDTO savedExpense = expenseService.saveExpense(expense);
//            return ResponseEntity.ok("Gasto registrado: " + savedExpense.getDescription());
            return ResponseEntity.ok("Gasto registrado: " + savedExpense);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error procesando la factura: " + e.getMessage());
        }
    }


    // 3. Entrada por Voz
    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> addVoiceExpense(@RequestParam("audio") MultipartFile audioFile,
                                                      @RequestParam("userId") Long userId,
                                                      @RequestParam(value = "sharedGroupId", required = false) Long sharedGroupId) {
        try {
            String transcript = speechToTextService.convertAudioToText(audioFile.getBytes());
            if (transcript == null || transcript.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            String aiResponse = ollamaService.parseVoiceWithAI(transcript); // Nuevo método en OllamaService

            JsonNode jsonArray = new ObjectMapper().readTree(aiResponse);

            if (!jsonArray.isArray()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Respuesta de IA no es un arreglo");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            SharedExpenseGroup group = null;
            if (sharedGroupId != null) {
                group = sharedExpenseGroupRepository.findById(sharedGroupId)
                        .orElseThrow(() -> new IllegalArgumentException("Shared group not found"));
            }

            List<Expense> expenses = new ArrayList<>();

            for (JsonNode json : jsonArray) {
                Category category = categoryService.findOrCreateCategory(
                        json.get("categoria").asText(),
                        json.get("descripcion").asText(),
                        json.get("emoji").asText(),
                        user
                );

                Expense expense = new Expense(
                        json.get("monto").asText(),
                        json.get("fecha").asText().equals("hoy") ? LocalDate.now() : LocalDate.parse(json.get("fecha").asText(), DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        json.get("descripcion").asText(),
                        category,
                        user
                );
                expense.setSource("voice");
                if (group != null) {
                    expense.setSharedExpenseGroup(group);
                }
                expenses.add(expense);
            }

            List<ExpenseDTO> savedExpenses = expenseService.saveAllExpenses(expenses); // Guardar todos de una vez
            return ResponseEntity.ok("Registros guardados: " + savedExpenses.toString());
        } catch (IOException e) {
            log.error("Error processing voice expense", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 4. Crear Grupo Compartido
    @PostMapping("/shared-group")
    public ResponseEntity<SharedExpenseGroup> createSharedExpenseGroup(@RequestBody SharedGroupRequest request) {
        try {
            SharedExpenseGroup group = expenseService.createSharedExpenseGroup(request.getName(), request.getUserIds());
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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

}