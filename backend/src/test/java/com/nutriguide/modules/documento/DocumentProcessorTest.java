package com.nutriguide.modules.documento;

import com.nutriguide.common.DocumentProcessingException;
import com.nutriguide.support.PdfDePrueba;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validación, extracción y limpieza del texto de los documentos antes de enviarlos a n8n.
 */
class DocumentProcessorTest {

    private final DocumentProcessor processor = new DocumentProcessor();

    @Test
    void extraeTextoDeUnPdfReal() throws Exception {
        byte[] pdf = PdfDePrueba.crear("GUIA DEL CALCIO. El calcio participa en la mineralizacion osea y la contraccion muscular.");
        assertThat(processor.extraerYLimpiarTexto(pdf, "pdf")).contains("calcio participa en la mineralizacion osea");
    }

    @Test
    void limpiaTextoYEliminaNumerosDePagina() {
        String sucio = "Primera línea\r\n\r\n\r\n\r\nPágina 2\n   - 3 -\nSegunda\t\tlínea con   espacios";
        assertThat(processor.limpiarTexto(sucio)).isEqualTo("Primera línea\n\nSegunda línea con espacios");
    }

    @Test
    void rechazaArchivosNoValidos() {
        assertThatThrownBy(() -> processor.extraerYLimpiarTexto("no es pdf".getBytes(), "pdf"))
                .isInstanceOf(DocumentProcessingException.class).hasMessageContaining("cabecera PDF");
        assertThatThrownBy(() -> processor.extraerYLimpiarTexto(new byte[]{'M', 'Z', 0, 1, 2}, "txt"))
                .isInstanceOf(DocumentProcessingException.class).hasMessageContaining("binarios");
        assertThatThrownBy(() -> processor.extraerYLimpiarTexto(new byte[]{(byte) 0xC3, (byte) 0x28, 'a', 'b'}, "txt"))
                .isInstanceOf(DocumentProcessingException.class).hasMessageContaining("UTF-8");
        assertThatThrownBy(() -> processor.extraerYLimpiarTexto("corto".getBytes(StandardCharsets.UTF_8), "txt"))
                .isInstanceOf(DocumentProcessingException.class).hasMessageContaining("suficiente texto");
        assertThat(processor.obtenerExtension("informe.final.PDF")).isEqualTo("pdf");
    }

    @Test
    void calculaHashParaDetectarDuplicados() {
        String a = processor.calcularSha256("contenido".getBytes(StandardCharsets.UTF_8));
        assertThat(a).hasSize(64).isEqualTo(processor.calcularSha256("contenido".getBytes(StandardCharsets.UTF_8)));
    }
}
