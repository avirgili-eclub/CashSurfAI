package com.py.lawbyteia.leyes.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatLawRequest {
    private String question;
    private String conversationId; // Para mantener contexto de la conversación
    private List<String> previousMessages; // Historial de mensajes si es necesario

    //Solo para usar en la clase TEST
    public ChatLawRequest(String question) {
        this.question = question;
        this.conversationId = null; // Opcional, depende de si lo necesitas
        this.previousMessages = new ArrayList<>(); // Lista vacía si no se pasa
    }
}