package com.simuladorinversiones.core.servicios;

import com.simuladorinversiones.core.entidades.SesionInversion;
import com.simuladorinversiones.core.enums.EstadoSesion;
import com.simuladorinversiones.core.enums.UnidadTiempo;
import com.simuladorinversiones.core.excepciones.ExcepcionOperacionInvalida;
import com.simuladorinversiones.core.repositorios.RepositorioSesionInversion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Avance y retroceso de la fecha simulada de una sesión, con el interés simple
 * sobre saldo negativo que pide la spec: se recalcula cada vez que la simulación
 * avanza, prorrateado según la cantidad de días avanzados. Retroceder el tiempo
 * no revierte ni vuelve a calcular intereses ya aplicados.
 */
public class ServicioAvanceTiempo {

    /** Tasa fija de partida, ajustable más adelante si hace falta. */
    public static final BigDecimal TASA_INTERES_MENSUAL = new BigDecimal("0.02");

    private static final BigDecimal DIAS_POR_MES = new BigDecimal("30.44");

    private final RepositorioSesionInversion repositorioSesion;

    public ServicioAvanceTiempo(RepositorioSesionInversion repositorioSesion) {
        this.repositorioSesion = repositorioSesion;
    }

    public SesionInversion avanzar(Long sesionId, UnidadTiempo unidad, int cantidad) {
        SesionInversion sesion = obtenerSesionActiva(sesionId);
        validarCantidadPositiva(cantidad);

        LocalDate fechaAnterior = sesion.getFechaActual();
        LocalDate fechaCalculada = calcularFecha(fechaAnterior, unidad, cantidad, true);
        LocalDate fechaNueva = fechaCalculada.isAfter(ServicioSesionInversion.FECHA_LIMITE)
                ? ServicioSesionInversion.FECHA_LIMITE
                : fechaCalculada;

        sesion.setFechaActual(fechaNueva);
        aplicarInteresSiCorresponde(sesion, fechaAnterior, fechaNueva);

        if (!fechaNueva.isBefore(ServicioSesionInversion.FECHA_LIMITE)) {
            sesion.setEstado(EstadoSesion.FINALIZADA);
        }

        sesion.setFechaUltimaModificacion(LocalDateTime.now());
        return repositorioSesion.actualizar(sesion);
    }

    public SesionInversion retroceder(Long sesionId, UnidadTiempo unidad, int cantidad) {
        SesionInversion sesion = obtenerSesionActiva(sesionId);
        validarCantidadPositiva(cantidad);

        LocalDate fechaCalculada = calcularFecha(sesion.getFechaActual(), unidad, cantidad, false);
        LocalDate fechaNueva = fechaCalculada.isBefore(ServicioSesionInversion.FECHA_MINIMA)
                ? ServicioSesionInversion.FECHA_MINIMA
                : fechaCalculada;

        sesion.setFechaActual(fechaNueva);
        sesion.setFechaUltimaModificacion(LocalDateTime.now());
        return repositorioSesion.actualizar(sesion);
    }

    private void aplicarInteresSiCorresponde(SesionInversion sesion, LocalDate fechaAnterior, LocalDate fechaNueva) {
        if (sesion.getSaldoEfectivoActual().compareTo(BigDecimal.ZERO) >= 0) {
            return;
        }
        long diasTranscurridos = ChronoUnit.DAYS.between(fechaAnterior, fechaNueva);
        if (diasTranscurridos <= 0) {
            return;
        }
        BigDecimal fraccionDeMes = BigDecimal.valueOf(diasTranscurridos).divide(DIAS_POR_MES, 6, RoundingMode.HALF_UP);
        BigDecimal interes = sesion.getSaldoEfectivoActual().multiply(TASA_INTERES_MENSUAL).multiply(fraccionDeMes);
        sesion.setSaldoEfectivoActual(
                sesion.getSaldoEfectivoActual().add(interes).setScale(2, RoundingMode.HALF_UP));
    }

    private LocalDate calcularFecha(LocalDate base, UnidadTiempo unidad, int cantidad, boolean avanzando) {
        int signo = avanzando ? 1 : -1;
        return switch (unidad) {
            case DIA -> base.plusDays((long) signo * cantidad);
            case SEMANA -> base.plusWeeks((long) signo * cantidad);
            case MES -> base.plusMonths((long) signo * cantidad);
            case ANIO -> base.plusYears((long) signo * cantidad);
        };
    }

    private SesionInversion obtenerSesionActiva(Long sesionId) {
        SesionInversion sesion = repositorioSesion.buscarPorId(sesionId)
                .orElseThrow(() -> new ExcepcionOperacionInvalida("No existe la sesión " + sesionId));
        if (sesion.getEstado() != EstadoSesion.ACTIVA) {
            throw new ExcepcionOperacionInvalida("La sesión " + sesionId + " no está activa");
        }
        return sesion;
    }

    private void validarCantidadPositiva(int cantidad) {
        if (cantidad <= 0) {
            throw new ExcepcionOperacionInvalida("La cantidad de tiempo a mover debe ser mayor a cero");
        }
    }
}
