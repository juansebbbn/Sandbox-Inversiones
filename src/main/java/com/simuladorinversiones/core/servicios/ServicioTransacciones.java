package com.simuladorinversiones.core.servicios;

import com.simuladorinversiones.core.entidades.Activo;
import com.simuladorinversiones.core.entidades.CarteraUsuario;
import com.simuladorinversiones.core.entidades.EstadoSesion;
import com.simuladorinversiones.core.entidades.HistorialPrecio;
import com.simuladorinversiones.core.entidades.PosicionCartera;
import com.simuladorinversiones.core.entidades.SesionInversion;
import com.simuladorinversiones.core.entidades.Transaccion;
import com.simuladorinversiones.core.entidades.TipoTransaccion;
import com.simuladorinversiones.core.repositorios.RepositorioActivo;
import com.simuladorinversiones.core.repositorios.RepositorioCarteraUsuario;
import com.simuladorinversiones.core.repositorios.RepositorioHistorialPrecio;
import com.simuladorinversiones.core.repositorios.RepositorioPosicionCartera;
import com.simuladorinversiones.core.repositorios.RepositorioSesionInversion;
import com.simuladorinversiones.core.repositorios.RepositorioTransaccion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Compra y venta de activos. El saldo de efectivo puede quedar negativo (se permite
 * quedar en descubierto), pero no se puede vender más cantidad de un activo de la
 * que la sesión efectivamente posee (no hay venta en corto).
 */
public class ServicioTransacciones {

    private final RepositorioSesionInversion repositorioSesion;
    private final RepositorioActivo repositorioActivo;
    private final RepositorioCarteraUsuario repositorioCartera;
    private final RepositorioPosicionCartera repositorioPosicion;
    private final RepositorioTransaccion repositorioTransaccion;
    private final RepositorioHistorialPrecio repositorioHistorialPrecio;

    public ServicioTransacciones(RepositorioSesionInversion repositorioSesion,
                                  RepositorioActivo repositorioActivo,
                                  RepositorioCarteraUsuario repositorioCartera,
                                  RepositorioPosicionCartera repositorioPosicion,
                                  RepositorioTransaccion repositorioTransaccion,
                                  RepositorioHistorialPrecio repositorioHistorialPrecio) {
        this.repositorioSesion = repositorioSesion;
        this.repositorioActivo = repositorioActivo;
        this.repositorioCartera = repositorioCartera;
        this.repositorioPosicion = repositorioPosicion;
        this.repositorioTransaccion = repositorioTransaccion;
        this.repositorioHistorialPrecio = repositorioHistorialPrecio;
    }

    public Transaccion comprar(Long sesionId, Long activoId, BigDecimal cantidad) {
        SesionInversion sesion = obtenerSesionActiva(sesionId);
        Activo activo = obtenerActivo(activoId);

        if (activo.getFechaDisponibleDesde().isAfter(sesion.getFechaActual())) {
            throw new ExcepcionOperacionInvalida(
                    "El activo " + activo.getNombre() + " todavía no está disponible en esta fecha");
        }

        BigDecimal precioUnitario = obtenerPrecioVigente(activoId, sesion);
        validarCantidadPositiva(cantidad);
        BigDecimal costoTotal = precioUnitario.multiply(cantidad);

        sesion.setSaldoEfectivoActual(
                sesion.getSaldoEfectivoActual().subtract(costoTotal).setScale(2, RoundingMode.HALF_UP));
        sesion.setFechaUltimaModificacion(LocalDateTime.now());
        repositorioSesion.actualizar(sesion);

        CarteraUsuario cartera = obtenerCartera(sesionId);
        repositorioPosicion.buscarPorCarteraYActivo(cartera.getId(), activoId).ifPresentOrElse(
                posicion -> aumentarPosicion(posicion, cantidad, precioUnitario),
                () -> crearPosicion(cartera, activo, cantidad, precioUnitario));

        return registrarTransaccion(sesion, activo, TipoTransaccion.COMPRA, cantidad, precioUnitario);
    }

