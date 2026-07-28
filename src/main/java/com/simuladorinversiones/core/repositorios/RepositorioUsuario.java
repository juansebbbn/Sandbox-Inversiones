package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.Usuario;
import jakarta.persistence.EntityManager;

public class RepositorioUsuario extends RepositorioBase<Usuario, Long> {

    public RepositorioUsuario(EntityManager entityManager) {
        super(entityManager, Usuario.class);
    }
}
