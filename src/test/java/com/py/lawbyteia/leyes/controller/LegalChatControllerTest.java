//package com.py.lawbyteia.leyes.controller;
//
//import com.py.lawbyteia.leyes.domain.dto.ChatLawRequest;
//import com.py.lawbyteia.leyes.services.LawSearchService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.messages.AssistantMessage;
//import org.springframework.ai.chat.model.ChatResponse;
//import org.springframework.ai.chat.model.Generation;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.util.List;
//
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//class LegalChatControllerTest {
//
//    private MockMvc mockMvc;
//
//    @Mock
//    private LawSearchService lawSearchService;
//
//    @Mock
//    private ChatClient chatClient;
//
//    @InjectMocks
//    private LegalChatController legalChatController;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//        mockMvc = MockMvcBuilders.standaloneSetup(legalChatController).build();
//    }
//
//
//    @Test
//    void askLegalQuestion() {
//    }
//
//    @Test
//    void testAskLegalQuestion() throws Exception {
//        // Simulamos la consulta del usuario
//        String userQuestion = "¿Cuáles son mis derechos laborales en Paraguay?";
//        ChatLawRequest request = new ChatLawRequest(userQuestion); // Usamos el nuevo constructor
//
//        // Simulamos la respuesta esperada de LawSearchService
//        String mockLegalContext = "Artículo 123: Derechos del trabajador en Paraguay...";
//        when(lawSearchService.processLegalQuery(userQuestion)).thenReturn(mockLegalContext);
//
//        String respuestaIA = "Esta es una respuesta de prueba";
//        // Crear AssistantMessage con el contenido de la respuesta
//        AssistantMessage assistantMessage = new AssistantMessage(respuestaIA);
//
//        // Simulamos la respuesta del modelo de IA
//        Generation generation = new Generation(assistantMessage);
//        // Crear ChatResponse con la lista de Generations
//        ChatResponse chatResponse = new ChatResponse(List.of(generation));
//        // Mockear el comportamiento del chatClient
////        when(chatClient.prompt()).thenReturn(mock(ChatClient.Prompt.class));
////        when(chatClient.prompt().user(anyString())).thenReturn(mock(ChatClient.Prompt.class));
////        when(chatClient.prompt().user(anyString()).call()).thenReturn(chatResponse);
//
//        // Simulación de la solicitud HTTP
//        mockMvc.perform(post("/api/legal/chat/ask")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"question\": \"" + userQuestion + "\"}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.response").value("En Paraguay, los derechos laborales incluyen..."))
//                .andExpect(jsonPath("$.timestamp").exists())
//                .andExpect(jsonPath("$.question").value(userQuestion));
//
//        // Verificamos que se llamaron los métodos con los valores correctos
//        verify(lawSearchService, times(1)).processLegalQuery(userQuestion);
//        verify(chatClient, times(1)).prompt();
//    }
//}