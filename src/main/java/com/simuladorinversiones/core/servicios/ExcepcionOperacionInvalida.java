package com.simuladorinversiones.core.servicios;

/** Se lanza cuando una operación de negocio viola una regla de la simulación (ej. vender más de lo que se posee). */
public class ExcepcionOperacionInvalida extends RuntimeException {

    public ExcepcionOperacionInvalida(String mensaje) {
        super(mensaje);
    }
}
