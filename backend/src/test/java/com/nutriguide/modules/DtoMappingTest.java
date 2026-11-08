package com.nutriguide.modules;

import com.nutriguide.common.BusquedaTexto;
import com.nutriguide.modules.receta.Receta;
import com.nutriguide.modules.receta.RecetaResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conversión de entidades al formato que consume el frontend.
 */
class DtoMappingTest {

    @Test
    void recetaSinDatosNutricionalesNoInventaValores() {
        Receta receta = new Receta("Ensalada", "Descripción", null,
                "Lechuga; Tomate\nAceite", "1. Lavar.\n2) Mezclar.", 10,
                "Información nutricional no disponible.", "Ensaladas");

        RecetaResponse r = RecetaResponse.fromEntity(receta);

        assertThat(r.ingredientes()).containsExactly("Lechuga", "Tomate", "Aceite");
        assertThat(r.pasos()).containsExactly("Lavar.", "Mezclar.");
        assertThat(r.informacionNutricional().calorias()).isNull();
        assertThat(r.informacionNutricional().proteinas()).isNull();
        assertThat(r.informacionNutricional().tamanoPorcion()).isNull();
        assertThat(r.informacionNutricional().unidadPorcion()).isNull();
    }

    @Test
    void recetaConDatosLosExponePorPorcion() {
        Receta receta = new Receta("Bowl", "Desc", null, "Quinua", "1. Cocinar.", 25, "Estimación USDA.", "Almuerzos");
        receta.setCalorias(new BigDecimal("510.00"));
        receta.setPorcionGramos(new BigDecimal("435.00"));

        RecetaResponse r = RecetaResponse.fromEntity(receta);

        assertThat(r.informacionNutricional().calorias()).isEqualByComparingTo("510");
        assertThat(r.informacionNutricional().tamanoPorcion()).isEqualByComparingTo("435");
        assertThat(r.informacionNutricional().unidadPorcion()).isEqualTo("g (1 porción servida)");
        assertThat(r.informacionNutricional().fuente()).isEqualTo("Estimación USDA.");
        assertThat(r.tiempoPreparacion()).isEqualTo(25);
    }

    @Test
    void patronDeBusquedaIgnoraTildesYEscapaComodines() {
        assertThat(BusquedaTexto.patronLike("  Brócoli ")).isEqualTo("%brocoli%");
        assertThat(BusquedaTexto.patronLike("50%_x")).isEqualTo("%50\\%\\_x%");
    }
}
