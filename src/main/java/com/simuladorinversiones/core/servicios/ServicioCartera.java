package com.simuladorinversiones.core.servicios;

import com.simuladorinversiones.core.entidades.Activo;
import com.simuladorinversiones.core.entidades.CarteraUsuario;
import com.simuladorinversiones.core.entidades.HistorialPrecio;
import com.simuladorinversiones.core.entidades.PosicionCartera;
import com.simuladorinversiones.core.entidades.SesionInversion;
import com.simuladorinversiones.core.repositorios.RepositorioCarteraUsuario;
import com.simuladorinversiones.core.repositorios.RepositorioHistorialPrecio;
import com.simuladorinversiones.core.repositorios.RepositorioPosicionCartera;
import com.simuladorinversiones.core.repositorios.RepositorioSesionInversion;

import java.math.BigDecimal;
import java.util.List;

/** Lectura de la cartera de una sesión, enriquecida con precios vigentes para mostrar en la UI. */
public class ServicioCartera {

    private final RepositorioSesionInversion repositorioSesion;
    private final RepositorioCarteraUsuario repositorioCartera;
    private final RepositorioPosicionCartera repositorioPosicion;
    private final RepositorioHistorialPrecio repositorioHistorialPrecio;

    public ServicioCartera(RepositorioSesionInversion repositorioSesion,
                            RepositorioCarteraUsuario repositorioCartera,
                            RepositorioPosicionCartera repositorioPosicion,
                            RepositorioHistorialPrecio repositorioHistorialPrecio) {
        this.repositorioSesion = repositorioSesion;
        this.repositorioCartera = repositorioCartera;
        this.repositorioPosicion = repositorioPosicion;
        this.repositorioHistorialPrecio = repositorioHistorialPrecio;
    }

    public List<PosicionPortafolio> listarPosiciones(Long sesionId) {
        SesionInversion sesion = obtenerSesion(sesionId);
        CarteraUsuario cartera = obtenerCartera(sesionId);

        return repositorioPosicion.buscarPorCartera(cartera.getId()).stream()
                .map(posicion -> enriquecerPosicion(posicion, sesion))
                .toList();
    }

    public BigDecimal calcularPatrimonioNeto(Long sesionId) {
        SesionInversion sesion = obtenerSesion(sesionId);
        BigDecimal valorPosiciones = listarPosiciones(sesionId).stream()
                .map(PosicionPortafolio::valorActual)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sesion.getSaldoEfectivoActual().add(valorPosiciones);
    }

    private PosicionPortafolio enriquecerPosicion(PosicionCartera posicion, SesionInversion sesion) {
        Activo activo = posicion.getActivo();
        BigDecimal precioActual = repositorioHistorialPrecio
                .buscarUltimoPrecioHasta(activo.getId(), sesion.getFechaActual())
                .map(HistorialPrecio::getValor)
                .orElse(null);

        BigDecimal valorActual = precioActual == null ? null : precioActual.multiply(posicion.getCantidad());
        BigDecimal costoBase = posicion.getPrecioPromedioCompra().multiply(posicion.getCantidad());
        BigDecimal gananciaPerdida = valorActual == null ? null : valorActual.subtract(costoBase);

        return new PosicionPortafolio(activo.getId(), activo.getNombre(), activo.getTicker(),
                posicion.getCantidad(), posicion.getPrecioPromedioCompra(), precioActual, valorActual, gananciaPerdida);
    }

    private SesionInversion obtenerSesion(Long sesionId) {
        return repositorioSesion.buscarPorId(sesionId)
                .orElseThrow(() -> new ExcepcionOperacionInvalida("No existe la sesión " + sesionId));
    }

    private CarteraUsuario obtenerCartera(Long sesionId) {
        return repositorioCartera.buscarPorSesion(sesionId)
                .orElseThrow(() -> new IllegalStateException("La sesión " + sesionId + " no tiene cartera asociada"));
    }
}
