package com.py.lawbyteia.leyes.controller;

import com.py.lawbyteia.leyes.domain.dto.ChatLawRequest;
import com.py.lawbyteia.leyes.domain.dto.ChatLawResponse;
import com.py.lawbyteia.leyes.domain.record.LegalResponse;
import com.py.lawbyteia.leyes.services.LawSearchService;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/legal/chat")
@Slf4j
public class LegalChatController {

    private final LawSearchService lawSearchService;
    private final ChatClient chatClient;

    public LegalChatController(LawSearchService lawSearchService, ChatClient.Builder chatClient) {
        this.lawSearchService = lawSearchService;

        OllamaApi ollamaApi = new OllamaApi("http://localhost:11434");

        OllamaOptions ollamaOptions = OllamaOptions.builder()
                .model("deepseek-r1:7b")// Opcional: Configura la temperatura si lo necesitas
                .build();

        // ✅ Pasar los nuevos parámetros requeridos
//        FunctionCallbackResolver functionCallbackResolver = new FunctionCallbackResolver(Collections.emptyList());
        List<FunctionCallback> toolFunctionCallbacks = Collections.emptyList();
        ObservationRegistry observationRegistry = ObservationRegistry.NOOP;
        ModelManagementOptions modelManagementOptions = ModelManagementOptions.defaults(); // Nuevo en M5


        // Configurar las opciones para el modelo correcto
        OllamaChatModel chatModel = new OllamaChatModel(ollamaApi, ollamaOptions, null,
                toolFunctionCallbacks, observationRegistry, modelManagementOptions);

        this.chatClient = ChatClient.builder(chatModel, ObservationRegistry.NOOP, null).build();
    }


    @PostMapping("/ask")
    public ResponseEntity<ChatLawResponse> askLegalQuestion(@RequestBody ChatLawRequest request) {
        try {
            // Usar LawSearchService para obtener el contexto legal relevante
            String legalContext = lawSearchService.processLegalQuery(request.getQuestion());
            String prompt = """
                    %s
                    
                    Pregunta del usuario: %s
                    """.formatted(legalContext, request.getQuestion());

            log.info("El legal context es: {}", legalContext);
            log.info("El prompt es: {}", prompt);

            var response = chatClient.prompt()
                    .user(legalContext)
                    .call();

            return ResponseEntity.ok(new ChatLawResponse(
                    response.content(),
                    LocalDateTime.now(),
                    request.getQuestion()
            ));

        } catch (Exception e) {
            log.error("Error al procesar la pregunta legal: {}", request.getQuestion(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatLawResponse(
                            "Lo siento, hubo un error procesando tu consulta.",
                            LocalDateTime.now(),
                            request.getQuestion()
                    ));
        }
    }

    @PostMapping(value = "/ask-stream", produces = "text/event-stream")
    public Flux<String> askLegalQuestionStreaming(@RequestBody ChatLawRequest request) {
        String legalContext = lawSearchService.processLegalQuery(request.getQuestion());
        String prompt = """
                %s
                User Question: %s
                """.formatted(legalContext, request.getQuestion());

        // Usamos 'stream()' para obtener respuestas reactivas en tiempo real
        // Devuelve el Flux de respuesta sin suscribir directamente aquí
        return Flux.create(sink -> {
            try {
                var responseStream = chatClient.prompt()
                        .user(prompt)
                        .stream(); // Usamos 'stream'

                responseStream.content()
                        .doOnNext(content -> {
                            log.info("Tipo de contenido: {}", content.getClass().getName());
                            sink.next(content);

                        })
                        .doOnTerminate(sink::complete)
                        .subscribe();  // No suscribimos aquí, Spring WebFlux se encarga
            } catch (Exception e) {
                log.error("Error al procesar la pregunta legal: {}", request.getQuestion(), e);
                // Emitir una respuesta de error en caso de excepción
                sink.next("Lo siento, hubo un error procesando tu consulta.");
                sink.complete();
            }
        });
    }


//    @PostMapping(value = "/ask-stream", produces = "text/event-stream")
//    public Flux<String> askLegalQuestionStreaming(@RequestBody ChatLawRequest request) {
//    }


    @GetMapping("/ask")
    public LegalResponse askLegalQuestion(@RequestParam String question) {
        String prompt = """
                Responde a la siguiente pregunta basándote SOLO en los documentos legales cargados en la base de datos. 
                Si la respuesta no está en los documentos, responde "No tengo información sobre eso."
                
                Pregunta: %s
                """.formatted(question);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(LegalResponse.class);
    }

}
