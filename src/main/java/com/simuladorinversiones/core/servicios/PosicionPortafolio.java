package com.simuladorinversiones.core.servicios;

import java.math.BigDecimal;

/**
 * Vista de una posición de cartera enriquecida con el precio vigente, pensada
 * para mostrarse en la UI. No es una entidad persistida, se arma al vuelo a
 * partir de {@code PosicionCartera} + el último precio conocido del activo.
 */
public record PosicionPortafolio(
        Long activoId,
        String nombreActivo,
        String tickerActivo,
        BigDecimal cantidad,
        BigDecimal precioPromedioCompra,
        BigDecimal precioActual,
        BigDecimal valorActual,
        BigDecimal gananciaPerdida) {
}
