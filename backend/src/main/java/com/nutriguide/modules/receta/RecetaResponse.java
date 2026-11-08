package com.nutriguide.modules.receta;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Receta tal como la consume el frontend (recetas.js / normalize.js).
 * <ul>
 *   <li>ingredientes y pasos se envían como listas.</li>
 *   <li>tiempoPreparacion va en minutos (número).</li>
 *   <li>informacionNutricional agrupa los valores POR PORCIÓN SERVIDA; un valor desconocido es null
 *       (nunca se inventa). informacionNutricionalTexto explica cómo se estimaron.</li>
 * </ul>
 */
public record RecetaResponse(
        Long id,
        String nombre,
        String descripcion,
        String imagenUrl,
        String categoria,
        List<String> ingredientes,
        List<String> pasos,
        Integer tiempoPreparacion,
        Nutricion informacionNutricional,
        String informacionNutricionalTexto) {

    public record Nutricion(
            BigDecimal calorias,
            BigDecimal proteinas,
            BigDecimal carbohidratos,
            BigDecimal grasas,
            BigDecimal fibra,
            BigDecimal tamanoPorcion,
            String unidadPorcion,
            String fuente) {
    }

    public static RecetaResponse fromEntity(Receta receta) {
        Nutricion nutricion = new Nutricion(
                receta.getCalorias(),
                receta.getProteinas(),
                receta.getCarbohidratos(),
                receta.getGrasas(),
                receta.getFibra(),
                receta.getPorcionGramos(),
                receta.getPorcionGramos() != null ? "g (1 porción servida)" : null,
                receta.getInformacionNutricional());
        return new RecetaResponse(
                receta.getId(),
                receta.getNombre(),
                receta.getDescripcion(),
                receta.getImagenUrl(),
                receta.getCategoria(),
                lineas(receta.getIngredientes(), false),
                lineas(receta.getPreparacion(), true),
                receta.getTiempoPreparacion(),
                nutricion,
                receta.getInformacionNutricional());
    }

    /** Divide un texto en líneas (también admite ';' como separador de ingredientes). */
    private static List<String> lineas(String texto, boolean quitarNumeracion) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        String separador = quitarNumeracion ? "\\r?\\n" : "\\r?\\n|;";
        return Arrays.stream(texto.split(separador))
                .map(String::trim)
                .map(s -> quitarNumeracion ? s.replaceFirst("^\\d+\\s*[.)-]\\s*", "") : s)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
