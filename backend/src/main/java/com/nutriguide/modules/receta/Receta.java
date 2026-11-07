package com.nutriguide.modules.receta;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa una receta educativa con ingredientes, preparación
 * e información nutricional estandarizada por porción.
 */
@Entity
@Table(name = "recetas")
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ingredientes;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String preparacion;

    @Column(name = "tiempo_preparacion", nullable = false)
    private Integer tiempoPreparacion;

    @Column(name = "informacion_nutricional", nullable = false, columnDefinition = "TEXT")
    private String informacionNutricional;

    @Column(nullable = false, length = 100)
    private String categoria;

    public Receta() {
    }

    public Receta(
            String nombre,
            String descripcion,
            String imagenUrl,
            String ingredientes,
            String preparacion,
            Integer tiempoPreparacion,
            String informacionNutricional,
            String categoria) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imagenUrl = imagenUrl;
        this.ingredientes = ingredientes;
        this.preparacion = preparacion;
        this.tiempoPreparacion = tiempoPreparacion;
        this.informacionNutricional = informacionNutricional;
        this.categoria = categoria;
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

    public String getPreparacion() {
        return preparacion;
    }

    public void setPreparacion(String preparacion) {
        this.preparacion = preparacion;
    }

    public Integer getTiempoPreparacion() {
        return tiempoPreparacion;
    }

    public void setTiempoPreparacion(Integer tiempoPreparacion) {
        this.tiempoPreparacion = tiempoPreparacion;
    }

    public String getInformacionNutricional() {
        return informacionNutricional;
    }

    public void setInformacionNutricional(String informacionNutricional) {
        this.informacionNutricional = informacionNutricional;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}

