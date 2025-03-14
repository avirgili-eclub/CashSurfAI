package com.py.cashsurfai.ai.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.py.cashsurfai.finanzas.domain.models.entity.Category;
import com.py.cashsurfai.finanzas.domain.models.entity.Expense;
import com.py.cashsurfai.finanzas.domain.models.entity.User;
import com.py.cashsurfai.finanzas.domain.repository.UserRepository;
import com.py.cashsurfai.finanzas.services.CategoryService;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Log4j2
public class ExpenseAnalysisService {

    private final ChatClient chatClient; // Inyectado por spring-ai-openai-spring-boot-starter
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Para parsear JSON

    @Value("${spring.ai.openai.chat.options.model}")
    private String openAiModel;

    public ExpenseAnalysisService(ChatClient.Builder builder, UserRepository userRepository, CategoryService categoryService) {
        this.chatClient = builder.defaultOptions(ChatOptions.builder()
                .model(openAiModel)
                        .maxTokens(1000)
                        .temperature(0.2)
                .build()).build();
        this.userRepository = userRepository;
        this.categoryService = categoryService;
    }

    /**
     * Método genérico para consultas a OpenAI con system y user prompts
     */
    public String askOpenAi(String systemPrompt, String userPrompt) {
        try {
            var response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call();
            log.debug("OpenAI Response: {}", response.content());

            return response.content();

        } catch (Exception e) {
            throw new RuntimeException("Error al comunicarse con OpenAI: " + e.getMessage(), e);
        }
    }


    /**
     * Método para analizar facturas y devolver un Expense en formato JSON
     */
    public String parseInvoiceWithAI(String invoiceText) {
        String systemPrompt =
                "Analiza el siguiente texto y extrae el monto, fecha, descripción y categoría de la factura. Si encuentras errores en el texto, corrígelos mientras extraes los datos. " +
                "No devuelvas el texto corregido, solo el objeto JSON con los datos extraídos y corregidos si es necesario. \n" +
                "Sigue estas reglas:\n" +
                "- Factura Numero: Busca algo como Factura, Factura Nro, Transaccion Nro, N°, seguido de un numero entero sin coma ni puntos pero podria contener guion medio.\n" +
                "- Monto: Busca 'Total', '$', o números con o sin separadores y devuélvelo como entero sin separadores de miles.\n" +
                "- Fecha: Busca formatos como 'dd/mm/yyyy' o 'dd-mm-yyyy' y transforma para dame en formato dd/MM/yyyy.\n" +
                "- Descripción: Identifica el propósito o artículo del gasto, si no hay descripción clara, usa 'otros'.\n" +
                "- Categoría: Deduce según el contexto (comida, transporte, servicios, otros).\n" +
                "- Emoji: Asigna un emoji relacionado con la categoría como una secuencia Unicode escapada (ej. Emoji: '\\uD83C\\uDF54' para comida 🍔, Emoji: '\\uD83C\\uDFAE' para juegos 🎮).\n" +
                "Salida esperada: {\"nroFactura\": 001-001017374,\"monto\": 4000, \"fecha\": \"03/03/2025\", \"descripcion\": \"Supermercado XYZ\", \"categoria\": \"comida\", \"emoji\": \"\\uD83C\\uDF79\"}";

        return askOpenAi(systemPrompt, invoiceText);
    }

