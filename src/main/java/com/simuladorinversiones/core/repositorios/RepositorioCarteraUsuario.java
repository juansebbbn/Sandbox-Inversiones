package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.CarteraUsuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.Optional;

public class RepositorioCarteraUsuario extends RepositorioBase<CarteraUsuario, Long> {

    public RepositorioCarteraUsuario(EntityManager entityManager) {
        super(entityManager, CarteraUsuario.class);
    }

    public Optional<CarteraUsuario> buscarPorSesion(Long sesionId) {
        try {
            CarteraUsuario cartera = entityManager.createQuery(
                            "SELECT c FROM CarteraUsuario c WHERE c.sesion.id = :sesionId", CarteraUsuario.class)
                    .setParameter("sesionId", sesionId)
                    .getSingleResult();
            return Optional.of(cartera);
        } catch (NoResultException excepcion) {
            return Optional.empty();
        }
    }
}
