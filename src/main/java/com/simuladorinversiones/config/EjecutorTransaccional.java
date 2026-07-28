package com.simuladorinversiones.config;

import com.simuladorinversiones.core.servicios.FabricaServicios;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.function.Function;

/**
 * Punto único desde el que la UI ejecuta una operación de negocio: abre un
 * EntityManager y una transacción, arma los servicios sobre ese EntityManager
 * y hace commit o rollback según corresponda. Cada acción del usuario en la UI
 * (comprar, avanzar tiempo, etc.) pasa por acá.
 */
public final class EjecutorTransaccional {

    private EjecutorTransaccional() {
    }

    public static <T> T ejecutar(Function<FabricaServicios, T> operacion) {
        EntityManagerFactory fabricaEntityManager = ConfiguracionBaseDatos.obtenerFabrica();
        EntityManager entityManager = fabricaEntityManager.createEntityManager();
        EntityTransaction transaccion = entityManager.getTransaction();
        try {
            transaccion.begin();
            T resultado = operacion.apply(new FabricaServicios(entityManager));
            transaccion.commit();
            return resultado;
        } catch (RuntimeException excepcion) {
            if (transaccion.isActive()) {
                transaccion.rollback();
            }
            throw excepcion;
        } finally {
            entityManager.close();
        }
    }
}
