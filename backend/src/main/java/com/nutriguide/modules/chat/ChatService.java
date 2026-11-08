package com.nutriguide.modules.chat;

import com.nutriguide.ia.N8nClient;
import org.springframework.stereotype.Service;

/**
 * Delega la pregunta al flujo RAG de n8n (Qdrant + Gemini) y devuelve la respuesta con sus fuentes.
 * El frontend sigue usando POST /api/chat/preguntar sin conocer n8n.
 */
@Service
public class ChatService {

    private final N8nClient n8nClient;

    public ChatService(N8nClient n8nClient) {
        this.n8nClient = n8nClient;
    }

    public ChatResponse preguntar(ChatRequest request) {
        return n8nClient.preguntar(request.getPregunta().trim());
    }
}
