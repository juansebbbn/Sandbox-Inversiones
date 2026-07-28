package com.simuladorinversiones.ui;

import com.simuladorinversiones.config.ConfiguracionBaseDatos;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class AplicacionPrincipal extends Application {

    @Override
    public void start(Stage escenarioPrincipal) throws IOException {
        FXMLLoader cargador = new FXMLLoader(getClass().getResource("/ui/pantalla-principal.fxml"));
        Parent raiz = cargador.load();

        escenarioPrincipal.setTitle("Simulador de Inversiones");
        escenarioPrincipal.setScene(new Scene(raiz, 480, 320));
        escenarioPrincipal.show();
    }

    @Override
    public void stop() {
        ConfiguracionBaseDatos.cerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
