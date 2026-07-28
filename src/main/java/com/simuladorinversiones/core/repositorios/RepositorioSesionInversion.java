package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.SesionInversion;
import jakarta.persistence.EntityManager;

import java.util.List;

public class RepositorioSesionInversion extends RepositorioBase<SesionInversion, Long> {

    public RepositorioSesionInversion(EntityManager entityManager) {
        super(entityManager, SesionInversion.class);
    }

    public List<SesionInversion> buscarPorUsuario(Long usuarioId) {
        return entityManager.createQuery(
                        "SELECT s FROM SesionInversion s WHERE s.usuario.id = :usuarioId "
                                + "ORDER BY s.fechaUltimaModificacion DESC",
                        SesionInversion.class)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
    }
}
