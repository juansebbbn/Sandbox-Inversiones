package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.SesionInversion;
import jakarta.persistence.EntityManager;

public class RepositorioSesionInversion extends RepositorioBase<SesionInversion, Long> {

    public RepositorioSesionInversion(EntityManager entityManager) {
        super(entityManager, SesionInversion.class);
    }
}