    public Transaccion vender(Long sesionId, Long activoId, BigDecimal cantidad) {
        SesionInversion sesion = obtenerSesionActiva(sesionId);
        Activo activo = obtenerActivo(activoId);
        BigDecimal precioUnitario = obtenerPrecioVigente(activoId, sesion);
        validarCantidadPositiva(cantidad);

        CarteraUsuario cartera = obtenerCartera(sesionId);
        PosicionCartera posicion = repositorioPosicion.buscarPorCarteraYActivo(cartera.getId(), activoId)
                .orElseThrow(() -> new ExcepcionOperacionInvalida(
                        "No se puede vender " + activo.getNombre() + ": no hay posición en cartera"));

        if (posicion.getCantidad().compareTo(cantidad) < 0) {
            throw new ExcepcionOperacionInvalida(
                    "No se puede vender más cantidad de " + activo.getNombre() + " de la que se posee");
        }

        BigDecimal montoVenta = precioUnitario.multiply(cantidad);
        sesion.setSaldoEfectivoActual(
                sesion.getSaldoEfectivoActual().add(montoVenta).setScale(2, RoundingMode.HALF_UP));
        sesion.setFechaUltimaModificacion(LocalDateTime.now());
        repositorioSesion.actualizar(sesion);

        BigDecimal cantidadRestante = posicion.getCantidad().subtract(cantidad);
        if (cantidadRestante.compareTo(BigDecimal.ZERO) == 0) {
            repositorioPosicion.eliminar(posicion);
        } else {
            posicion.setCantidad(cantidadRestante);
            repositorioPosicion.actualizar(posicion);
        }

        return registrarTransaccion(sesion, activo, TipoTransaccion.VENTA, cantidad, precioUnitario);
    }

    private void aumentarPosicion(PosicionCartera posicion, BigDecimal cantidad, BigDecimal precioUnitario) {
        BigDecimal cantidadVieja = posicion.getCantidad();
        BigDecimal costoViejo = posicion.getPrecioPromedioCompra().multiply(cantidadVieja);
        BigDecimal costoNuevo = precioUnitario.multiply(cantidad);
        BigDecimal cantidadNueva = cantidadVieja.add(cantidad);

        posicion.setCantidad(cantidadNueva);
        posicion.setPrecioPromedioCompra(costoViejo.add(costoNuevo).divide(cantidadNueva, 4, RoundingMode.HALF_UP));
        repositorioPosicion.actualizar(posicion);
    }

    private void crearPosicion(CarteraUsuario cartera, Activo activo, BigDecimal cantidad, BigDecimal precioUnitario) {
        PosicionCartera posicion = new PosicionCartera();
        posicion.setCartera(cartera);
        posicion.setActivo(activo);
        posicion.setCantidad(cantidad);
        posicion.setPrecioPromedioCompra(precioUnitario);
        repositorioPosicion.guardar(posicion);
    }

    private Transaccion registrarTransaccion(SesionInversion sesion, Activo activo, TipoTransaccion tipo,
                                              BigDecimal cantidad, BigDecimal precioUnitario) {
        Transaccion transaccion = new Transaccion();
        transaccion.setSesion(sesion);
        transaccion.setActivo(activo);
        transaccion.setTipo(tipo);
        transaccion.setCantidad(cantidad);
        transaccion.setPrecioUnitario(precioUnitario);
        transaccion.setFecha(sesion.getFechaActual());
        return repositorioTransaccion.guardar(transaccion);
    }

    private BigDecimal obtenerPrecioVigente(Long activoId, SesionInversion sesion) {
        HistorialPrecio precio = repositorioHistorialPrecio.buscarUltimoPrecioHasta(activoId, sesion.getFechaActual())
                .orElseThrow(() -> new ExcepcionOperacionInvalida(
                        "No hay precio disponible para este activo en la fecha actual de la simulación"));
        return precio.getValor();
    }

    private CarteraUsuario obtenerCartera(Long sesionId) {
        return repositorioCartera.buscarPorSesion(sesionId)
                .orElseThrow(() -> new IllegalStateException("La sesión " + sesionId + " no tiene cartera asociada"));
    }

    private Activo obtenerActivo(Long activoId) {
        return repositorioActivo.buscarPorId(activoId)
                .orElseThrow(() -> new ExcepcionOperacionInvalida("No existe el activo " + activoId));
    }

    private SesionInversion obtenerSesionActiva(Long sesionId) {
        SesionInversion sesion = repositorioSesion.buscarPorId(sesionId)
                .orElseThrow(() -> new ExcepcionOperacionInvalida("No existe la sesión " + sesionId));
        if (sesion.getEstado() != EstadoSesion.ACTIVA) {
            throw new ExcepcionOperacionInvalida("La sesión " + sesionId + " no está activa");
        }
        return sesion;
    }

    private void validarCantidadPositiva(BigDecimal cantidad) {
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExcepcionOperacionInvalida("La cantidad a operar debe ser mayor a cero");
        }
    }
}
