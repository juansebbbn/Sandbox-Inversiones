package com.simuladorinversiones.core.datos;

import com.simuladorinversiones.core.enums.TipoActivo;

import java.time.LocalDate;
import java.util.List;

/** Resultado crudo de una fuente de datos histórica, listo para volcar a la base con {@link ImportadorDatosHistoricos}. */
public record ActivoDescargado(String ticker, String nombre, TipoActivo tipo,
                                LocalDate fechaDisponibleDesde, List<PrecioDiario> precios) {
}
