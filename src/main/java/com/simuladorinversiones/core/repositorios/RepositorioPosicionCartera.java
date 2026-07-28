package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.PosicionCartera;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.List;
import java.util.Optional;

public class RepositorioPosicionCartera extends RepositorioBase<PosicionCartera, Long> {

    public RepositorioPosicionCartera(EntityManager entityManager) {
        super(entityManager, PosicionCartera.class);
    }

    public Optional<PosicionCartera> buscarPorCarteraYActivo(Long carteraId, Long activoId) {
        try {
            PosicionCartera posicion = entityManager.createQuery(
                            "SELECT p FROM PosicionCartera p "
                                    + "WHERE p.cartera.id = :carteraId AND p.activo.id = :activoId",
                            PosicionCartera.class)
                    .setParameter("carteraId", carteraId)
                    .setParameter("activoId", activoId)
                    .getSingleResult();
            return Optional.of(posicion);
        } catch (NoResultException excepcion) {
            return Optional.empty();
        }
    }

    public List<PosicionCartera> buscarPorCartera(Long carteraId) {
        return entityManager.createQuery(
                        "SELECT p FROM PosicionCartera p WHERE p.cartera.id = :carteraId", PosicionCartera.class)
                .setParameter("carteraId", carteraId)
                .getResultList();
    }
}
