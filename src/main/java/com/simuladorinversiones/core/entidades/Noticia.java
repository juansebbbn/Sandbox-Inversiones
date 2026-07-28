package com.simuladorinversiones.core.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "noticia")
public class Noticia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "titulo", nullable = false, length = 500)
    private String titulo;

    @Column(name = "resumen", columnDefinition = "TEXT")
    private String resumen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activo_relacionado_id")
    private Activo activoRelacionado;

    @Column(name = "fuente", length = 200)
    private String fuente;

    public Noticia() {
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public Activo getActivoRelacionado() {
        return activoRelacionado;
    }

    public void setActivoRelacionado(Activo activoRelacionado) {
        this.activoRelacionado = activoRelacionado;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }
}
