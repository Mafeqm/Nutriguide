package com.nutriguide.rag;

import com.nutriguide.config.AiConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Componente encargado de dividir el texto limpio de un documento en fragmentos pequeños (chunks)
 * con solapamiento contextual (overlap) para preservar el significado semántico al generar embeddings.
 */
@Component
public class TextChunker {

    private final AiConfig aiConfig;

    public TextChunker(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    /**
     * Divide un texto utilizando la configuración predeterminada del sistema RAG.
     */
    public List<String> dividirEnFragmentos(String textoLimpio) {
        return dividirEnFragmentos(
                textoLimpio,
                aiConfig.getChunkMaxChars(),
                aiConfig.getChunkOverlapChars());
    }

    /**
     * Divide un texto en fragmentos de tamaño máximo {@code maxChars} con un solapamiento
     * de {@code overlapChars} caracteres entre fragmentos consecutivos, respetando límites
     * de párrafos, oraciones y palabras.
     */
    public List<String> dividirEnFragmentos(String textoLimpio, int maxChars, int overlapChars) {
        List<String> fragmentos = new ArrayList<>();
        if (textoLimpio == null || textoLimpio.isBlank()) {
            return fragmentos;
        }
        String texto = textoLimpio.trim();
        int safeMaxChars = Math.max(200, maxChars);
        int safeOverlap = Math.max(0, Math.min(overlapChars, safeMaxChars / 2));

        if (texto.length() <= safeMaxChars) {
            fragmentos.add(texto);
            return fragmentos;
        }

        int inicio = 0;
        int longitud = texto.length();

        while (inicio < longitud) {
            int finCandidato = Math.min(inicio + safeMaxChars, longitud);
            if (finCandidato < longitud) {
                int corteNatural = buscarCorteNatural(texto, inicio, finCandidato);
                if (corteNatural > inicio + (safeMaxChars / 3)) {
                    finCandidato = corteNatural;
                }
            }

            String fragmento = texto.substring(inicio, finCandidato).trim();
            if (!fragmento.isEmpty()) {
                fragmentos.add(fragmento);
            }

            if (finCandidato >= longitud) {
                break;
            }

            int nuevoInicio = Math.max(inicio + 1, finCandidato - safeOverlap);
            // Ajustar nuevoInicio al comienzo de una palabra para no cortar términos a la mitad
            while (nuevoInicio < finCandidato && nuevoInicio > inicio
                    && !Character.isWhitespace(texto.charAt(nuevoInicio - 1))) {
                nuevoInicio--;
            }
            if (nuevoInicio <= inicio) {
                nuevoInicio = finCandidato;
            }
            inicio = nuevoInicio;
        }

        return fragmentos;
    }

    private int buscarCorteNatural(String texto, int inicio, int finMaximo) {
        String ventana = texto.substring(inicio, finMaximo);

        // Prioridad 1: Doble salto de línea (fin de párrafo)
        int ultimoParrafo = ventana.lastIndexOf("\n\n");
        if (ultimoParrafo >= ventana.length() / 2) {
            return inicio + ultimoParrafo + 2;
        }

        // Prioridad 2: Fin de oración (punto y espacio o punto y salto de línea)
        int ultimoPunto = Math.max(ventana.lastIndexOf(". "), ventana.lastIndexOf(".\n"));
        if (ultimoPunto >= ventana.length() / 2) {
            return inicio + ultimoPunto + 2;
        }

        // Prioridad 3: Salto de línea simple o punto y coma
        int ultimoSalto = Math.max(ventana.lastIndexOf('\n'), ventana.lastIndexOf("; "));
        if (ultimoSalto >= ventana.length() / 2) {
            return inicio + ultimoSalto + 1;
        }

        // Prioridad 4: Último espacio en blanco
        int ultimoEspacio = ventana.lastIndexOf(' ');
        if (ultimoEspacio > 0) {
            return inicio + ultimoEspacio + 1;
        }

        return finMaximo;
    }
}

