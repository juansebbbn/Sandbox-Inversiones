package com.simuladorinversiones.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/** Cambia la pantalla mostrada en el Stage principal, cargando el FXML correspondiente. */
public class NavegadorPantallas {

    private final Stage escenario;

    public NavegadorPantallas(Stage escenario) {
        this.escenario = escenario;
    }

    public <T> T mostrar(String rutaFxml) {
        try {
            FXMLLoader cargador = new FXMLLoader(getClass().getResource(rutaFxml));
            Parent raiz = cargador.load();

            Scene escena = escenario.getScene();
            if (escena == null) {
                escena = new Scene(raiz, 1150, 740);
                escena.getStylesheets().add(
                        Objects.requireNonNull(getClass().getResource("/ui/estilos.css")).toExternalForm());
                escenario.setScene(escena);
            } else {
                escena.setRoot(raiz);
            }
            return cargador.getController();
        } catch (IOException excepcion) {
            throw new UncheckedIOException(excepcion);
        }
    }
}
