package com.simuladorinversiones.ui;

import com.simuladorinversiones.config.ConfiguracionBaseDatos;
import com.simuladorinversiones.config.EjecutorTransaccional;
import javafx.application.Application;
import javafx.stage.Stage;

public class AplicacionPrincipal extends Application {

    @Override
    public void start(Stage escenarioPrincipal) {
        escenarioPrincipal.setTitle("Simulador de Inversiones");

        NavegadorPantallas navegador = new NavegadorPantallas(escenarioPrincipal);

        // App de un solo usuario sin autenticación: se reutiliza o se crea el único Usuario al arrancar.
        Long usuarioId = EjecutorTransaccional.ejecutar(
                fabrica -> fabrica.obtenerOCrearUsuarioUnico().getId());

        ControladorPantallaInicio controlador = navegador.mostrar("/ui/pantalla-inicio.fxml");
        controlador.inicializar(navegador, usuarioId);

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
