package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.CarteraUsuario;
import jakarta.persistence.EntityManager;

public class RepositorioCarteraUsuario extends RepositorioBase<CarteraUsuario, Long> {

    public RepositorioCarteraUsuario(EntityManager entityManager) {
        super(entityManager, CarteraUsuario.class);
    }
}
