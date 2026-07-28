package com.simuladorinversiones.core.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "capital_inicial_por_defecto")
    private BigDecimal capitalInicialPorDefecto;

    public Usuario() {
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getCapitalInicialPorDefecto() {
        return capitalInicialPorDefecto;
    }

    public void setCapitalInicialPorDefecto(BigDecimal capitalInicialPorDefecto) {
        this.capitalInicialPorDefecto = capitalInicialPorDefecto;
    }
}
