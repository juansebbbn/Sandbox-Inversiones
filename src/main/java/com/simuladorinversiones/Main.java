package com.simuladorinversiones;

import com.simuladorinversiones.ui.AplicacionPrincipal;

/**
 * Punto de entrada explícito, separado de {@link AplicacionPrincipal}
 * para evitar problemas al ejecutar el jar sin el module-path de JavaFX.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        AplicacionPrincipal.main(args);
    }
}
