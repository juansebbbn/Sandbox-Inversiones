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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/** Representa la tenencia de un activo dentro de una {@link CarteraUsuario}. */
@Entity
@Table(name = "posicion_cartera",
        uniqueConstraints = @UniqueConstraint(name = "uq_posicion_cartera_activo",
                columnNames = {"cartera_id", "activo_id"}))
public class PosicionCartera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartera_id", nullable = false)
    private CarteraUsuario cartera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activo_id", nullable = false)
    private Activo activo;

    @Column(name = "cantidad", nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_promedio_compra", nullable = false)
    private BigDecimal precioPromedioCompra;

    public PosicionCartera() {
    }

    public Long getId() {
        return id;
    }

    public CarteraUsuario getCartera() {
        return cartera;
    }

    public void setCartera(CarteraUsuario cartera) {
        this.cartera = cartera;
    }

    public Activo getActivo() {
        return activo;
    }

    public void setActivo(Activo activo) {
        this.activo = activo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioPromedioCompra() {
        return precioPromedioCompra;
    }

    public void setPrecioPromedioCompra(BigDecimal precioPromedioCompra) {
        this.precioPromedioCompra = precioPromedioCompra;
    }
}
