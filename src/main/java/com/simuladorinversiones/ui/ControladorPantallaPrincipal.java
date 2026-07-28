package com.simuladorinversiones.ui;

import com.simuladorinversiones.config.ConfiguracionBaseDatos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controlador de la pantalla mínima de este módulo: solo confirma que la app
 * puede levantar el EntityManagerFactory y hablar con PostgreSQL. La UI real
 * tipo dashboard se construye en un módulo posterior.
 */
public class ControladorPantallaPrincipal {

    @FXML
    private Label etiquetaEstadoConexion;

    @FXML
    private void initialize() {
        try {
            EntityManagerFactory fabrica = ConfiguracionBaseDatos.obtenerFabrica();
            try (EntityManager entityManager = fabrica.createEntityManager()) {
                long cantidadUsuarios = entityManager
                        .createQuery("SELECT COUNT(u) FROM Usuario u", Long.class)
                        .getSingleResult();
                etiquetaEstadoConexion.setText(
                        "Conexión a la base de datos OK (usuarios registrados: " + cantidadUsuarios + ")");
            }
        } catch (Exception excepcion) {
            etiquetaEstadoConexion.setText("No se pudo conectar a la base de datos: " + excepcion.getMessage());
        }
    }
}
