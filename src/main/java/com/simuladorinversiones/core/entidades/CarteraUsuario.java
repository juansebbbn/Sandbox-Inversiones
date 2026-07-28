package com.simuladorinversiones.core.entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cartera_usuario")
public class CarteraUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "sesion_id", nullable = false, unique = true)
    private SesionInversion sesion;

    public CarteraUsuario() {
    }

    public Long getId() {
        return id;
    }

    public SesionInversion getSesion() {
        return sesion;
    }

    public void setSesion(SesionInversion sesion) {
        this.sesion = sesion;
    }
}
