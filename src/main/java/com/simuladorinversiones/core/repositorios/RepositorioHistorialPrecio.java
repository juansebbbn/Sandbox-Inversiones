package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.HistorialPrecio;
import jakarta.persistence.EntityManager;

public class RepositorioHistorialPrecio extends RepositorioBase<HistorialPrecio, Long> {

    public RepositorioHistorialPrecio(EntityManager entityManager) {
        super(entityManager, HistorialPrecio.class);
    }
}
