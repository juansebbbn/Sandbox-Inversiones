package com.simuladorinversiones.core.servicios;

import com.simuladorinversiones.core.entidades.CarteraUsuario;
import com.simuladorinversiones.core.entidades.SesionInversion;
import com.simuladorinversiones.core.entidades.Usuario;
import com.simuladorinversiones.core.enums.EstadoSesion;
import com.simuladorinversiones.core.excepciones.ExcepcionOperacionInvalida;
import com.simuladorinversiones.core.repositorios.RepositorioCarteraUsuario;
import com.simuladorinversiones.core.repositorios.RepositorioSesionInversion;
import com.simuladorinversiones.core.repositorios.RepositorioUsuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Alta, pausa, reanudación y finalización de sesiones de inversión. */
public class ServicioSesionInversion {

    /** La simulación permite arrancar como muy temprano en 1900, según la spec del producto. */
    public static final LocalDate FECHA_MINIMA = LocalDate.of(1900, 1, 1);

    /** Límite superior de la simulación: "principios de 2026". */
    public static final LocalDate FECHA_LIMITE = LocalDate.of(2026, 1, 1);

    private final RepositorioSesionInversion repositorioSesion;
    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioCarteraUsuario repositorioCartera;

    public ServicioSesionInversion(RepositorioSesionInversion repositorioSesion,
                                    RepositorioUsuario repositorioUsuario,
                                    RepositorioCarteraUsuario repositorioCartera) {
        this.repositorioSesion = repositorioSesion;
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioCartera = repositorioCartera;
    }

    public SesionInversion crearSesion(Long usuarioId, LocalDate fechaInicio, BigDecimal capitalInicial) {
        if (fechaInicio.isBefore(FECHA_MINIMA) || !fechaInicio.isBefore(FECHA_LIMITE)) {
            throw new ExcepcionOperacionInvalida(
                    "La fecha de inicio debe estar entre " + FECHA_MINIMA + " y " + FECHA_LIMITE);
        }

        Usuario usuario = repositorioUsuario.buscarPorId(usuarioId)
                .orElseThrow(() -> new ExcepcionOperacionInvalida("No existe el usuario " + usuarioId));

        LocalDateTime ahora = LocalDateTime.now();
        SesionInversion sesion = new SesionInversion();
        sesion.setUsuario(usuario);
        sesion.setFechaInicio(fechaInicio);
        sesion.setFechaActual(fechaInicio);
        sesion.setFechaLimite(FECHA_LIMITE);
        sesion.setCapitalInicial(capitalInicial);
        sesion.setSaldoEfectivoActual(capitalInicial);
        sesion.setEstado(EstadoSesion.ACTIVA);
        sesion.setFechaCreacion(ahora);
        sesion.setFechaUltimaModificacion(ahora);
        repositorioSesion.guardar(sesion);

        CarteraUsuario cartera = new CarteraUsuario();
        cartera.setSesion(sesion);
        repositorioCartera.guardar(cartera);

        return sesion;
    }

    public SesionInversion pausar(Long sesionId) {
        SesionInversion sesion = obtenerSesionActiva(sesionId);
        sesion.setEstado(EstadoSesion.PAUSADA);
        sesion.setFechaUltimaModificacion(LocalDateTime.now());
        return repositorioSesion.actualizar(sesion);
    }

    public SesionInversion reanudar(Long sesionId) {
        SesionInversion sesion = buscarPorId(sesionId)
                .orElseThrow(() -> new ExcepcionOperacionInvalida("No existe la sesión " + sesionId));
        if (sesion.getEstado() != EstadoSesion.PAUSADA) {
            throw new ExcepcionOperacionInvalida("Solo se puede reanudar una sesión pausada");
        }
        sesion.setEstado(EstadoSesion.ACTIVA);
        sesion.setFechaUltimaModificacion(LocalDateTime.now());
        return repositorioSesion.actualizar(sesion);
    }

    public SesionInversion finalizar(Long sesionId) {
        SesionInversion sesion = buscarPorId(sesionId)
                .orElseThrow(() -> new ExcepcionOperacionInvalida("No existe la sesión " + sesionId));
        sesion.setEstado(EstadoSesion.FINALIZADA);
        sesion.setFechaUltimaModificacion(LocalDateTime.now());
        return repositorioSesion.actualizar(sesion);
    }

    public List<SesionInversion> listarPorUsuario(Long usuarioId) {
        return repositorioSesion.buscarPorUsuario(usuarioId);
    }

    public Optional<SesionInversion> buscarPorId(Long sesionId) {
        return repositorioSesion.buscarPorId(sesionId);
    }

    private SesionInversion obtenerSesionActiva(Long sesionId) {
        SesionInversion sesion = buscarPorId(sesionId)
                .orElseThrow(() -> new ExcepcionOperacionInvalida("No existe la sesión " + sesionId));
        if (sesion.getEstado() != EstadoSesion.ACTIVA) {
            throw new ExcepcionOperacionInvalida("La sesión " + sesionId + " no está activa");
        }
        return sesion;
    }
}
