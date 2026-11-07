package com.nutriguide.modules.alimento;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidad JPA que representa un alimento y su composición nutricional estandarizada
 * según una porción de referencia y unidad definidas (por ejemplo, por cada 100 g).
 */
@Entity
@Table(name = "alimentos")
public class Alimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 100)
    private String categoria;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calorias;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal proteinas;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal carbohidratos;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal grasas;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fibra;

    @Column(name = "porcion_referencia", nullable = false, precision = 8, scale = 2)
    private BigDecimal porcionReferencia;

    @Column(nullable = false, length = 50)
    private String unidad;

    @Column(name = "fuente_informacion", nullable = false, length = 255)
    private String fuenteInformacion;

    public Alimento() {
    }

    public Alimento(
            String nombre,
            String descripcion,
            String categoria,
            String imagenUrl,
            BigDecimal calorias,
            BigDecimal proteinas,
            BigDecimal carbohidratos,
            BigDecimal grasas,
            BigDecimal fibra,
            BigDecimal porcionReferencia,
            String unidad,
            String fuenteInformacion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.imagenUrl = imagenUrl;
        this.calorias = calorias;
        this.proteinas = proteinas;
        this.carbohidratos = carbohidratos;
        this.grasas = grasas;
        this.fibra = fibra;
        this.porcionReferencia = porcionReferencia;
        this.unidad = unidad;
        this.fuenteInformacion = fuenteInformacion;
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
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

    public BigDecimal getPorcionReferencia() {
        return porcionReferencia;
    }

    public void setPorcionReferencia(BigDecimal porcionReferencia) {
        this.porcionReferencia = porcionReferencia;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getFuenteInformacion() {
        return fuenteInformacion;
    }

    public void setFuenteInformacion(String fuenteInformacion) {
        this.fuenteInformacion = fuenteInformacion;
    }
}

