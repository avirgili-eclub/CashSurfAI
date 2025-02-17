package com.py.lawbyteia.ai.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OllamaService {
    private final RestTemplate restTemplate;
    private final String OLLAMA_URL = "http://localhost:11434/api/generate"; // Ollama por defecto corre en este puerto

    public String askDeepSeek(String context, String query) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "deepseek"); // Usa tu modelo descargado en Ollama
        payload.put("prompt", "Use the following legal information to answer in spanish: " + context + "\nQuestion: " + query);

        return restTemplate.postForObject(OLLAMA_URL, payload, String.class);
    }

}
