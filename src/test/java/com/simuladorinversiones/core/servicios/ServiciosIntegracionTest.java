package com.simuladorinversiones.core.servicios;

import com.simuladorinversiones.config.ConfiguracionBaseDatos;
import com.simuladorinversiones.core.entidades.Activo;
import com.simuladorinversiones.core.entidades.HistorialPrecio;
import com.simuladorinversiones.core.entidades.SesionInversion;
import com.simuladorinversiones.core.entidades.TipoActivo;
import com.simuladorinversiones.core.entidades.Transaccion;
import com.simuladorinversiones.core.entidades.Usuario;
import com.simuladorinversiones.core.repositorios.RepositorioActivo;
import com.simuladorinversiones.core.repositorios.RepositorioCarteraUsuario;
import com.simuladorinversiones.core.repositorios.RepositorioHistorialPrecio;
import com.simuladorinversiones.core.repositorios.RepositorioPosicionCartera;
import com.simuladorinversiones.core.repositorios.RepositorioSesionInversion;
import com.simuladorinversiones.core.repositorios.RepositorioTransaccion;
import com.simuladorinversiones.core.repositorios.RepositorioUsuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de integración de extremo a extremo contra PostgreSQL real: crea una
 * sesión, compra un activo (quedando en descubierto), avanza el tiempo un mes
 * (para probar el interés sobre saldo negativo) y vende. Requiere
 * `docker compose up -d` y `mvn flyway:migrate` antes de correrlo.
 */
class ServiciosIntegracionTest {

    private EntityManager entityManager;
    private Usuario usuario;
    private Activo activo;

    @BeforeEach
    void prepararDatosBase() {
        EntityManagerFactory fabrica = ConfiguracionBaseDatos.obtenerFabrica();
        entityManager = fabrica.createEntityManager();

        ejecutarEnTransaccion(() -> {
            usuario = new Usuario();
            entityManager.persist(usuario);

            activo = new Activo();
            activo.setNombre("Activo de prueba de integración");
            activo.setTipo(TipoActivo.ACCION);
            activo.setFechaDisponibleDesde(LocalDate.of(1950, 1, 1));
            activo.setTicker("INT-" + (System.nanoTime() % 1_000_000));
            entityManager.persist(activo);

            HistorialPrecio precio = new HistorialPrecio();
            precio.setActivo(activo);
            precio.setFecha(LocalDate.of(1950, 1, 1));
            precio.setValor(new BigDecimal("100.00"));
            entityManager.persist(precio);
        });
    }

    @AfterEach
    void limpiarDatos() {
        ejecutarEnTransaccion(() -> {
            entityManager.createQuery("DELETE FROM Transaccion t WHERE t.activo.id = :id")
                    .setParameter("id", activo.getId()).executeUpdate();
            entityManager.createQuery("DELETE FROM PosicionCartera p WHERE p.activo.id = :id")
                    .setParameter("id", activo.getId()).executeUpdate();
            entityManager.createQuery("DELETE FROM CarteraUsuario c WHERE c.sesion.usuario.id = :id")
                    .setParameter("id", usuario.getId()).executeUpdate();
            entityManager.createQuery("DELETE FROM SesionInversion s WHERE s.usuario.id = :id")
                    .setParameter("id", usuario.getId()).executeUpdate();
            entityManager.createQuery("DELETE FROM HistorialPrecio h WHERE h.activo.id = :id")
                    .setParameter("id", activo.getId()).executeUpdate();
            entityManager.createQuery("DELETE FROM Activo a WHERE a.id = :id")
                    .setParameter("id", activo.getId()).executeUpdate();
            entityManager.createQuery("DELETE FROM Usuario u WHERE u.id = :id")
                    .setParameter("id", usuario.getId()).executeUpdate();
        });
        entityManager.close();
    }

    @Test
    void flujoCompletoDeUnaSesionDeInversion() {
        ServicioSesionInversion servicioSesion = new ServicioSesionInversion(
                new RepositorioSesionInversion(entityManager),
                new RepositorioUsuario(entityManager),
                new RepositorioCarteraUsuario(entityManager));
        ServicioTransacciones servicioTransacciones = new ServicioTransacciones(
                new RepositorioSesionInversion(entityManager),
                new RepositorioActivo(entityManager),
                new RepositorioCarteraUsuario(entityManager),
                new RepositorioPosicionCartera(entityManager),
                new RepositorioTransaccion(entityManager),
                new RepositorioHistorialPrecio(entityManager));
        ServicioAvanceTiempo servicioAvance = new ServicioAvanceTiempo(new RepositorioSesionInversion(entityManager));

        Long[] sesionId = new Long[1];
        ejecutarEnTransaccion(() -> {
            SesionInversion sesion = servicioSesion.crearSesion(
                    usuario.getId(), LocalDate.of(1950, 1, 1), new BigDecimal("1000.00"));
            sesionId[0] = sesion.getId();
        });

        // 15 unidades a 100 = 1500: deja el saldo (1000 - 1500) en -500.
        ejecutarEnTransaccion(() -> {
            Transaccion compra = servicioTransacciones.comprar(sesionId[0], activo.getId(), new BigDecimal("15"));
            assertEquals(0, new BigDecimal("15").compareTo(compra.getCantidad()));
        });

        ejecutarEnTransaccion(() -> servicioAvance.avanzar(sesionId[0], UnidadTiempo.MES, 1));

        SesionInversion[] sesionTrasAvance = new SesionInversion[1];
        ejecutarEnTransaccion(() -> sesionTrasAvance[0] = entityManager.find(SesionInversion.class, sesionId[0]));
        assertTrue(sesionTrasAvance[0].getSaldoEfectivoActual().compareTo(new BigDecimal("-500.00")) < 0,
                "El saldo negativo debería haber acumulado interés al avanzar un mes");

        ejecutarEnTransaccion(() -> servicioTransacciones.vender(sesionId[0], activo.getId(), new BigDecimal("15")));

        entityManager.clear();
        SesionInversion[] sesionFinal = new SesionInversion[1];
        ejecutarEnTransaccion(() -> sesionFinal[0] = entityManager.find(SesionInversion.class, sesionId[0]));
        assertTrue(sesionFinal[0].getSaldoEfectivoActual().compareTo(BigDecimal.ZERO) > 0,
                "Tras vender todo a un precio estable, el saldo debería volver a ser positivo");
    }

    private void ejecutarEnTransaccion(Runnable operacion) {
        EntityTransaction transaccion = entityManager.getTransaction();
        transaccion.begin();
        operacion.run();
        transaccion.commit();
    }
}
