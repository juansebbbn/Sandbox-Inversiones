package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.Transaccion;
import jakarta.persistence.EntityManager;

public class RepositorioTransaccion extends RepositorioBase<Transaccion, Long> {

    public RepositorioTransaccion(EntityManager entityManager) {
        super(entityManager, Transaccion.class);
    }
}
