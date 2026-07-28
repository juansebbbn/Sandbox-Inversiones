package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.Activo;
import jakarta.persistence.EntityManager;

public class RepositorioActivo extends RepositorioBase<Activo, Long> {

    public RepositorioActivo(EntityManager entityManager) {
        super(entityManager, Activo.class);
    }
}
