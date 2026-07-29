package com.simuladorinversiones.core.datos;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un punto de la serie histórica de precios: fecha y valor de cierre. */
public record PrecioDiario(LocalDate fecha, BigDecimal valor) {
}
