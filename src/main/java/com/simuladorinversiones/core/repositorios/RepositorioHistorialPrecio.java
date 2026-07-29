package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.core.entidades.HistorialPrecio;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class RepositorioHistorialPrecio extends RepositorioBase<HistorialPrecio, Long> {

    public RepositorioHistorialPrecio(EntityManager entityManager) {
        super(entityManager, HistorialPrecio.class);
    }

    /**
     * Último precio conocido de un activo en o antes de la fecha dada. Como la
     * frecuencia de muestreo de los datos históricos varía, se usa el precio
     * vigente más reciente en vez de exigir un dato exacto para esa fecha.
     */
    public Optional<HistorialPrecio> buscarUltimoPrecioHasta(Long activoId, LocalDate fecha) {
        List<HistorialPrecio> resultado = entityManager.createQuery(
                        "SELECT h FROM HistorialPrecio h WHERE h.activo.id = :activoId AND h.fecha <= :fecha "
                                + "ORDER BY h.fecha DESC",
                        HistorialPrecio.class)
                .setParameter("activoId", activoId)
                .setParameter("fecha", fecha)
                .setMaxResults(1)
                .getResultList();
        return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    }

    /** Usado por la ingesta de datos históricos para saltear puntos que ya están cargados para ese activo. */
    public List<LocalDate> buscarFechasExistentes(Long activoId) {
        return entityManager.createQuery(
                        "SELECT h.fecha FROM HistorialPrecio h WHERE h.activo.id = :activoId", LocalDate.class)
                .setParameter("activoId", activoId)
                .getResultList();
    }
}
