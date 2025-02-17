package com.py.lawbyteia.leyes.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatLawRequest {
    private String question;
    private String conversationId; // Para mantener contexto de la conversación
    private List<String> previousMessages; // Historial de mensajes si es necesario
}