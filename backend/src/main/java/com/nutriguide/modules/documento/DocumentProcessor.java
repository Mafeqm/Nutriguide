package com.nutriguide.modules.documento;

import com.nutriguide.common.DocumentProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

/**
 * Componente encargado de validar archivos (.pdf y .txt), calcular su huella SHA-256,
 * extraer su contenido textual y limpiar el texto para el flujo RAG.
 */
@Component
public class DocumentProcessor {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L; // 10 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "txt");

    /**
     * Valida las restricciones de seguridad y formato de un archivo subido.
     */
    public void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new DocumentProcessingException("El archivo enviado está vacío o no fue proporcionado.");
        }
        if (archivo.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new DocumentProcessingException("El archivo supera el límite máximo permitido de 10 MB.");
        }
        String originalFilename = archivo.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new DocumentProcessingException("El archivo debe tener un nombre válido.");
        }
        String extension = obtenerExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new DocumentProcessingException(
                    "Formato de archivo no soportado ('." + extension + "'). Solo se permiten archivos .pdf y .txt.");
        }
    }

    /**
     * Extrae y limpia el texto de un archivo MultipartFile (.pdf o .txt).
     */
    public String extraerYLimpiarTexto(MultipartFile archivo) {
        validarArchivo(archivo);
        String extension = obtenerExtension(archivo.getOriginalFilename());
        try {
            byte[] bytes = archivo.getBytes();
            return extraerYLimpiarTexto(bytes, extension);
        } catch (IOException ex) {
            throw new DocumentProcessingException("Error al leer los bytes del archivo: " + ex.getMessage(), ex);
        }
    }

    /**
     * Extrae y limpia el texto a partir de un arreglo de bytes y su extensión ('pdf' o 'txt').
     */
    public String extraerYLimpiarTexto(byte[] bytes, String extension) {
        if (bytes == null || bytes.length == 0) {
            throw new DocumentProcessingException("El contenido del documento está vacío.");
        }
        String ext = extension.toLowerCase(Locale.ROOT).replace(".", "");
        String textoCrudo;
        if ("pdf".equals(ext)) {
            textoCrudo = extraerTextoDePdf(bytes);
        } else if ("txt".equals(ext)) {
            textoCrudo = decodificarTextoPlano(bytes);
        } else {
            throw new DocumentProcessingException("Extensión no soportada: " + extension);
        }

        String textoLimpio = limpiarTexto(textoCrudo);
        if (textoLimpio.length() < 20) {
            throw new DocumentProcessingException(
                    "El documento no contiene suficiente texto legible para ser procesado en el sistema RAG.");
        }
        return textoLimpio;
    }

    /**
     * Un .txt debe ser texto UTF-8 válido: se rechazan binarios renombrados (bytes nulos o
     * secuencias UTF-8 inválidas) para no indexar contenido corrupto o peligroso.
     */
    private String decodificarTextoPlano(byte[] bytes) {
        for (byte b : bytes) {
            if (b == 0) {
                throw new DocumentProcessingException("El archivo .txt contiene datos binarios; no es un texto válido.");
            }
        }
        try {
            String texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return texto.startsWith("﻿") ? texto.substring(1) : texto;
        } catch (CharacterCodingException ex) {
            throw new DocumentProcessingException("El archivo .txt debe estar codificado en UTF-8.");
        }
    }

    private String extraerTextoDePdf(byte[] pdfBytes) {
        if (pdfBytes.length < 4 || pdfBytes[0] != '%' || pdfBytes[1] != 'P' || pdfBytes[2] != 'D' || pdfBytes[3] != 'F') {
            throw new DocumentProcessingException("El archivo con extensión .pdf no posee una cabecera PDF válida.");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.isEncrypted()) {
                throw new DocumentProcessingException("No se permiten archivos PDF protegidos con contraseña.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (DocumentProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocumentProcessingException("No fue posible extraer texto del archivo PDF: " + ex.getMessage(), ex);
        }
    }

    /**
     * Limpia y normaliza el texto extraído eliminando caracteres de control,
     * espacios redundantes y artefactos de paginación.
     */
    public String limpiarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        String normalizado = texto
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                // Eliminar caracteres de control invisibles excepto saltos de línea y tabuladores
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", " ")
                // Reemplazar tabuladores y espacios horizontales repetidos por un único espacio
                .replaceAll("[ \\t]+", " ")
                // Eliminar líneas con números de página aislados (ej. "- 1 -", "Página 2")
                .replaceAll("(?m)^\\s*(?:P[áa]gina\\s+\\d+|[-–—]?\\s*\\d+\\s*[-–—]?)\\s*$", "")
                // Reducir 3 o más saltos de línea seguidos a máximo 2 (separador de párrafo)
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        return normalizado;
    }

    /**
     * Calcula el hash SHA-256 en formato hexadecimal para control de archivos duplicados.
     */
    public String calcularSha256(byte[] contenido) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contenido);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la JVM", ex);
        }
    }

    public String obtenerExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT).trim();
    }
}

