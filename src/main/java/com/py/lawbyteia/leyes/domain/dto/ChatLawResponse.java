package com.py.lawbyteia.leyes.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatLawResponse {
    private String answer;
    private LocalDateTime timestamp;
    private String originalQuestion;
    private List<String> relevantSources; // Referencias a artículos/documentos citados
    private String conversationId;

    public ChatLawResponse(String answer, LocalDateTime timestamp, String originalQuestion) {
        this.answer = answer;
        this.timestamp = timestamp;
        this.originalQuestion = originalQuestion;
    }
}
