package com.nutriguide.modules.receta;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DTO de salida para representar una receta educativa.
 * Compatible con normalize.js del frontend (informacionNutricional estructurada y campos planos).
 */
public class RecetaResponse {

    private static final Pattern KCAL_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*kcal", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROT_PATTERN = Pattern.compile("Prote[íi]nas\\s*:\\s*(\\d+(?:\\.\\d+)?)\\s*g", Pattern.CASE_INSENSITIVE);
    private static final Pattern CARB_PATTERN = Pattern.compile("Carbohidratos\\s*:\\s*(\\d+(?:\\.\\d+)?)\\s*g", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRAS_PATTERN = Pattern.compile("Grasas(?:\\s+totales)?\\s*:\\s*(\\d+(?:\\.\\d+)?)\\s*g", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIBR_PATTERN = Pattern.compile("Fibra(?:\\s+diet[ée]tica)?\\s*:\\s*(\\d+(?:\\.\\d+)?)\\s*g", Pattern.CASE_INSENSITIVE);
    private static final Pattern PORCION_G_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*g\\)");

    private Long id;
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    private String ingredientes;
    private List<String> listaIngredientes;
    private String preparacion;
    private List<String> pasos;
    private Integer tiempoPreparacion;
    private String tiempoFormateado;
    private String informacionNutricionalTexto;
    private NutricionReceta informacionNutricional;
    private BigDecimal calorias;
    private BigDecimal proteinas;
    private BigDecimal carbohidratos;
    private BigDecimal grasas;
    private BigDecimal fibra;
    private BigDecimal tamanoPorcion;
    private String unidadPorcion;
    private String fuente;
    private String categoria;

    public RecetaResponse() {
    }

    public static RecetaResponse fromEntity(Receta receta) {
        RecetaResponse response = new RecetaResponse();
        response.setId(receta.getId());
        response.setNombre(receta.getNombre());
        response.setDescripcion(receta.getDescripcion());
        response.setImagenUrl(receta.getImagenUrl());

        if (receta.getIngredientes() != null) {
            List<String> items = Arrays.stream(receta.getIngredientes().split("[;\\n]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            response.setListaIngredientes(items);
            response.setIngredientes(String.join("\n", items));
        }

        response.setPreparacion(receta.getPreparacion());
        if (receta.getPreparacion() != null) {
            List<String> steps = Arrays.stream(receta.getPreparacion().split("\\r?\\n"))
                    .map(s -> s.replaceFirst("^\\s*\\d+[.)-]?\\s*", "").trim())
                    .filter(s -> !s.isEmpty())
                    .toList();
            response.setPasos(steps);
        }

        response.setTiempoPreparacion(receta.getTiempoPreparacion());
        response.setTiempoFormateado(receta.getTiempoPreparacion() + " min");

        String infoTexto = receta.getInformacionNutricional() != null ? receta.getInformacionNutricional() : "";
        response.setInformacionNutricionalTexto(infoTexto);

        BigDecimal kcal = extraerValor(KCAL_PATTERN, infoTexto, new BigDecimal("350"));
        BigDecimal prot = extraerValor(PROT_PATTERN, infoTexto, new BigDecimal("15"));
        BigDecimal carb = extraerValor(CARB_PATTERN, infoTexto, new BigDecimal("45"));
        BigDecimal gras = extraerValor(GRAS_PATTERN, infoTexto, new BigDecimal("10"));
        BigDecimal fibr = extraerValor(FIBR_PATTERN, infoTexto, new BigDecimal("8"));
        BigDecimal porcion = extraerValor(PORCION_G_PATTERN, infoTexto, new BigDecimal("300"));

        response.setCalorias(kcal);
        response.setProteinas(prot);
        response.setCarbohidratos(carb);
        response.setGrasas(gras);
        response.setFibra(fibr);
        response.setTamanoPorcion(porcion);
        response.setUnidadPorcion("g (1 porción servida)");
        response.setFuente(infoTexto);

        NutricionReceta nutricionObj = new NutricionReceta(
                kcal, prot, carb, gras, fibr, porcion, "g (1 porción servida)", infoTexto
        );
        response.setInformacionNutricional(nutricionObj);
        response.setCategoria(receta.getCategoria());
        return response;
    }

    private static BigDecimal extraerValor(Pattern pattern, String texto, BigDecimal valorPorDefecto) {
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return valorPorDefecto;
    }

    public record NutricionReceta(
            BigDecimal calorias,
            BigDecimal proteinas,
            BigDecimal carbohidratos,
            BigDecimal grasas,
            BigDecimal fibra,
            BigDecimal tamanoPorcion,
            String unidadPorcion,
            String fuente
    ) {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(String ingredientes) {
        this.ingredientes = ingredientes;
    }

    public List<String> getListaIngredientes() {
        return listaIngredientes;
    }

    public void setListaIngredientes(List<String> listaIngredientes) {
        this.listaIngredientes = listaIngredientes;
    }

    public String getPreparacion() {
        return preparacion;
    }

    public void setPreparacion(String preparacion) {
        this.preparacion = preparacion;
    }

    public List<String> getPasos() {
        return pasos;
    }

    public void setPasos(List<String> pasos) {
        this.pasos = pasos;
    }

    public Integer getTiempoPreparacion() {
        return tiempoPreparacion;
    }

    public void setTiempoPreparacion(Integer tiempoPreparacion) {
        this.tiempoPreparacion = tiempoPreparacion;
    }

    public String getTiempoFormateado() {
        return tiempoFormateado;
    }

    public void setTiempoFormateado(String tiempoFormateado) {
        this.tiempoFormateado = tiempoFormateado;
    }

    public String getInformacionNutricionalTexto() {
        return informacionNutricionalTexto;
    }

    public void setInformacionNutricionalTexto(String informacionNutricionalTexto) {
        this.informacionNutricionalTexto = informacionNutricionalTexto;
    }

    public NutricionReceta getInformacionNutricional() {
        return informacionNutricional;
    }

    public void setInformacionNutricional(NutricionReceta informacionNutricional) {
        this.informacionNutricional = informacionNutricional;
    }

    public BigDecimal getCalorias() {
        return calorias;
    }

    public void setCalorias(BigDecimal calorias) {
        this.calorias = calorias;
    }

    public BigDecimal getProteinas() {
        return proteinas;
    }

    public void setProteinas(BigDecimal proteinas) {
        this.proteinas = proteinas;
    }

    public BigDecimal getCarbohidratos() {
        return carbohidratos;
    }

    public void setCarbohidratos(BigDecimal carbohidratos) {
        this.carbohidratos = carbohidratos;
    }

    public BigDecimal getGrasas() {
        return grasas;
    }

    public void setGrasas(BigDecimal grasas) {
        this.grasas = grasas;
    }

    public BigDecimal getFibra() {
        return fibra;
    }

    public void setFibra(BigDecimal fibra) {
        this.fibra = fibra;
    }

    public BigDecimal getTamanoPorcion() {
        return tamanoPorcion;
    }

    public void setTamanoPorcion(BigDecimal tamanoPorcion) {
        this.tamanoPorcion = tamanoPorcion;
    }

    public String getUnidadPorcion() {
        return unidadPorcion;
    }

    public void setUnidadPorcion(String unidadPorcion) {
        this.unidadPorcion = unidadPorcion;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}