    /**
     * Método para analizar transcripciones de voz y devolver JSON con emoji
     */
    public String parseVoiceWithAI(String transcript) {
        String systemPrompt = "Reescribe el siguiente texto si está mal escrito o si necesita más claridad.\n" +
                "Luego, analiza el texto mejorado y extrae cada gasto mencionado como un arreglo JSON valido.\n" +
                "Devuelve exclusivamente UN arreglo JSON válido.\n" +
                "Sigue estas reglas:\n" +
                "- Identifica cada gasto como una combinación de monto y descripción en el texto (ej. '12000 por una cocacola', '3000 en Pulp').\n" +
                "- Monto: Busca números con o sin separadores (ej. '22000' o '50000 guaranies') y devuélvelo como entero.\n" +
                "- Fecha: Busca referencias como 'hoy', 'ayer', o fechas específicas (default a hoy si no hay) y dame en formato dd/MM/yyyy.\n" +
                "- Descripcion: Identifica el propósito o artículo del gasto o comentario del artículo. Si no hay descripción clara, usa 'otros'.\n" +
                "- Categoria: Deduce según el contexto (comida, transporte, servicios, utiles, juegos, etc.).\n" +
                "- Emoji: Asigna un emoji relacionado con la categoría como una secuencia Unicode escapada (ej. Emoji: '\\uD83C\\uDF54' para comida 🍔, Emoji: '\\uD83C\\uDFAE' para juegos 🎮), si no por default pon '\\ud83e\\uddfe'.\n" +
                "Ejemplo:\n" +
                "Entrada: 'Hoy gasté 12000 en una cocacola'\n" +
                "Salida esperada: [{\"monto\": 12000, \"fecha\": \"13/03/2025\", \"descripcion\": \"una cocacola\", \"categoria\": \"bebidas\", \"emoji\": \"\\uD83C\\uDF79\"}]";

        String rawResponse = askOpenAi(systemPrompt, transcript);
        log.info("Raw AI Response: {}", rawResponse);

        // Usar regex para extraer el arreglo JSON entre [ y ]
        String jsonArray = extractJsonArray(rawResponse);
        log.info("Json extracted: {}", jsonArray);

        if (!isValidJson(jsonArray)) {
            log.error("No se encontró un arreglo JSON válido en la respuesta: {}", rawResponse);
            throw new IllegalStateException("No se pudo extraer un arreglo JSON válido de la respuesta de OpenAI");
        }

        log.info("Extracted JSON Array: {}", jsonArray);
        return jsonArray;
    }

    /**
     * Extrae el arreglo JSON entre [ y ] usando regex
     */
    private String extractJsonArray(String rawResponse) {
        int start = rawResponse.indexOf("```json");
        int end = rawResponse.lastIndexOf("```");

        if (start != -1 && end != -1 && start < end) {
            return rawResponse.substring(start + 7, end).trim(); // +7 para saltar "```json\n"
        }
        return null; // Si no hay JSON, retorna null
    }

    /**
     * Extrae el objeto JSON entre { y } usando regex
     */
    private String extractJsonObject(String rawResponse) {
        String regex = "\\{(?:[^\\{\\}]+|\\{(?:[^\\{\\}]+|\\{[^\\{\\}]*\\})*\\})*\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(rawResponse);
        return matcher.find() ? matcher.group(0) : null;
    }

    private boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parsea la respuesta de OpenAI a un objeto Expense
     */
    public Expense parseAIResponse(String aiResponse, Long userId) {
        try {
            String cleanedJson = extractJsonObject(aiResponse);
            if (cleanedJson == null) {
                log.error("No se encontró un objeto JSON válido en la respuesta: {}", aiResponse);
                throw new IllegalStateException("No se pudo extraer un objeto JSON válido");
            }

            JsonNode json = objectMapper.readTree(cleanedJson);

            if (json.isObject()) {
                String invoiceNumber = json.get("nroFactura").asText();
                String amount = json.get("monto").asText();
                String dateStr = json.get("fecha").asText();
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String description = json.get("descripcion").asText();
                User user = userRepository.findById(userId).orElseThrow();
                Category category = categoryService.findOrCreateCategory(
                        json.get("categoria").asText(),
                        json.get("descripcion").asText(),
                        json.get("emoji").asText(),
                        user
                );

                return Expense.builder()
                        .invoiceNumber(invoiceNumber)
                        .amount(amount)
                        .date(date)
                        .description(description)
                        .category(category)
                        .user(user)
                        .build();
            } else {
                throw new RuntimeException("El contenido del JSON no es un objeto válido");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing OpenAI response: " + e.getMessage(), e);
        }
    }
}