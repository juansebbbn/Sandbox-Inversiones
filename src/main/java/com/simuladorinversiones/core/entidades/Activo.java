package com.simuladorinversiones.core.entidades;

import com.simuladorinversiones.core.enums.TipoActivo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "activo")
public class Activo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoActivo tipo;

    @Column(name = "fecha_disponible_desde", nullable = false)
    private LocalDate fechaDisponibleDesde;

    @Column(name = "ticker", nullable = false, length = 20, unique = true)
    private String ticker;

    public Activo() {
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoActivo getTipo() {
        return tipo;
    }

    public void setTipo(TipoActivo tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFechaDisponibleDesde() {
        return fechaDisponibleDesde;
    }

    public void setFechaDisponibleDesde(LocalDate fechaDisponibleDesde) {
        this.fechaDisponibleDesde = fechaDisponibleDesde;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }
}
