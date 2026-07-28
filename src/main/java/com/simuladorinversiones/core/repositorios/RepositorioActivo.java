package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.Activo;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

public class RepositorioActivo extends RepositorioBase<Activo, Long> {

    public RepositorioActivo(EntityManager entityManager) {
        super(entityManager, Activo.class);
    }

    /** Activos que ya existían (según fechaDisponibleDesde) en la fecha simulada dada. */
    public List<Activo> buscarDisponiblesEn(LocalDate fecha) {
        return entityManager.createQuery(
                        "SELECT a FROM Activo a WHERE a.fechaDisponibleDesde <= :fecha ORDER BY a.nombre",
                        Activo.class)
                .setParameter("fecha", fecha)
                .getResultList();
    }
}
