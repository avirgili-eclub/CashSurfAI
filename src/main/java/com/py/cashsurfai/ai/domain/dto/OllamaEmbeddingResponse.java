package com.py.cashsurfai.ai.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class OllamaEmbeddingResponse {
    private List<Float> embedding;
}