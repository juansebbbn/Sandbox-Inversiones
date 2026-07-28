package com.simuladorinversiones.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Punto único de acceso al {@link EntityManagerFactory} de la aplicación.
 * La URL/usuario/password de conexión se pueden sobrescribir con las variables
 * de entorno DB_URL, DB_USUARIO y DB_PASSWORD; si no están presentes se usan
 * los valores de desarrollo definidos en persistence.xml (los mismos del
 * docker-compose.yml de este repo).
 */
public final class ConfiguracionBaseDatos {

    private static final String UNIDAD_PERSISTENCIA = "simuladorInversiones";

    private static EntityManagerFactory fabrica;

    private ConfiguracionBaseDatos() {
    }

    public static synchronized EntityManagerFactory obtenerFabrica() {
        if (fabrica == null) {
            fabrica = Persistence.createEntityManagerFactory(UNIDAD_PERSISTENCIA, propiedadesDesdeEntorno());
        }
        return fabrica;
    }

    public static synchronized void cerrar() {
        if (fabrica != null && fabrica.isOpen()) {
            fabrica.close();
            fabrica = null;
        }
    }

    private static Map<String, Object> propiedadesDesdeEntorno() {
        Map<String, Object> propiedades = new HashMap<>();
        agregarSiPresente(propiedades, "jakarta.persistence.jdbc.url", System.getenv("DB_URL"));
        agregarSiPresente(propiedades, "jakarta.persistence.jdbc.user", System.getenv("DB_USUARIO"));
        agregarSiPresente(propiedades, "jakarta.persistence.jdbc.password", System.getenv("DB_PASSWORD"));
        return propiedades;
    }

    private static void agregarSiPresente(Map<String, Object> propiedades, String clave, String valor) {
        if (valor != null && !valor.isBlank()) {
            propiedades.put(clave, valor);
        }
    }
}
