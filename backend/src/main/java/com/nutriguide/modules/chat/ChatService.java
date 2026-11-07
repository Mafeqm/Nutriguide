package com.nutriguide.modules.chat;

import com.nutriguide.rag.RagService;
import org.springframework.stereotype.Service;

/**
 * Servicio del módulo de chat que delega la consulta del usuario al flujo RAG verificable.
 */
@Service
public class ChatService {

    private final RagService ragService;

    public ChatService(RagService ragService) {
        this.ragService = ragService;
    }

    public ChatResponse preguntar(ChatRequest request) {
        return ragService.responderPregunta(request.getPregunta().trim());
    }
}

