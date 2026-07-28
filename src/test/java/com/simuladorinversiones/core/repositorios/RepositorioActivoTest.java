package com.simuladorinversiones.core.repositorios;

import com.simuladorinversiones.config.ConfiguracionBaseDatos;
import com.simuladorinversiones.core.entidades.Activo;
import com.simuladorinversiones.core.entidades.TipoActivo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de humo de extremo a extremo: confirma que la app puede guardar y leer
 * un Activo real contra la instancia de PostgreSQL levantada con Docker.
 * Requiere `docker compose up -d` y `mvn flyway:migrate` antes de correrlo.
 */
class RepositorioActivoTest {

    @Test
    void guardaYLeeUnActivo() {
        EntityManagerFactory fabrica = ConfiguracionBaseDatos.obtenerFabrica();
        EntityManager entityManager = fabrica.createEntityManager();
        EntityTransaction transaccion = entityManager.getTransaction();
        RepositorioActivo repositorio = new RepositorioActivo(entityManager);

        Activo activo = new Activo();
        activo.setNombre("Dow Jones Industrial Average");
        activo.setTipo(TipoActivo.INDICE);
        activo.setFechaDisponibleDesde(LocalDate.of(1900, 1, 1));
        activo.setTicker("TEST-" + (System.nanoTime() % 1_000_000));

        try {
            transaccion.begin();
            repositorio.guardar(activo);
            transaccion.commit();

            Optional<Activo> encontrado = repositorio.buscarPorId(activo.getId());

            assertTrue(encontrado.isPresent());
            assertEquals(activo.getTicker(), encontrado.get().getTicker());
            assertEquals(TipoActivo.INDICE, encontrado.get().getTipo());
        } finally {
            transaccion.begin();
            repositorio.buscarPorId(activo.getId()).ifPresent(repositorio::eliminar);
            transaccion.commit();
            entityManager.close();
        }
    }
}
