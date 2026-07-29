package com.simuladorinversiones.core.datos;

import com.simuladorinversiones.core.entidades.Activo;

/** Resumen de una corrida de {@link ImportadorDatosHistoricos#importar(ActivoDescargado)}. */
public record ResultadoImportacion(Activo activo, int nuevos, int yaExistentes) {
}
