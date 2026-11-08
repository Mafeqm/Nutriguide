package com.nutriguide.modules.chat;

import java.util.List;

/**
 * DTO de salida del asistente inteligente basado en RAG, incluyendo la respuesta generada
 * y la lista de fuentes documentales recuperadas de Qdrant por el flujo n8n.
 */
public class ChatResponse {

    private String respuesta;
    private List<FuenteResponse> fuentes;
    private boolean informacionSuficiente;
    private String advertenciaEducativa;

    public ChatResponse() {
    }

    public ChatResponse(
            String respuesta,
            List<FuenteResponse> fuentes,
            boolean informacionSuficiente,
            String advertenciaEducativa) {
        this.respuesta = respuesta;
        this.fuentes = fuentes;
        this.informacionSuficiente = informacionSuficiente;
        this.advertenciaEducativa = advertenciaEducativa;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public List<FuenteResponse> getFuentes() {
        return fuentes;
    }

    public void setFuentes(List<FuenteResponse> fuentes) {
        this.fuentes = fuentes;
    }

    public boolean isInformacionSuficiente() {
        return informacionSuficiente;
    }

    public void setInformacionSuficiente(boolean informacionSuficiente) {
        this.informacionSuficiente = informacionSuficiente;
    }

    public String getAdvertenciaEducativa() {
        return advertenciaEducativa;
    }

    public void setAdvertenciaEducativa(String advertenciaEducativa) {
        this.advertenciaEducativa = advertenciaEducativa;
    }

    /**
     * Representa una fuente documental verificada recuperada de Qdrant por el flujo n8n.
     */
    public static class FuenteResponse {
        private String titulo;
        private String contenido;
        private Long documentoId;
        private Integer numeroFragmento;
        private String categoria;
        private String fuenteOrigen;
        private Double similitud;

        public FuenteResponse() {
        }

        public FuenteResponse(
                String titulo,
                String contenido,
                Long documentoId,
                Integer numeroFragmento,
                String categoria,
                String fuenteOrigen,
                Double similitud) {
            this.titulo = titulo;
            this.contenido = contenido;
            this.documentoId = documentoId;
            this.numeroFragmento = numeroFragmento;
            this.categoria = categoria;
            this.fuenteOrigen = fuenteOrigen;
            this.similitud = similitud;
        }

        public String getTitulo() {
            return titulo;
        }

        public void setTitulo(String titulo) {
            this.titulo = titulo;
        }

        public String getContenido() {
            return contenido;
        }

        public void setContenido(String contenido) {
            this.contenido = contenido;
        }

        public Long getDocumentoId() {
            return documentoId;
        }

        public void setDocumentoId(Long documentoId) {
            this.documentoId = documentoId;
        }

        public Integer getNumeroFragmento() {
            return numeroFragmento;
        }

        public void setNumeroFragmento(Integer numeroFragmento) {
            this.numeroFragmento = numeroFragmento;
        }

        public String getCategoria() {
            return categoria;
        }

        public void setCategoria(String categoria) {
            this.categoria = categoria;
        }

        public String getFuenteOrigen() {
            return fuenteOrigen;
        }

        public void setFuenteOrigen(String fuenteOrigen) {
            this.fuenteOrigen = fuenteOrigen;
        }

        public Double getSimilitud() {
            return similitud;
        }

        public void setSimilitud(Double similitud) {
            this.similitud = similitud;
        }
    }
}

