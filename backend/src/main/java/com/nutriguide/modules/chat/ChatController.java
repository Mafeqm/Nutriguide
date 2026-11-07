package com.nutriguide.modules.chat;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST del asistente inteligente basado en RAG.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Endpoint principal del asistente educativo nutricional basado en RAG + pgvector.
     */
    @PostMapping("/preguntar")
    public ResponseEntity<ChatResponse> preguntar(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.preguntar(request);
        return ResponseEntity.ok(response);
    }
}

