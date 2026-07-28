package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.Noticia;
import jakarta.persistence.EntityManager;

public class RepositorioNoticia extends RepositorioBase<Noticia, Long> {

    public RepositorioNoticia(EntityManager entityManager) {
        super(entityManager, Noticia.class);
    }
}
