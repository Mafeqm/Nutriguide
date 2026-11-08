package com.nutriguide.common;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilidades para búsquedas por nombre insensibles a mayúsculas y tildes
 * ("brocoli" encuentra "Brócoli"). El patrón se usa como parámetro de una consulta
 * parametrizada, con los comodines de LIKE escapados.
 */
public final class BusquedaTexto {

    private BusquedaTexto() {
    }

    public static String patronLike(String texto) {
        String normalizado = Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String escapado = normalizado
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
