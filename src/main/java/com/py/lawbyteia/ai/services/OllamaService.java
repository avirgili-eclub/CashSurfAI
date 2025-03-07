package com.py.lawbyteia.ai.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.py.lawbyteia.ai.domain.dto.OllamaEmbeddingResponse;
import com.py.lawbyteia.finanzas.domain.models.entity.Expense;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class OllamaService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS); // Para parsear JSON
    private final String OLLAMA_URL = "http://localhost:11434/api/generate"; // Ollama por defecto corre en este puerto

    @Value("${spring.ai.ollama.embedding.model}")
    private String LLM_MODEL;

    /**
     * Método genérico para consultas a Ollama
     */
    public String askOllama(String model, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("prompt", prompt);
        payload.put("stream", false); // Explicitly disable streaming

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(OLLAMA_URL, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return extractResponseContent(response.getBody());
            } else {
                throw new RuntimeException("Error en la respuesta de Ollama: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al comunicarse con Ollama: " + e.getMessage(), e);
        }
//        String response = restTemplate.postForObject(OLLAMA_URL, payload, String.class);
//
//        return extractResponseContent(response); // Extraer el contenido útil de la respuesta
    }

    /**
     * Método específico para consultas legales (como el original)
     */
    public String askDeepSeek(String context, String query) {
        String prompt = "IMPORTANT - Use the following legal information to answer in Spanish: " + context + "\nQuestion: " + query;
        return askOllama(LLM_MODEL, prompt);
    }

    /**
     * Método para analizar facturas y devolver un Expense en formato JSON
     */
    public Expense parseInvoiceWithAI(String invoiceText) {
        String prompt = buildInvoiceParsingPrompt(invoiceText);
        String aiResponse = askOllama(LLM_MODEL, prompt);
        return parseAIResponse(aiResponse);
    }

    /**
     * Construye un prompt optimizado para analizar facturas
     */
    private String buildInvoiceParsingPrompt(String invoiceText) {
        return "Extrae el monto, fecha, descripción y categoría de esta factura y devuélvelo SOLO como un objeto JSON, sin texto adicional, explicaciones ni delimitadores como ```json o ```. Usa estas reglas:\n" +
                "- Monto: Busca 'Total', '$', o números con decimales y devuélvelo como un número sin separadores de miles ni puntos (ej. 22000 en lugar de 22.000).\n" +
                "- Fecha: Busca formatos como 'dd/mm/yyyy' o 'dd-mm-yyyy'.\n" +
                "- Descripción: El nombre del comercio o una línea representativa.\n" +
                "- Categoría: Deduce según el contexto (comida, transporte, servicios, otros).\n" +
                "Ejemplos:\n" +
                "```\n" +
                "Supermercado XYZ\n" +
                "Fecha: 03/03/2025\n" +
                "Total: $4.000,00\n" +
                "```\n" +
                "Supermercado XYZ\n" +
                "Fecha: 03/03/2025\n" +
                "Total: PYG 4.000\n" +
                "```\n" +
                "Respuesta esperada:\n" +
                "```json\n" +
                "{\"monto\": 4.000, \"date\": \"03/03/2025\", \"description\": \"Supermercado XYZ\", \"category\": \"comida\"}\n" +
                "```\n" +
                "Texto de la factura:\n" +
                "```\n" +
                invoiceText + "\n" +
                "```";
    }

    /**
     * Parsea la respuesta de Ollama a un objeto Expense
     */
    private Expense parseAIResponse(String aiResponse) {
        try {
            // Limpiar la respuesta para eliminar delimitadores residuales como ```
            String cleanedJson = aiResponse.replaceAll("```", "").trim();
            System.out.println("cleanedJson: " + cleanedJson); // Para depuración

            // Parsear el JSON limpio
            JsonNode json = objectMapper.readTree(cleanedJson);

            if (json.isObject()) {
                // Tomar el monto como String directamente
                String amount = json.get("monto").asText(); // Mantiene "22.000" como está

                String dateStr = json.get("date").asText();
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")); // Formato con barras
                String description = json.get("description").asText();
                String category = json.get("category").asText();

                return new Expense(amount, date, description, category, null);
            } else {
                throw new RuntimeException("El contenido del JSON no es un objeto válido");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing AI response: " + e.getMessage(), e);
        }
    }

    /**
     * Extrae el contenido útil de la respuesta de Ollama
     */
    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response);
            return json.get("response").asText(); // Extrae el campo "response"
        } catch (Exception e) {
            return response; // Fallback si no se puede parsear
        }
    }
//    private String extractResponseContent(String rawResponse) {
//        try {
//            JsonNode json = objectMapper.readTree(rawResponse);
//            return json.get("response").asText(); // Ollama devuelve la respuesta en el campo "response"
//        } catch (Exception e) {
//            return rawResponse; // Fallback si no se puede parsear
//        }
//    }

/*    Meta 2: Categorización Automática Avanzada (Mediano Plazo - 3 Meses)
    Objetivo: Que el modelo deduzca categorías con un 85% de precisión en facturas ambiguas (ej. "Tienda ABC" → "otros" o "comida" según contexto).
    Acción:
    Entrena el modelo con ejemplos específicos o usa un dataset de categorías comunes.
    Añade una función en OllamaService para refinar categorías con contexto histórico del usuario:*/

        public String refineCategory (String description, List < Expense > userHistory){
            String prompt = "Dada esta descripción: '" + description + "' y el historial de gastos: " + userHistory.toString() +
                    ", deduce la categoría (comida, transporte, servicios, otros).";
            return askOllama(LLM_MODEL, prompt);
        }
//
//    Meta 3: Proyecciones Financieras Básicas (Mediano Plazo - 6 Meses)
//    Objetivo: Implementar un método que use IA para predecir gastos mensuales basándose en facturas procesadas.
//    Acción:
//    Añade un método como:

        public String predictMonthlyExpenses (List < Expense > expenses) {
            String prompt = "Dada esta lista de gastos: " + expenses.toString() +
                    ", predice el gasto mensual promedio y posibles categorías altas para el próximo mes en formato JSON.";
            return askOllama(LLM_MODEL, prompt);
        }
    }
