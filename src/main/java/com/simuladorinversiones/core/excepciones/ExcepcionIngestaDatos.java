package com.simuladorinversiones.core.excepciones;

/** Falla al descargar o interpretar datos de una fuente histórica externa (red, formato inesperado, ticker inexistente). */
public class ExcepcionIngestaDatos extends RuntimeException {

    public ExcepcionIngestaDatos(String mensaje) {
        super(mensaje);
    }

    public ExcepcionIngestaDatos(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
